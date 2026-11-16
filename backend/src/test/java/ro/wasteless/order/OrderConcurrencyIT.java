package ro.wasteless.order;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import ro.wasteless.support.TestFoundation;

@TestPropertySource(properties = "app.orders.cancel-grace-seconds=3600")
class OrderConcurrencyIT extends OrderTestSupport {
  @Autowired PlatformTransactionManager transactions;

  @Test
  void demandBeyondStockCreatesExactlyAvailableUnits() throws Exception {
    var c = catalog();
    var u = client();
    jdbc.update("update offers set quantity_total=5,quantity_available=5 where id=?", c.offer());
    List<Callable<MvcResult>> actions = new ArrayList<>();
    for (int i = 0; i < 12; i++)
      actions.add(() -> place(u, c.offer(), 1, UUID.randomUUID().toString()));
    var results = parallel(actions);
    assertThat(results.stream().filter(r -> r.getResponse().getStatus() == 201).count())
        .as(diagnostics(c.offer()))
        .isEqualTo(5);
    assertThat(results.stream().filter(r -> r.getResponse().getStatus() == 409).count())
        .isEqualTo(7);
    assertThat(stock(c.offer())).isZero();
    assertThat(orderCount(c.offer())).isEqualTo(5);
    assertThat(
            jdbc.queryForObject(
                "select count(*) from payments p join orders o on o.id=p.order_id where o.offer_id=? and p.status='CAPTURED'",
                Integer.class,
                c.offer()))
        .isEqualTo(5);
    assertThat(
            jdbc.queryForObject(
                "select count(distinct pickup_code) from orders where offer_id=?",
                Integer.class,
                c.offer()))
        .isEqualTo(5);
    assertThat(
            jdbc.queryForObject(
                "select count(*) from order_status_history h join orders o on o.id=h.order_id where o.offer_id=? and h.to_status='ACTIVE'",
                Integer.class,
                c.offer()))
        .isEqualTo(5);
  }

  @Test
  void identicalConcurrentKeysExecuteOnlyOnce() throws Exception {
    var c = catalog();
    var u = client();
    String key = UUID.randomUUID().toString();
    List<Callable<MvcResult>> actions = new ArrayList<>();
    for (int i = 0; i < 6; i++) actions.add(() -> place(u, c.offer(), 1, key));
    var results = parallel(actions);
    Set<String> references = new HashSet<>();
    for (var r : results) {
      assertThat(r.getResponse().getStatus())
          .as(r.getResponse().getContentAsString())
          .isEqualTo(201);
      references.add(body(r).get("id").asText());
    }
    assertThat(references).hasSize(1);
    assertThat(stock(c.offer())).isEqualTo(9);
    assertThat(orderCount(c.offer())).isEqualTo(1);
    assertThat(
            jdbc.queryForObject("select count(*) from payments where user_id=?", Integer.class, u))
        .isEqualTo(1);
  }

  @Test
  void parallelCancellationRefundsAndRestocksExactlyOnce() throws Exception {
    var c = catalog();
    var u = client();
    String ref = body(place(u, c.offer(), 2, UUID.randomUUID().toString())).get("id").asText();
    List<Callable<MvcResult>> actions = new ArrayList<>();
    for (int i = 0; i < 6; i++) actions.add(() -> cancel(u, ref, UUID.randomUUID().toString()));
    for (var r : parallel(actions))
      assertThat(r.getResponse().getStatus())
          .as(r.getResponse().getContentAsString())
          .isEqualTo(200);
    assertThat(stock(c.offer())).isEqualTo(10);
    assertThat(state(ref)).isEqualTo("CANCELED");
    assertThat(
            jdbc.queryForObject(
                "select count(*) from refunds where order_id=(select id from orders where reference=?) and status='SUCCEEDED'",
                Integer.class,
                ref))
        .isEqualTo(1);
    assertThat(
            jdbc.queryForObject(
                "select sum(amount) from refunds where order_id=(select id from orders where reference=?) and status='SUCCEEDED'",
                java.math.BigDecimal.class,
                ref))
        .isEqualByComparingTo("8");
    assertThat(history(ref, "CANCELED")).isEqualTo(1);
    assertThat(history(ref, "REFUND_SUCCEEDED")).isEqualTo(1);
  }

  @Test
  void pickupAndCancellationRaceHasOnlyOneTerminalWinner() throws Exception {
    var c = catalog();
    var u = client();
    var order = body(place(u, c.offer(), 1, UUID.randomUUID().toString()));
    String ref = order.get("id").asText(), code = order.get("code").asText();
    // Grace policy makes BOTH operations eligible; the database lock must decide the winner.
    jdbc.update(
        "update orders set pickup_start_at=?,pickup_end_at=? where reference=?",
        java.sql.Timestamp.from(TestFoundation.NOW.minusSeconds(60)),
        java.sql.Timestamp.from(TestFoundation.NOW.plusSeconds(600)),
        ref);
    var results =
        parallel(
            List.of(() -> cancel(u, ref, "race-cancel"), () -> validate(c.owner(), ref, code)));
    assertThat(results.stream().map(r -> r.getResponse().getStatus()).toList())
        .containsExactlyInAnyOrder(200, 409);
    assertThat(state(ref)).isIn("CANCELED", "COMPLETED");
    assertThat(history(ref, "CANCELED") + history(ref, "COMPLETED")).isEqualTo(1);
    if (state(ref).equals("CANCELED")) {
      assertThat(stock(c.offer())).isEqualTo(10);
      assertThat(history(ref, "REFUND_SUCCEEDED")).isEqualTo(1);
    } else {
      assertThat(stock(c.offer())).isEqualTo(9);
      assertThat(history(ref, "REFUND_SUCCEEDED")).isZero();
      assertThat(
              jdbc.queryForObject(
                  "select count(*) from refunds where order_id=(select id from orders where reference=?)",
                  Integer.class,
                  ref))
          .isZero();
    }
  }

  @Test
  void pauseAndDeletionWinningTheOfferLockPreventPlacement() throws Exception {
    for (String mutation : List.of("pause", "soft-delete", "hard-delete")) {
      var c = catalog();
      var u = client();
      CountDownLatch locked = new CountDownLatch(1),
          release = new CountDownLatch(1),
          requestStarted = new CountDownLatch(1);
      ExecutorService pool = Executors.newFixedThreadPool(2);
      try {
        var writer =
            pool.submit(
                () ->
                    new TransactionTemplate(transactions)
                        .executeWithoutResult(
                            s -> {
                              if (mutation.equals("pause"))
                                jdbc.update(
                                    "update offers set status='PAUSED' where id=?", c.offer());
                              else if (mutation.equals("soft-delete"))
                                jdbc.update(
                                    "update offers set deleted_at=now() where id=?", c.offer());
                              else jdbc.update("delete from offers where id=?", c.offer());
                              locked.countDown();
                              try {
                                if (!release.await(10, TimeUnit.SECONDS))
                                  throw new IllegalStateException("Release timeout");
                              } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                                throw new IllegalStateException(e);
                              }
                            }));
        assertThat(locked.await(10, TimeUnit.SECONDS)).as(mutation).isTrue();
        var reader =
            pool.submit(
                () -> {
                  requestStarted.countDown();
                  return place(u, c.offer(), 1, UUID.randomUUID().toString());
                });
        assertThat(requestStarted.await(10, TimeUnit.SECONDS)).isTrue();
        release.countDown();
        writer.get(20, TimeUnit.SECONDS);
        var result = reader.get(20, TimeUnit.SECONDS);
        assertThat(result.getResponse().getStatus())
            .as(mutation + ": " + result.getResponse().getContentAsString())
            .isEqualTo(409);
        assertThat(
                jdbc.queryForObject(
                    "select count(*) from orders where customer_user_id=?", Integer.class, u))
            .isZero();
        assertThat(
                jdbc.queryForObject(
                    "select count(*) from payments where user_id=?", Integer.class, u))
            .isZero();
      } finally {
        release.countDown();
        pool.shutdownNow();
      }
    }
  }

  private List<MvcResult> parallel(List<Callable<MvcResult>> actions) throws Exception {
    ExecutorService pool = Executors.newFixedThreadPool(actions.size());
    CountDownLatch ready = new CountDownLatch(actions.size()), start = new CountDownLatch(1);
    try {
      List<Future<MvcResult>> futures = new ArrayList<>();
      for (var action : actions)
        futures.add(
            pool.submit(
                () -> {
                  ready.countDown();
                  if (!start.await(10, TimeUnit.SECONDS))
                    throw new IllegalStateException("Start timeout");
                  return action.call();
                }));
      assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
      start.countDown();
      List<MvcResult> results = new ArrayList<>();
      for (var future : futures) results.add(future.get(30, TimeUnit.SECONDS));
      return results;
    } finally {
      start.countDown();
      pool.shutdownNow();
    }
  }

  private MvcResult cancel(UUID user, String ref, String key) throws Exception {
    return mvc.perform(
            post("/api/v1/orders/" + ref + "/cancel")
                .with(user(user.toString()).roles("CLIENT"))
                .header("Idempotency-Key", key)
                .contentType("application/json")
                .content("{}"))
        .andReturn();
  }

  private MvcResult validate(UUID owner, String ref, String code) throws Exception {
    return mvc.perform(
            post("/api/v1/businesses/me/orders/" + ref + "/validate")
                .with(user(owner.toString()).roles("BUSINESS"))
                .contentType("application/json")
                .content("{\"code\":\"" + code + "\"}"))
        .andReturn();
  }

  private int orderCount(UUID offer) {
    return jdbc.queryForObject(
        "select count(*) from orders where offer_id=?", Integer.class, offer);
  }

  private String state(String ref) {
    return jdbc.queryForObject("select status from orders where reference=?", String.class, ref);
  }

  private int history(String ref, String status) {
    return jdbc.queryForObject(
        "select count(*) from order_status_history h join orders o on o.id=h.order_id where o.reference=? and h.to_status=?",
        Integer.class,
        ref,
        status);
  }

  private String diagnostics(UUID offer) {
    return "offer=" + offer + ", remaining=" + stock(offer) + ", orders=" + orderCount(offer);
  }
}

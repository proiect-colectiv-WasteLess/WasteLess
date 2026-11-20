package ro.wasteless;

import static org.assertj.core.api.Assertions.*;

import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.*;
import org.springframework.transaction.support.TransactionTemplate;
import ro.wasteless.common.api.DomainException;
import ro.wasteless.common.port.OfferStockPort;
import ro.wasteless.support.*;

class OfferStockConcurrencyIT extends PostgisIntegrationTest {
  @Autowired JdbcTemplate jdbc;
  @Autowired OfferStockPort stock;
  @Autowired PlatformTransactionManager manager;

  @Test
  void hundredReservationsForTenUnitsCommitExactlyTenAndRestoreCaps() throws Exception {
    var fixture = new Fixtures(jdbc).catalog();
    var id = fixture.offer();
    jdbc.update(
        "update offers set pickup_start_at=?,pickup_end_at=? where id=?",
        Timestamp.from(TestFoundation.NOW.plusSeconds(3600)),
        Timestamp.from(TestFoundation.NOW.plusSeconds(7200)),
        id);
    var transaction = new TransactionTemplate(manager);
    var start = new CountDownLatch(1);
    var futures = new ArrayList<Future<Boolean>>();
    try (var executor = Executors.newFixedThreadPool(16)) {
      for (int i = 0; i < 100; i++)
        futures.add(
            executor.submit(
                () -> {
                  start.await();
                  try {
                    transaction.executeWithoutResult(s -> stock.reserve(id, 1));
                    return true;
                  } catch (DomainException e) {
                    return false;
                  }
                }));
      start.countDown();
      int successes = 0;
      for (var future : futures) if (future.get(60, TimeUnit.SECONDS)) successes++;
      assertThat(successes).isEqualTo(10);
    }
    assertThat(
            jdbc.queryForObject(
                "select quantity_available from offers where id=?", Integer.class, id))
        .isZero();
    assertThat(jdbc.queryForObject("select status from offers where id=?", String.class, id))
        .isEqualTo("SOLD_OUT");
    transaction.executeWithoutResult(s -> stock.release(id, 4));
    assertThat(
            jdbc.queryForObject(
                "select quantity_available from offers where id=?", Integer.class, id))
        .isEqualTo(4);
    transaction.executeWithoutResult(s -> stock.release(id, 100));
    assertThat(
            jdbc.queryForObject(
                "select quantity_available from offers where id=?", Integer.class, id))
        .isEqualTo(10);
    assertThatThrownBy(() -> stock.reserve(id, 1))
        .isInstanceOf(IllegalTransactionStateException.class);
    assertThatThrownBy(
            () ->
                transaction.executeWithoutResult(
                    s -> {
                      stock.reserve(id, 2);
                      throw new IllegalStateException("rollback");
                    }))
        .isInstanceOf(IllegalStateException.class);
    assertThat(
            jdbc.queryForObject(
                "select quantity_available from offers where id=?", Integer.class, id))
        .isEqualTo(10);
  }
}

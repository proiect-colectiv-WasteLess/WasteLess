package ro.wasteless.order;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

import java.util.*;
import org.junit.jupiter.api.Test;

class OrderContractIT extends OrderTestSupport {
  @Test
  void flutterKeysAndCanceledTabContract() throws Exception {
    var c = catalog();
    var u = client();
    var created = place(u, c.offer(), 2, UUID.randomUUID().toString());
    assertThat(created.getResponse().getStatus()).isEqualTo(201);
    var o = body(created);
    List<String> keys = new ArrayList<>();
    keys.addAll(o.propertyNames());
    assertThat(keys)
        .contains(
            "id",
            "quantity",
            "itemName",
            "shop",
            "imageAsset",
            "pickupWindow",
            "code",
            "price",
            "timeAgo",
            "isCompleted",
            "status",
            "createdAt",
            "canceledAt");
    assertThat(o.get("isCompleted").asBoolean()).isFalse();
    var canceled =
        mvc.perform(
                post("/api/v1/orders/" + o.get("id").asText() + "/cancel")
                    .with(user(u.toString()).roles("CLIENT"))
                    .header("Idempotency-Key", "contract-cancel")
                    .contentType("application/json")
                    .content("{}"))
            .andReturn();
    assertThat(canceled.getResponse().getStatus()).isEqualTo(200);
    assertThat(body(canceled).get("status").asText()).isEqualTo("CANCELED");
    assertThat(body(canceled).get("canceledAt").isNull()).isFalse();
    assertThat(body(canceled).get("isCompleted").asBoolean()).isFalse();
    var replay =
        mvc.perform(
                post("/api/v1/orders/" + o.get("id").asText() + "/cancel")
                    .with(user(u.toString()).roles("CLIENT"))
                    .header("Idempotency-Key", "contract-cancel")
                    .contentType("application/json")
                    .content("{}"))
            .andReturn();
    assertThat(body(replay)).isEqualTo(body(canceled));
    assertThat(stock(c.offer())).isEqualTo(10);
  }

  @Test
  void invalidInputCannotSupplyPriceOwnerOrFractionalQuantity() throws Exception {
    var c = catalog();
    var u = client();
    String base = "{\"offerId\":\"" + c.offer() + "\",\"quantity\":1,\"paymentMethod\":\"CARD\"}";
    for (String input :
        List.of(
            base.replace("\"quantity\":1", "\"quantity\":1.5"),
            base.replace("\"quantity\":1", "\"quantity\":0"),
            base.replace("\"CARD\"", "0"),
            base.substring(0, base.length() - 1) + ",\"price\":0}",
            base.substring(0, base.length() - 1) + ",\"customerUserId\":\"" + u + "\"}")) {
      var r =
          mvc.perform(
                  post("/api/v1/orders")
                      .with(user(u.toString()).roles("CLIENT"))
                      .header("Idempotency-Key", UUID.randomUUID().toString())
                      .contentType("application/json")
                      .content(input))
              .andReturn();
      assertThat(r.getResponse().getStatus()).as(input).isEqualTo(400);
      assertThat(body(r).get("code").asText()).isEqualTo("VALIDATION_FAILED");
    }
    var missing =
        mvc.perform(
                post("/api/v1/orders")
                    .with(user(u.toString()).roles("CLIENT"))
                    .contentType("application/json")
                    .content(base))
            .andReturn();
    assertThat(missing.getResponse().getStatus()).isEqualTo(400);
    assertThat(body(missing).get("code").asText()).isEqualTo("VALIDATION_FAILED");
    assertThat(mvc.perform(get("/api/v1/orders")).andReturn().getResponse().getStatus())
        .isEqualTo(401);
    assertThat(stock(c.offer())).isEqualTo(10);
  }

  @Test
  void unorderableStockAndPaymentFailuresHaveNoSideEffects() throws Exception {
    var u = client();
    for (String condition :
        List.of("stock", "paused", "deleted", "expired", "timeout", "decline")) {
      var c = catalog();
      switch (condition) {
        case "stock" ->
            jdbc.update(
                "update offers set quantity_available=0,status='SOLD_OUT' where id=?", c.offer());
        case "paused" -> jdbc.update("update offers set status='PAUSED' where id=?", c.offer());
        case "deleted" -> jdbc.update("update offers set deleted_at=now() where id=?", c.offer());
        case "expired" ->
            jdbc.update(
                "update offers set expires_at=? where id=?",
                java.sql.Timestamp.from(ro.wasteless.support.TestFoundation.NOW),
                c.offer());
        case "timeout" ->
            jdbc.update("update offers set price=14.14,original_price=20 where id=?", c.offer());
        case "decline" ->
            jdbc.update("update offers set price=13.13,original_price=20 where id=?", c.offer());
      }
      int before = stock(c.offer());
      var r = place(u, c.offer(), 1, UUID.randomUUID().toString());
      assertThat(r.getResponse().getStatus())
          .as(condition)
          .isEqualTo(List.of("timeout", "decline").contains(condition) ? 402 : 409);
      assertThat(stock(c.offer())).isEqualTo(before);
    }
    var c = catalog();
    assertThat(place(u, c.offer(), 11, UUID.randomUUID().toString()).getResponse().getStatus())
        .isEqualTo(409);
    assertThat(stock(c.offer())).isEqualTo(10);
    assertThat(
            jdbc.queryForObject("select count(*) from payments where user_id=?", Integer.class, u))
        .isZero();
    assertThat(
            jdbc.queryForObject(
                "select count(*) from orders where customer_user_id=?", Integer.class, u))
        .isZero();
  }
}

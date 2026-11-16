package ro.wasteless.order;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

import java.util.*;
import org.junit.jupiter.api.Test;

class OrderPlacementIT extends OrderTestSupport {
  @Test
  void placeSnapshotsAndRollsBackOnDecline() throws Exception {
    var c = catalog();
    var u = client();
    var result = place(u, c.offer(), 2, UUID.randomUUID().toString());
    assertThat(result.getResponse().getStatus()).isEqualTo(201);
    var json = body(result);
    assertThat(json.get("id").asText()).startsWith("WL-");
    assertThat(json.get("code").asText()).matches("[0-9]{4}");
    assertThat(json.get("price").decimalValue()).isEqualByComparingTo("8");
    assertThat(stock(c.offer())).isEqualTo(8);
    assertThat(
            jdbc.queryForObject(
                "select count(*) from payments where order_id=(select id from orders where reference=?) and status='CAPTURED'",
                Integer.class,
                json.get("id").asText()))
        .isEqualTo(1);
    jdbc.update("update offers set price=13.13,original_price=20 where id=?", c.offer());
    assertThat(place(u, c.offer(), 1, UUID.randomUUID().toString()).getResponse().getStatus())
        .isEqualTo(402);
    assertThat(stock(c.offer())).isEqualTo(8);
    assertThat(
            jdbc.queryForObject(
                "select count(*) from orders where customer_user_id=?", Integer.class, u))
        .isEqualTo(1);
    for (String state : List.of("PAUSED", "EXPIRED")) {
      jdbc.update("update offers set status=? where id=?", state, c.offer());
      assertThat(place(u, c.offer(), 1, UUID.randomUUID().toString()).getResponse().getStatus())
          .isEqualTo(409);
    }
    assertThat(
            mvc.perform(
                    post("/api/v1/orders")
                        .with(user(c.owner().toString()).roles("BUSINESS"))
                        .header("Idempotency-Key", "wrong-role")
                        .contentType("application/json")
                        .content(
                            "{\"offerId\":\""
                                + c.offer()
                                + "\",\"quantity\":1,\"paymentMethod\":\"CARD\"}"))
                .andReturn()
                .getResponse()
                .getStatus())
        .isEqualTo(403);
  }
}

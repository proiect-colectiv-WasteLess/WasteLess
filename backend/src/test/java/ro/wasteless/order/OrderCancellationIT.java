package ro.wasteless.order;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

import java.util.*;
import org.junit.jupiter.api.Test;

class OrderCancellationIT extends OrderTestSupport {
  @Test
  void cancellationRestoresExactlyOnceAndGuardsOwnership() throws Exception {
    var c = catalog();
    var u = client();
    String ref = body(place(u, c.offer(), 2, UUID.randomUUID().toString())).get("id").asText();
    var result = cancel(u, ref, "first");
    assertThat(result.getResponse().getStatus()).isEqualTo(200);
    assertThat(body(result).get("status").asText()).isEqualTo("CANCELED");
    assertThat(body(result).get("refund").get("status").asText()).isEqualTo("SUCCEEDED");
    assertThat(stock(c.offer())).isEqualTo(10);
    assertThat(cancel(u, ref, "second").getResponse().getStatus()).isEqualTo(200);
    assertThat(cancel(client(), ref, "other").getResponse().getStatus()).isEqualTo(404);
    assertThat(stock(c.offer())).isEqualTo(10);
    assertThat(
            jdbc.queryForObject(
                "select count(*) from refunds where order_id=(select id from orders where reference=?)",
                Integer.class,
                ref))
        .isEqualTo(1);
    String late = body(place(u, c.offer(), 1, UUID.randomUUID().toString())).get("id").asText();
    jdbc.update(
        "update orders set pickup_start_at=? where reference=?",
        java.sql.Timestamp.from(ro.wasteless.support.TestFoundation.NOW.minusSeconds(1)),
        late);
    assertThat(cancel(u, late, "late").getResponse().getStatus()).isEqualTo(409);
  }

  @Test
  void failedRefundRemainsVisibleWithoutRestockOrCancellation() throws Exception {
    var c = catalog();
    var u = client();
    jdbc.update("update offers set price=15.15,original_price=20 where id=?", c.offer());
    String ref = body(place(u, c.offer(), 1, UUID.randomUUID().toString())).get("id").asText();
    assertThat(cancel(u, ref, "failure").getResponse().getStatus()).isEqualTo(502);
    assertThat(stock(c.offer())).isEqualTo(9);
    var detail =
        body(
            mvc.perform(get("/api/v1/orders/" + ref).with(user(u.toString()).roles("CLIENT")))
                .andReturn());
    assertThat(detail.get("status").asText()).isEqualTo("ACTIVE");
    assertThat(detail.get("refund").get("status").asText()).isEqualTo("FAILED");
  }

  private org.springframework.test.web.servlet.MvcResult cancel(UUID u, String ref, String key)
      throws Exception {
    return mvc.perform(
            post("/api/v1/orders/" + ref + "/cancel")
                .with(user(u.toString()).roles("CLIENT"))
                .header("Idempotency-Key", key)
                .contentType("application/json")
                .content("{\"reason\":\"Changed plans\"}"))
        .andReturn();
  }
}

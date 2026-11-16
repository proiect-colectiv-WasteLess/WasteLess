package ro.wasteless.order;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ro.wasteless.order.service.RefundReconciliation;
import ro.wasteless.support.TestFoundation;

class RefundRetryIT extends OrderTestSupport {
  @Autowired RefundReconciliation reconciliation;

  @Test
  void backoffThenSuccessAndNoDuplicateRefund() throws Exception {
    var c = catalog();
    var u = client();
    jdbc.update("update offers set price=16.16,original_price=20 where id=?", c.offer());
    String ref = body(place(u, c.offer(), 1, UUID.randomUUID().toString())).get("id").asText();
    var cancel =
        mvc.perform(
                post("/api/v1/orders/" + ref + "/cancel")
                    .with(user(u.toString()).roles("CLIENT"))
                    .header("Idempotency-Key", "retry-start")
                    .contentType("application/json")
                    .content("{}"))
            .andReturn();
    assertThat(cancel.getResponse().getStatus()).isEqualTo(502);
    reconciliation.reconcile();
    assertThat(state(ref)).isEqualTo("ACTIVE");
    for (int i = 0; i < 2; i++) {
      jdbc.update(
          "update refunds set processed_at=? where order_id=(select id from orders where reference=?)",
          java.sql.Timestamp.from(TestFoundation.NOW.minusSeconds(3600)),
          ref);
      reconciliation.reconcile();
    }
    assertThat(state(ref)).isEqualTo("CANCELED");
    assertThat(stock(c.offer())).isEqualTo(10);
    reconciliation.reconcile();
    assertThat(
            jdbc.queryForObject(
                "select count(*) from refunds where order_id=(select id from orders where reference=?) and status='SUCCEEDED'",
                Integer.class,
                ref))
        .isEqualTo(1);
  }

  private String state(String ref) {
    return jdbc.queryForObject("select status from orders where reference=?", String.class, ref);
  }
}

package ro.wasteless.order;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.event.*;
import ro.wasteless.common.event.OrderCompletedEvent;
import ro.wasteless.support.TestFoundation;

@RecordApplicationEvents
class OrderAuditIT extends OrderTestSupport {
  @Autowired ApplicationEvents events;

  @Test
  void historyAndEventsAreExactlyOnceAndRejectedTransitionsLeaveNoRows() throws Exception {
    var c = catalog();
    var u = client();
    var order = body(place(u, c.offer(), 1, UUID.randomUUID().toString()));
    String ref = order.get("id").asText(), code = order.get("code").asText();
    assertThat(count(ref, "ACTIVE")).isEqualTo(1);
    var invalid =
        mvc.perform(
                post("/api/v1/businesses/me/orders/" + ref + "/validate")
                    .with(user(c.owner().toString()).roles("BUSINESS"))
                    .contentType("application/json")
                    .content("{\"code\":\"" + (code.equals("0000") ? "1111" : "0000") + "\"}"))
            .andReturn();
    assertThat(invalid.getResponse().getStatus()).isEqualTo(409);
    assertThat(count(ref, "COMPLETED")).isZero();
    jdbc.update(
        "update orders set pickup_start_at=?,pickup_end_at=? where reference=?",
        java.sql.Timestamp.from(TestFoundation.NOW.minusSeconds(1)),
        java.sql.Timestamp.from(TestFoundation.NOW.plusSeconds(600)),
        ref);
    for (int i = 0; i < 2; i++)
      assertThat(
              mvc.perform(
                      post("/api/v1/businesses/me/orders/" + ref + "/validate")
                          .with(user(c.owner().toString()).roles("BUSINESS"))
                          .contentType("application/json")
                          .content("{\"code\":\"" + code + "\"}"))
                  .andReturn()
                  .getResponse()
                  .getStatus())
          .isEqualTo(200);
    assertThat(count(ref, "COMPLETED")).isEqualTo(1);
    assertThat(events.stream(OrderCompletedEvent.class).count()).isEqualTo(1);
    jdbc.update("delete from offers where id=?", c.offer());
    assertThat(count(ref, "ACTIVE")).isEqualTo(1);
    assertThat(count(ref, "COMPLETED")).isEqualTo(1);
  }

  private int count(String ref, String state) {
    return jdbc.queryForObject(
        "select count(*) from order_status_history where order_id=(select id from orders where reference=?) and to_status=?",
        Integer.class,
        ref,
        state);
  }
}

package ro.wasteless.order;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

import java.util.*;
import org.junit.jupiter.api.Test;

class CustomerOrdersIT extends OrderTestSupport {
  @Test
  void ownedHistoryAndSnapshotsSurviveHardDeletion() throws Exception {
    var c = catalog();
    var u = client();
    var other = client();
    String ref = body(place(u, c.offer(), 1, UUID.randomUUID().toString())).get("id").asText();
    var detail =
        mvc.perform(get("/api/v1/orders/" + ref).with(user(u.toString()).roles("CLIENT")))
            .andReturn();
    assertThat(detail.getResponse().getStatus()).isEqualTo(200);
    assertThat(
            mvc.perform(get("/api/v1/orders/" + ref).with(user(other.toString()).roles("CLIENT")))
                .andReturn()
                .getResponse()
                .getStatus())
        .isEqualTo(404);
    var history =
        mvc.perform(
                get("/api/v1/orders?status=ACTIVE&size=1").with(user(u.toString()).roles("CLIENT")))
            .andReturn();
    assertThat(body(history).get("totalElements").asInt()).isEqualTo(1);
    jdbc.update("delete from offers where id=?", c.offer());
    var after =
        body(
            mvc.perform(get("/api/v1/orders/" + ref).with(user(u.toString()).roles("CLIENT")))
                .andReturn());
    assertThat(after.get("itemName")).isEqualTo(body(detail).get("itemName"));
    assertThat(after.get("shop")).isEqualTo(body(detail).get("shop"));
    assertThat(after.get("price")).isEqualTo(body(detail).get("price"));
    for (String state : List.of("COMPLETED", "CANCELED")) {
      jdbc.update("update orders set status=? where reference=?", state, ref);
      var tab =
          body(
              mvc.perform(
                      get("/api/v1/orders?status=" + state)
                          .with(user(u.toString()).roles("CLIENT")))
                  .andReturn());
      assertThat(tab.get("totalElements").asInt()).isEqualTo(1);
      assertThat(tab.get("content").get(0).get("isCompleted").asBoolean())
          .isEqualTo(state.equals("COMPLETED"));
    }
    assertThat(
            mvc.perform(get("/api/v1/orders?size=101").with(user(u.toString()).roles("CLIENT")))
                .andReturn()
                .getResponse()
                .getStatus())
        .isEqualTo(400);
  }
}

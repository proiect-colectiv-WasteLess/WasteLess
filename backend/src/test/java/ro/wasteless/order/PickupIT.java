package ro.wasteless.order;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import ro.wasteless.support.TestFoundation;

class PickupIT extends OrderTestSupport {
  @Test
  void validateWindowCodeOwnershipAndRepeat() throws Exception {
    var c = catalog();
    var other = catalog();
    var u = client();
    var order = body(place(u, c.offer(), 1, UUID.randomUUID().toString()));
    String ref = order.get("id").asText(), code = order.get("code").asText();
    String bad = code.equals("0000") ? "1111" : "0000";
    assertThat(validate(c.owner(), ref, bad).getResponse().getStatus()).isEqualTo(409);
    assertThat(validate(other.owner(), ref, code).getResponse().getStatus()).isEqualTo(404);
    assertThat(validate(c.owner(), ref, code).getResponse().getStatus()).isEqualTo(409);
    jdbc.update(
        "update orders set pickup_start_at=?,pickup_end_at=? where reference=?",
        java.sql.Timestamp.from(TestFoundation.NOW.minusSeconds(60)),
        java.sql.Timestamp.from(TestFoundation.NOW.plusSeconds(600)),
        ref);
    var result = validate(c.owner(), ref, code);
    assertThat(result.getResponse().getStatus()).isEqualTo(200);
    assertThat(body(result).get("isCompleted").asBoolean()).isTrue();
    assertThat(validate(c.owner(), ref, code).getResponse().getStatus()).isEqualTo(200);
    var detail =
        body(
            mvc.perform(get("/api/v1/orders/" + ref).with(user(u.toString()).roles("CLIENT")))
                .andReturn());
    assertThat(detail.get("status").asText()).isEqualTo("COMPLETED");
  }

  @Test
  void bruteForceIsRateLimitedAndCanceledOrdersStayCanceled() throws Exception {
    var c = catalog();
    var u = client();
    var order = body(place(u, c.offer(), 1, UUID.randomUUID().toString()));
    String ref = order.get("id").asText(), code = order.get("code").asText();
    String bad = code.equals("0000") ? "1111" : "0000";
    for (int i = 0; i < 5; i++)
      assertThat(validate(c.owner(), ref, bad).getResponse().getStatus()).isEqualTo(409);
    assertThat(validate(c.owner(), ref, bad).getResponse().getStatus()).isEqualTo(429);
    jdbc.update("update orders set status='CANCELED' where reference=?", ref);
    // A different order does not share the first one's rate-limit bucket.
    var second = body(place(u, c.offer(), 1, UUID.randomUUID().toString()));
    jdbc.update("update orders set status='CANCELED' where reference=?", second.get("id").asText());
    assertThat(
            validate(c.owner(), second.get("id").asText(), second.get("code").asText())
                .getResponse()
                .getStatus())
        .isEqualTo(409);
  }

  private org.springframework.test.web.servlet.MvcResult validate(
      UUID owner, String ref, String code) throws Exception {
    return mvc.perform(
            post("/api/v1/businesses/me/orders/" + ref + "/validate")
                .with(user(owner.toString()).roles("BUSINESS"))
                .contentType("application/json")
                .content("{\"code\":\"" + code + "\"}"))
        .andReturn();
  }
}

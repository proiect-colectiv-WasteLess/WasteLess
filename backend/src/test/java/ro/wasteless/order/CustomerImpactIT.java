package ro.wasteless.order;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import ro.wasteless.support.TestFoundation;

class CustomerImpactIT extends OrderTestSupport {
  @Test
  void onlyCompletedQuantitiesContribute() throws Exception {
    var c = catalog();
    var u = client();
    var order = body(place(u, c.offer(), 2, UUID.randomUUID().toString()));
    assertThat(impact(u).get("mealsRescued").asLong()).isZero();
    String ref = order.get("id").asText();
    jdbc.update(
        "update orders set pickup_start_at=?,pickup_end_at=? where reference=?",
        java.sql.Timestamp.from(TestFoundation.NOW.minusSeconds(60)),
        java.sql.Timestamp.from(TestFoundation.NOW.plusSeconds(600)),
        ref);
    assertThat(
            mvc.perform(
                    post("/api/v1/businesses/me/orders/" + ref + "/validate")
                        .with(user(c.owner().toString()).roles("BUSINESS"))
                        .contentType("application/json")
                        .content("{\"code\":\"" + order.get("code").asText() + "\"}"))
                .andReturn()
                .getResponse()
                .getStatus())
        .isEqualTo(200);
    var impact = impact(u);
    assertThat(impact.get("mealsRescued").asLong()).isEqualTo(2);
    assertThat(impact.get("moneySaved").decimalValue()).isEqualByComparingTo("16.00");
    assertThat(impact.get("co2AvoidedKg").decimalValue()).isEqualByComparingTo("2.4");
    assertThat(impact.get("mealsToNextLevel").asLong()).isEqualTo(8);
    assertThat(impact(client()).get("mealsRescued").asLong()).isZero();
    assertThat(impact.has("orders")).isFalse();
    String canceled = body(place(u, c.offer(), 1, UUID.randomUUID().toString())).get("id").asText();
    assertThat(
            mvc.perform(
                    post("/api/v1/orders/" + canceled + "/cancel")
                        .with(user(u.toString()).roles("CLIENT"))
                        .header("Idempotency-Key", "impact-cancel")
                        .contentType("application/json")
                        .content("{}"))
                .andReturn()
                .getResponse()
                .getStatus())
        .isEqualTo(200);
    assertThat(impact(u).get("mealsRescued").asLong()).isEqualTo(2);
  }

  private tools.jackson.databind.JsonNode impact(UUID u) throws Exception {
    return body(
        mvc.perform(get("/api/v1/users/me/impact").with(user(u.toString()).roles("CLIENT")))
            .andReturn());
  }
}

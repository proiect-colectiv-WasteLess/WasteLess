package ro.wasteless.order;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import ro.wasteless.support.TestFoundation;

class BusinessDashboardIT extends OrderTestSupport {
  @Test
  void completedRevenueDateRangeAndOwnership() throws Exception {
    var c = catalog();
    var other = catalog();
    var u = client();
    var order = body(place(u, c.offer(), 2, UUID.randomUUID().toString()));
    assertThat(dashboard(c.owner(), "").get("activeOrders").asInt()).isEqualTo(1);
    assertThat(dashboard(c.owner(), "").get("revenue").decimalValue()).isEqualByComparingTo("0");
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
    var result = dashboard(c.owner(), "");
    assertThat(result.get("revenue").decimalValue()).isEqualByComparingTo("8");
    assertThat(result.get("commission").decimalValue()).isEqualByComparingTo("0.80");
    assertThat(result.get("netRevenue").decimalValue()).isEqualByComparingTo("7.20");
    assertThat(result.get("productsSold").asInt()).isEqualTo(2);
    assertThat(result.get("completedOrders").asInt()).isEqualTo(1);
    assertThat(result.get("from").asText()).isEqualTo("2026-11-16");
    assertThat(dashboard(c.owner(), "?from=2026-11-15&to=2026-11-15").get("revenue").decimalValue())
        .isEqualByComparingTo("0");
    assertThat(dashboard(other.owner(), "").get("revenue").decimalValue())
        .isEqualByComparingTo("0");
    assertThat(
            mvc.perform(
                    get("/api/v1/businesses/me/dashboard?from=2026-11-17&to=2026-11-16")
                        .with(user(c.owner().toString()).roles("BUSINESS")))
                .andReturn()
                .getResponse()
                .getStatus())
        .isEqualTo(400);
  }

  private tools.jackson.databind.JsonNode dashboard(UUID owner, String query) throws Exception {
    var result =
        mvc.perform(
                get("/api/v1/businesses/me/dashboard" + query)
                    .with(user(owner.toString()).roles("BUSINESS")))
            .andReturn();
    assertThat(result.getResponse().getStatus()).isEqualTo(200);
    return body(result);
  }
}

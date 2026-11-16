package ro.wasteless.order;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.*;
import org.springframework.test.web.servlet.*;
import ro.wasteless.support.*;
import tools.jackson.databind.*;

@SpringBootTest(
    properties = {"app.payment-mode=mock", "app.orders.refund-reconciliation-delay-ms=3600000"})
@ActiveProfiles("test")
@AutoConfigureMockMvc
@org.springframework.context.annotation.Import(TestFoundation.class)
public abstract class OrderTestSupport {
  @DynamicPropertySource
  static void database(DynamicPropertyRegistry r) {
    var db = IntegrationDatabase.settings();
    r.add("spring.datasource.url", db::getJdbcUrl);
    r.add("spring.datasource.username", db::getUsername);
    r.add("spring.datasource.password", db::getPassword);
  }

  @Autowired protected MockMvc mvc;
  @Autowired protected JdbcTemplate jdbc;
  @Autowired protected ObjectMapper mapper;

  protected UUID client() {
    return new Fixtures(jdbc).user();
  }

  protected Fixtures.Catalog catalog() {
    var c = new Fixtures(jdbc).catalog();
    jdbc.update(
        "update offers set pickup_start_at=?,pickup_end_at=? where id=?",
        java.sql.Timestamp.from(TestFoundation.NOW.plusSeconds(3600)),
        java.sql.Timestamp.from(TestFoundation.NOW.plusSeconds(10800)),
        c.offer());
    return c;
  }

  protected MvcResult place(UUID client, UUID offer, int quantity, String key) throws Exception {
    return mvc.perform(
            post("/api/v1/orders")
                .with(user(client.toString()).roles("CLIENT"))
                .header("Idempotency-Key", key)
                .contentType("application/json")
                .content(
                    "{\"offerId\":\""
                        + offer
                        + "\",\"quantity\":"
                        + quantity
                        + ",\"paymentMethod\":\"CARD\"}"))
        .andReturn();
  }

  protected JsonNode body(MvcResult r) throws Exception {
    return mapper.readTree(r.getResponse().getContentAsString());
  }

  protected int stock(UUID offer) {
    return jdbc.queryForObject(
        "select quantity_available from offers where id=?", Integer.class, offer);
  }
}

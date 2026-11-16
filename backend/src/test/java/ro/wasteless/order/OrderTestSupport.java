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
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import ro.wasteless.support.*;
import tools.jackson.databind.*;

@SpringBootTest(properties = {"app.payment-mode=mock"})
@ActiveProfiles("test")
@AutoConfigureMockMvc
@org.springframework.context.annotation.Import(TestFoundation.class)
public abstract class OrderTestSupport {
  static PostgreSQLContainer container;

  @DynamicPropertySource
  static void database(DynamicPropertyRegistry r) {
    String url = System.getenv("TEST_DB_URL");
    if (url == null) {
      container =
          new PostgreSQLContainer(
              DockerImageName.parse("postgis/postgis:16-3.4")
                  .asCompatibleSubstituteFor("postgres"));
      container.start();
      r.add("spring.datasource.url", container::getJdbcUrl);
      r.add("spring.datasource.username", container::getUsername);
      r.add("spring.datasource.password", container::getPassword);
    } else {
      r.add("spring.datasource.url", () -> url);
      r.add(
          "spring.datasource.username",
          () -> System.getenv().getOrDefault("TEST_DB_USERNAME", "postgres"));
      r.add(
          "spring.datasource.password", () -> System.getenv().getOrDefault("TEST_DB_PASSWORD", ""));
    }
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

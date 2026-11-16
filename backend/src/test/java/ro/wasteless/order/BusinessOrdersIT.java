package ro.wasteless.order;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class BusinessOrdersIT extends OrderTestSupport {
  @Test
  void isolatedListsExposeOnlyAbbreviatedName() throws Exception {
    var a = catalog();
    var b = catalog();
    var u = client();
    jdbc.update(
        "update users set first_name='Cezar',last_name='Morar',phone='private' where id=?", u);
    var placed = body(place(u, a.offer(), 1, UUID.randomUUID().toString()));
    var own =
        mvc.perform(
                get("/api/v1/businesses/me/orders?status=ACTIVE")
                    .with(user(a.owner().toString()).roles("BUSINESS")))
            .andReturn();
    assertThat(own.getResponse().getStatus()).isEqualTo(200);
    var item = body(own).get("content").get(0);
    assertThat(item.get("id")).isEqualTo(placed.get("id"));
    assertThat(item.get("customerName").asText()).isEqualTo("Cezar M.");
    assertThat(own.getResponse().getContentAsString())
        .doesNotContain("Morar", "@", "private", "phone", "email");
    var other =
        mvc.perform(
                get("/api/v1/businesses/me/orders")
                    .with(user(b.owner().toString()).roles("BUSINESS")))
            .andReturn();
    assertThat(body(other).get("totalElements").asInt()).isZero();
    assertThat(
            mvc.perform(
                    get("/api/v1/businesses/me/orders").with(user(u.toString()).roles("CLIENT")))
                .andReturn()
                .getResponse()
                .getStatus())
        .isEqualTo(403);
  }
}

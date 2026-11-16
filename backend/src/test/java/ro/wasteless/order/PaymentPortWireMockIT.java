package ro.wasteless.order;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.*;

import com.github.tomakehurst.wiremock.WireMockServer;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.*;
import ro.wasteless.common.port.PaymentGateway;
import tools.jackson.databind.json.JsonMapper;

@Import(PaymentPortWireMockIT.Provider.class)
class PaymentPortWireMockIT extends OrderTestSupport {
  static final WireMockServer server = new WireMockServer(options().dynamicPort());

  static {
    server.start();
  }

  @AfterAll
  static void closeServer() {
    server.stop();
  }

  @TestConfiguration
  static class Provider {
    @Bean
    @Primary
    PaymentGateway wirePayment() {
      return new PaymentGateway() {
        public Result authorize(
            UUID order, BigDecimal amount, String currency, String method, String key) {
          return new Result("auth-" + order, "AUTHORIZED");
        }

        public Result capture(
            UUID order, String auth, BigDecimal amount, String currency, String key) {
          return charge(order, amount, currency, key);
        }

        public Result charge(UUID order, BigDecimal amount, String currency, String key) {
          try {
            var req =
                HttpRequest.newBuilder(URI.create(server.baseUrl() + "/payments"))
                    .timeout(Duration.ofSeconds(3))
                    .header("Idempotency-Key", key)
                    .POST(HttpRequest.BodyPublishers.ofString(amount.toPlainString()))
                    .build();
            var result = HttpClient.newHttpClient().send(req, HttpResponse.BodyHandlers.ofString());
            if (result.statusCode() != 200) return new Result(null, "TIMEOUT");
            var json = JsonMapper.builder().build().readTree(result.body());
            return new Result(json.get("reference").asText(), json.get("status").asText());
          } catch (Exception e) {
            return new Result(null, "TIMEOUT");
          }
        }

        public Result refund(String reference, BigDecimal amount, String key) {
          return new Result("refund-" + key, "SUCCEEDED");
        }
      };
    }
  }

  @Test
  void orderCallsOnlyThePortAndReplaysWithoutSecondProviderCall() throws Exception {
    server.resetAll();
    server.stubFor(
        post(urlEqualTo("/payments"))
            .willReturn(okJson("{\"reference\":\"wire-charge\",\"status\":\"CAPTURED\"}")));
    var c = catalog();
    var u = client();
    String key = UUID.randomUUID().toString();
    assertThat(place(u, c.offer(), 1, key).getResponse().getStatus()).isEqualTo(201);
    assertThat(place(u, c.offer(), 1, key).getResponse().getStatus()).isEqualTo(201);
    server.verify(1, postRequestedFor(urlEqualTo("/payments")));
    assertThat(stock(c.offer())).isEqualTo(9);
    server.stubFor(post(urlEqualTo("/payments")).willReturn(aResponse().withStatus(503)));
    assertThat(place(u, c.offer(), 1, UUID.randomUUID().toString()).getResponse().getStatus())
        .isEqualTo(402);
    assertThat(stock(c.offer())).isEqualTo(9);
    assertThat(
            jdbc.queryForObject("select count(*) from payments where user_id=?", Integer.class, u))
        .isEqualTo(1);
  }
}

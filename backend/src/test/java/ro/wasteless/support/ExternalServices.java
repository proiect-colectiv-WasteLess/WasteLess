package ro.wasteless.support;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;

import com.github.tomakehurst.wiremock.WireMockServer;

/** Test-only HTTP contracts; adapters must explicitly configure this base URL. */
public final class ExternalServices implements AutoCloseable {
  public final WireMockServer server = new WireMockServer(options().dynamicPort());

  public ExternalServices() {
    server.start();
  }

  public String url() {
    return server.baseUrl();
  }

  public void paymentSuccess() {
    server.stubFor(
        post(urlEqualTo("/payments"))
            .willReturn(okJson("{\"reference\":\"test-charge\",\"status\":\"SUCCEEDED\"}")));
  }

  public void mailAccepted() {
    server.stubFor(post(urlEqualTo("/mail")).willReturn(aResponse().withStatus(202)));
  }

  public void paymentFailure() {
    server.stubFor(post(urlEqualTo("/payments")).willReturn(aResponse().withStatus(503)));
  }

  public void reset() {
    server.resetAll();
  }

  public void close() {
    server.stop();
  }
}

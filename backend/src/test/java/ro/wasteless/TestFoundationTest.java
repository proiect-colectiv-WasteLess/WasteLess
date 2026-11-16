package ro.wasteless;

import static org.assertj.core.api.Assertions.*;

import java.net.*;
import java.net.http.*;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import ro.wasteless.common.security.CurrentUser;
import ro.wasteless.support.*;

class TestFoundationTest {
  @Test
  void userHelperRestoresContext() throws Exception {
    var id = UUID.randomUUID();
    try (var ignored = TestFoundation.asUser(id, "BUSINESS")) {
      assertThat(new CurrentUser().id()).isEqualTo(id);
    }
    assertThat(
            org.springframework.security.core.context.SecurityContextHolder.getContext()
                .getAuthentication())
        .isNull();
  }

  @Test
  void externalServicesCanSwitchFromSuccessToFailure() throws Exception {
    try (var stub = new ExternalServices()) {
      stub.paymentSuccess();
      stub.mailAccepted();
      var client = HttpClient.newHttpClient();
      var request =
          HttpRequest.newBuilder(URI.create(stub.url() + "/payments"))
              .POST(HttpRequest.BodyPublishers.ofString("{}"))
              .build();
      assertThat(client.send(request, HttpResponse.BodyHandlers.ofString()).body())
          .contains("SUCCEEDED");
      stub.reset();
      stub.paymentFailure();
      assertThat(client.send(request, HttpResponse.BodyHandlers.ofString()).statusCode())
          .isEqualTo(503);
    }
  }
}

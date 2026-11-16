package ro.wasteless.order;

import static org.assertj.core.api.Assertions.*;

import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import ro.wasteless.payment.domain.*;
import ro.wasteless.payment.infrastructure.MockPaymentGateway;

class PaymentGatewayTest {
  @Test
  void successDeclineTimeoutRefundFailureAndReplay() {
    var gateway = new MockPaymentGateway();
    for (var method : PaymentMethod.values()) {
      var id = UUID.randomUUID();
      var success = gateway.charge(id, new BigDecimal("4.00"), "EUR", id.toString());
      assertThat(success.status()).isEqualTo("CAPTURED");
      assertThat(gateway.charge(id, new BigDecimal("4.00"), "EUR", id.toString()))
          .isEqualTo(success);
      var r = gateway.refund(success.reference(), new BigDecimal("4.00"), "r-" + id);
      assertThat(r.status()).isEqualTo("SUCCEEDED");
      assertThat(gateway.refund(success.reference(), new BigDecimal("4.00"), "r-" + id))
          .isEqualTo(r);
      var p = new Payment(new BigDecimal("4.00"), "EUR");
      p.method = method;
      assertThat(p.getAmount()).isEqualByComparingTo("4.00");
    }
    assertThat(
            gateway.charge(UUID.randomUUID(), new BigDecimal("13.13"), "EUR", "decline").status())
        .isEqualTo("DECLINED");
    assertThat(
            gateway.charge(UUID.randomUUID(), new BigDecimal("14.14"), "EUR", "timeout").status())
        .isEqualTo("TIMEOUT");
    var f = gateway.charge(UUID.randomUUID(), new BigDecimal("15.15"), "EUR", "refund-failure");
    assertThat(gateway.refund(f.reference(), new BigDecimal("15.15"), "f").status())
        .isEqualTo("PERMANENT_FAILURE");
    assertThat(gateway.refund(f.reference(), new BigDecimal("20"), "too-much").status())
        .isEqualTo("PERMANENT_FAILURE");
  }
}

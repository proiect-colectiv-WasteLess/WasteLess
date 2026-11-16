package ro.wasteless.common.port;

import java.util.UUID;

public interface PaymentGateway {
  default Result authorize(
      UUID orderId,
      java.math.BigDecimal amount,
      String currency,
      String method,
      String idempotencyKey) {
    throw new UnsupportedOperationException("Authorization is not supported by this provider");
  }

  default Result capture(
      UUID orderId,
      String authorizationReference,
      java.math.BigDecimal amount,
      String currency,
      String idempotencyKey) {
    throw new UnsupportedOperationException("Capture is not supported by this provider");
  }

  Result charge(UUID orderId, java.math.BigDecimal amount, String currency, String idempotencyKey);

  Result refund(String paymentReference, java.math.BigDecimal amount, String idempotencyKey);

  record Result(String reference, String status) {}
}

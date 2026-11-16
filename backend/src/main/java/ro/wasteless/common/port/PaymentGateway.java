package ro.wasteless.common.port;

import java.util.UUID;

public interface PaymentGateway {
  Result charge(UUID orderId, java.math.BigDecimal amount, String currency, String idempotencyKey);

  Result refund(String paymentReference, java.math.BigDecimal amount, String idempotencyKey);

  record Result(String reference, String status) {}
}

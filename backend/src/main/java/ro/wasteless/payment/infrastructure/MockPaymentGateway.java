package ro.wasteless.payment.infrastructure;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import ro.wasteless.common.port.PaymentGateway;

/** Deterministic local provider. Never handles card numbers or calls a bank. */
@Component
@ConditionalOnProperty(name = "app.payment-mode", havingValue = "mock")
public class MockPaymentGateway implements PaymentGateway {
  public Result authorize(
      UUID order, BigDecimal amount, String currency, String method, String key) {
    return once(
        "authorize:" + key,
        order + ":" + amount + ":" + currency + ":" + method,
        () -> {
          if (amount.compareTo(new BigDecimal("13.13")) == 0) return new Result(null, "DECLINED");
          if (amount.compareTo(new BigDecimal("14.14")) == 0) return new Result(null, "TIMEOUT");
          return new Result("mock-auth-" + order, "AUTHORIZED");
        });
  }

  public Result capture(
      UUID order, String authorization, BigDecimal amount, String currency, String key) {
    if (!("mock-auth-" + order).equals(authorization)) return new Result(null, "DECLINED");
    return charge(order, amount, currency, key);
  }

  private record Saved(String fingerprint, Result result) {}

  private final ConcurrentHashMap<String, Saved> operations = new ConcurrentHashMap<>();
  private final ConcurrentHashMap<String, BigDecimal> captured = new ConcurrentHashMap<>();
  private final ConcurrentHashMap<String, AtomicInteger> attempts = new ConcurrentHashMap<>();

  public Result charge(UUID order, BigDecimal amount, String currency, String key) {
    return once(
        "charge:" + key,
        order + ":" + amount.toPlainString() + ":" + currency,
        () -> {
          if (amount.compareTo(new BigDecimal("13.13")) == 0) return new Result(null, "DECLINED");
          if (amount.compareTo(new BigDecimal("14.14")) == 0) return new Result(null, "TIMEOUT");
          String ref = "mock-pay-" + order;
          captured.put(ref, amount);
          return new Result(ref, "CAPTURED");
        });
  }

  public Result refund(String reference, BigDecimal amount, String key) {
    BigDecimal paid = captured.get(reference);
    if (paid == null || amount.signum() <= 0 || amount.compareTo(paid) > 0)
      return new Result(null, "PERMANENT_FAILURE");
    if (paid.compareTo(new BigDecimal("15.15")) == 0) return new Result(null, "PERMANENT_FAILURE");
    if (paid.compareTo(new BigDecimal("16.16")) == 0
        && attempts.computeIfAbsent(key, k -> new AtomicInteger()).getAndIncrement() < 2)
      return new Result(null, "TRANSIENT_FAILURE");
    return once(
        "refund:" + key,
        reference + ":" + amount.toPlainString(),
        () -> new Result("mock-refund-" + key, "SUCCEEDED"));
  }

  private Result once(String key, String fingerprint, java.util.function.Supplier<Result> action) {
    Saved saved = operations.computeIfAbsent(key, k -> new Saved(fingerprint, action.get()));
    if (!saved.fingerprint.equals(fingerprint)) return new Result(null, "IDEMPOTENCY_MISMATCH");
    return saved.result;
  }
}

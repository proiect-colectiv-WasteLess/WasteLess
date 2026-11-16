package ro.wasteless.order.api;

import jakarta.validation.constraints.*;
import java.util.UUID;
import ro.wasteless.payment.domain.PaymentMethod;

public final class OrderRequests {
  private OrderRequests() {}

  public record Place(
      @NotNull UUID offerId,
      @NotNull @Min(1) @Max(10000) Integer quantity,
      @NotNull PaymentMethod paymentMethod) {}

  public record Cancel(@Size(max = 280) String reason) {}

  public record Validate(@NotNull @Pattern(regexp = "[0-9]{4}") String code) {}
}

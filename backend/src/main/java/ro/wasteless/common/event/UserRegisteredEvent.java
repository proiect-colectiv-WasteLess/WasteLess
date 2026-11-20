package ro.wasteless.common.event;

import java.time.Instant;
import java.util.UUID;

public record UserRegisteredEvent(
    UUID userId,
    Instant occurredAt,
    String businessName,
    java.math.BigDecimal latitude,
    java.math.BigDecimal longitude) {
  public UserRegisteredEvent(UUID userId, Instant occurredAt) {
    this(userId, occurredAt, null, null, null);
  }
}

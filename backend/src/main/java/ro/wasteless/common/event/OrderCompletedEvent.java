package ro.wasteless.common.event;

import java.time.Instant;
import java.util.UUID;

public record OrderCompletedEvent(UUID orderId, UUID userId, Instant occurredAt) {}

package ro.wasteless.common.event;

import java.time.Instant;
import java.util.UUID;

public record OrderCanceledEvent(UUID orderId, UUID userId, Instant occurredAt) {}

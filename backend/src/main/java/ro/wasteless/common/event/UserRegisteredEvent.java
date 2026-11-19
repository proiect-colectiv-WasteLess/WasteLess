package ro.wasteless.common.event;

import java.time.Instant;
import java.util.UUID;

public record UserRegisteredEvent(UUID userId, Instant occurredAt) {}

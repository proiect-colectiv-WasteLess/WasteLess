package ro.wasteless.common.event;

import java.time.Instant;
import java.util.UUID;

public record AccountDeletionRequestedEvent(UUID userId, Instant occurredAt) {}

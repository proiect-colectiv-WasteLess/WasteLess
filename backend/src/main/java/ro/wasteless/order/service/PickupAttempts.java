package ro.wasteless.order.service;

import java.time.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import ro.wasteless.common.api.*;

@Component
public class PickupAttempts {
  private record Entry(int failures, Instant expiresAt) {}

  private final Map<UUID, Entry> entries = new HashMap<>();
  private final Clock clock;
  private final int limit;
  private final long window;

  public PickupAttempts(
      Clock clock,
      @Value("${app.orders.pickup-attempt-limit:5}") int limit,
      @Value("${app.orders.pickup-attempt-window-seconds:300}") long window) {
    if (limit < 1 || window < 1) throw new IllegalArgumentException("Invalid pickup rate limit");
    this.clock = clock;
    this.limit = limit;
    this.window = window;
  }

  public synchronized void check(UUID order) {
    entries.entrySet().removeIf(e -> !e.getValue().expiresAt.isAfter(clock.instant()));
    Entry entry = entries.get(order);
    if ((entry != null && entry.failures >= limit) || (entry == null && entries.size() >= 10000))
      throw new DomainException(
          ErrorCode.RATE_LIMITED,
          HttpStatus.TOO_MANY_REQUESTS,
          "Too many pickup attempts; retry later");
  }

  public synchronized void failed(UUID order) {
    Entry old = entries.get(order);
    entries.put(
        order,
        new Entry(
            old == null ? 1 : old.failures + 1,
            old == null ? clock.instant().plusSeconds(window) : old.expiresAt));
  }

  public synchronized void clear(UUID order) {
    entries.remove(order);
  }
}

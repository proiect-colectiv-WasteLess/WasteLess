package ro.wasteless.auth.infrastructure;

import io.github.bucket4j.*;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Component;
import ro.wasteless.auth.application.AccountService;

@Component
public class AuthRateLimiter {
  private record Entry(Bucket bucket, Instant expiresAt) {}

  private final Map<String, Entry> entries = new HashMap<>();
  private final Clock clock;

  public AuthRateLimiter(Clock clock) {
    this.clock = clock;
  }

  public void account(String endpoint, String email) {
    int capacity = endpoint.equals("login") ? 20 : 5;
    consume(
        "account:" + endpoint + ":" + AccountService.hash(email.trim().toLowerCase(Locale.ROOT)),
        capacity,
        Duration.ofMinutes(15));
  }

  public void ip(String address) {
    consume("ip:" + address, 100, Duration.ofMinutes(15));
  }

  private synchronized void consume(String key, int capacity, Duration window) {
    var now = clock.instant();
    entries.entrySet().removeIf(e -> !e.getValue().expiresAt().isAfter(now));
    var entry = entries.get(key);
    if (entry == null) {
      if (entries.size() >= 100000) throw new ThrottledException(900);
      entry =
          new Entry(
              Bucket.builder()
                  .addLimit(l -> l.capacity(capacity).refillGreedy(capacity, window))
                  .build(),
              now.plus(window));
      entries.put(key, entry);
    }
    var probe = entry.bucket().tryConsumeAndReturnRemaining(1);
    if (!probe.isConsumed())
      throw new ThrottledException(
          Math.max(1, (probe.getNanosToWaitForRefill() + 999999999L) / 1000000000L));
  }

  public static final class ThrottledException extends RuntimeException {
    public final long retryAfter;

    public ThrottledException(long seconds) {
      super("Too many requests");
      retryAfter = seconds;
    }
  }
}

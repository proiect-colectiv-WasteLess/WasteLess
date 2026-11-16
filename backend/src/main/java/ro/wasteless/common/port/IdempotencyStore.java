package ro.wasteless.common.port;

import java.util.UUID;

public interface IdempotencyStore {
  boolean claim(
      UUID userId, String scope, String key, String requestHash, java.time.Instant expiresAt);

  java.util.Optional<Result> find(UUID userId, String scope, String key);

  void complete(UUID userId, String scope, String key, int status, String responseBody);

  record Result(String requestHash, Integer status, String responseBody) {}
}

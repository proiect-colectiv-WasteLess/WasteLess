package ro.wasteless.order.service;

import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.Clock;
import java.util.*;
import java.util.function.Supplier;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ro.wasteless.common.api.*;
import ro.wasteless.common.port.IdempotencyStore;
import ro.wasteless.common.security.CurrentUser;
import tools.jackson.databind.json.JsonMapper;

@Service
public class IdempotentOrders {
  private final IdempotencyStore store;
  private final CurrentUser user;
  private final JsonMapper json;
  private final Clock clock;

  public IdempotentOrders(IdempotencyStore store, CurrentUser user, JsonMapper json, Clock clock) {
    this.store = store;
    this.user = user;
    this.json = json;
    this.clock = clock;
  }

  @Transactional
  public ResponseEntity<String> execute(
      String scope, String key, Object request, int status, Supplier<?> action) {
    if (key == null || key.isBlank() || key.length() > 100)
      throw new DomainException(
          ErrorCode.VALIDATION_FAILED,
          HttpStatus.BAD_REQUEST,
          "Idempotency-Key must contain 1 to 100 characters");
    UUID actor = user.id();
    String hash = hash(json.writeValueAsString(request));
    if (!store.claim(actor, scope, key, hash, clock.instant().plusSeconds(86400))) {
      var saved =
          store
              .find(actor, scope, key)
              .orElseThrow(
                  () ->
                      new DomainException(
                          ErrorCode.CONFLICT,
                          HttpStatus.CONFLICT,
                          "Request is being processed; retry"));
      if (!hash.equals(saved.requestHash()))
        throw new DomainException(
            ErrorCode.IDEMPOTENCY_MISMATCH,
            HttpStatus.CONFLICT,
            "Key was used for a different request");
      if (saved.status() == null)
        throw new DomainException(
            ErrorCode.CONFLICT, HttpStatus.CONFLICT, "Request is being processed; retry");
      return ResponseEntity.status(saved.status())
          .contentType(
              saved.status() >= 400
                  ? MediaType.APPLICATION_PROBLEM_JSON
                  : MediaType.APPLICATION_JSON)
          .body(saved.responseBody());
    }
    Object result = action.get();
    Object payload = result;
    if (result instanceof ResponseEntity<?> response) {
      status = response.getStatusCode().value();
      payload = response.getBody();
    }
    String body = json.writeValueAsString(payload);
    store.complete(actor, scope, key, status, body);
    // Claim, business effects and reply become visible together at commit.
    return ResponseEntity.status(status)
        .contentType(
            status >= 400 ? MediaType.APPLICATION_PROBLEM_JSON : MediaType.APPLICATION_JSON)
        .body(body);
  }

  public static String hash(String value) {
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }
}

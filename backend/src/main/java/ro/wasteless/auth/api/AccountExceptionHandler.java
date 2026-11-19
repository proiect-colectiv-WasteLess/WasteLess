package ro.wasteless.auth.api;

import org.springframework.core.annotation.Order;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import ro.wasteless.auth.application.DuplicateEmailException;
import ro.wasteless.common.api.*;

@RestControllerAdvice
@Order(-1)
public class AccountExceptionHandler {
  @ExceptionHandler(ro.wasteless.auth.infrastructure.AuthRateLimiter.ThrottledException.class)
  ResponseEntity<ProblemDetail> throttled(
      ro.wasteless.auth.infrastructure.AuthRateLimiter.ThrottledException e) {
    return ResponseEntity.status(429)
        .header("Retry-After", Long.toString(e.retryAfter))
        .body(
            ProblemResponses.of(
                HttpStatus.TOO_MANY_REQUESTS, ErrorCode.RATE_LIMITED, e.getMessage()));
  }

  @ExceptionHandler(DuplicateEmailException.class)
  ProblemDetail duplicate(DuplicateEmailException e) {
    var p = ProblemResponses.of(HttpStatus.CONFLICT, ErrorCode.CONFLICT, e.getMessage());
    p.setProperty(
        "fieldErrors",
        java.util.List.of(java.util.Map.of("field", "email", "message", e.getMessage())));
    return p;
  }
}

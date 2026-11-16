package ro.wasteless.common.api;

import org.slf4j.MDC;
import org.springframework.http.*;

public final class ProblemResponses {
  private ProblemResponses() {}

  public static ProblemDetail of(HttpStatus status, ErrorCode code, String message) {
    var problem = ProblemDetail.forStatusAndDetail(status, message);
    problem.setTitle(status.getReasonPhrase());
    problem.setProperty("code", code.name());
    problem.setProperty("message", message);
    problem.setProperty("traceId", MDC.get("requestId"));
    problem.setProperty("fieldErrors", java.util.List.of());
    return problem;
  }
}

package ro.wasteless.auth.infrastructure;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import ro.wasteless.common.api.*;
import tools.jackson.databind.json.JsonMapper;

final class SecurityProblems {
  private SecurityProblems() {}

  static void write(HttpServletResponse response, JsonMapper mapper, int status, String message)
      throws IOException {
    response.setStatus(status);
    response.setContentType("application/problem+json");
    var code =
        status == 429
            ? ErrorCode.RATE_LIMITED
            : status == 403 ? ErrorCode.FORBIDDEN : ErrorCode.UNAUTHORIZED;
    response
        .getWriter()
        .write(
            mapper.writeValueAsString(
                ProblemResponses.of(HttpStatus.valueOf(status), code, message)));
  }
}

package ro.wasteless.auth.infrastructure;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.json.JsonMapper;

final class AuthRateFilter extends OncePerRequestFilter {
  private final AuthRateLimiter limiter;
  private final JsonMapper mapper;

  AuthRateFilter(AuthRateLimiter limiter, JsonMapper mapper) {
    this.limiter = limiter;
    this.mapper = mapper;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    if (request.getMethod().equals("POST")
        && request.getRequestURI().startsWith(request.getContextPath() + "/api/v1/auth/")) {
      try {
        limiter.ip(request.getRemoteAddr());
      } catch (AuthRateLimiter.ThrottledException e) {
        response.setHeader("Retry-After", Long.toString(e.retryAfter));
        SecurityProblems.write(response, mapper, 429, e.getMessage());
        return;
      }
    }
    chain.doFilter(request, response);
  }
}

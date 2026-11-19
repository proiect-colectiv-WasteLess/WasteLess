package ro.wasteless.auth.infrastructure;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.time.Clock;
import java.util.UUID;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;
import ro.wasteless.auth.domain.UserStatus;
import tools.jackson.databind.json.JsonMapper;

final class AccountStatusFilter extends OncePerRequestFilter {
  private final UserRepository users;
  private final Clock clock;
  private final JsonMapper mapper;

  AccountStatusFilter(UserRepository users, Clock clock, JsonMapper mapper) {
    this.users = users;
    this.clock = clock;
    this.mapper = mapper;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    var auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth instanceof JwtAuthenticationToken jwt) {
      var user = users.findById(UUID.fromString(jwt.getName())).orElse(null);
      boolean repeatedDelete =
          user != null
              && user.status == UserStatus.DELETED
              && request.getMethod().equals("DELETE")
              && request.getRequestURI().equals(request.getContextPath() + "/api/v1/users/me");
      if (user == null || (!repeatedDelete && !user.canAuthenticate(clock.instant()))) {
        SecurityContextHolder.clearContext();
        SecurityProblems.write(response, mapper, 401, "Authentication required");
        return;
      }
    }
    chain.doFilter(request, response);
  }
}

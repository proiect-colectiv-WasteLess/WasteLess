package ro.wasteless.common.security;

import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import ro.wasteless.common.api.*;

@Component
public class CurrentUser {
  public UUID id() {
    var auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken)
      throw new DomainException(
          ErrorCode.UNAUTHORIZED, HttpStatus.UNAUTHORIZED, "Authentication required");
    try {
      return UUID.fromString(auth.getName());
    } catch (IllegalArgumentException e) {
      throw new DomainException(
          ErrorCode.UNAUTHORIZED, HttpStatus.UNAUTHORIZED, "Invalid user identity");
    }
  }
}

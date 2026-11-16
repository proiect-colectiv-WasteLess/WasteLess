package ro.wasteless.common.security;

import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import ro.wasteless.common.api.*;

@Component
public class OwnershipGuard {
  private final CurrentUser currentUser;

  public OwnershipGuard(CurrentUser user) {
    currentUser = user;
  }

  public void requireOwner(UUID ownerId) {
    if (!currentUser.id().equals(ownerId))
      throw new DomainException(ErrorCode.FORBIDDEN, HttpStatus.FORBIDDEN, "Access denied");
  }
}

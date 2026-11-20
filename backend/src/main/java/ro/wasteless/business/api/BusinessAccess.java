package ro.wasteless.business.api;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ro.wasteless.business.domain.Business;
import ro.wasteless.business.infrastructure.BusinessRepository;
import ro.wasteless.common.api.*;
import ro.wasteless.common.security.CurrentUser;

/** Public business boundary used by catalog and media features. */
@Service
public class BusinessAccess {
  private final BusinessRepository businesses;
  private final CurrentUser user;

  public BusinessAccess(BusinessRepository businesses, CurrentUser user) {
    this.businesses = businesses;
    this.user = user;
  }

  @Transactional(readOnly = true)
  public Business current() {
    return businesses
        .findByOwnerUserIdAndDeletedAtIsNull(user.id())
        .orElseThrow(BusinessAccess::notFound);
  }

  @Transactional
  public Business lockCurrent() {
    return businesses.lockByOwner(user.id()).orElseThrow(BusinessAccess::notFound);
  }

  public static DomainException notFound() {
    return new DomainException(ErrorCode.NOT_FOUND, HttpStatus.NOT_FOUND, "Resource not found");
  }
}

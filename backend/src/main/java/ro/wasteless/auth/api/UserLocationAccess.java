package ro.wasteless.auth.api;

import java.math.BigDecimal;
import java.util.Optional;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ro.wasteless.auth.infrastructure.*;
import ro.wasteless.common.security.CurrentUser;

/** Discovery may obtain only the authenticated customer's consented location. */
@Service
public class UserLocationAccess {
  private final UserRepository users;
  private final UserLocationRepository locations;
  private final CurrentUser current;

  public UserLocationAccess(
      UserRepository users, UserLocationRepository locations, CurrentUser current) {
    this.users = users;
    this.locations = locations;
    this.current = current;
  }

  @Transactional(readOnly = true)
  @PreAuthorize("hasRole('CLIENT')")
  public Optional<Coordinates> currentLocation() {
    var id = current.id();
    return users
        .findById(id)
        .filter(u -> u.locationSharingEnabled && u.deletedAt == null)
        .flatMap(u -> locations.findById(id))
        .map(l -> new Coordinates(l.latitude, l.longitude));
  }

  public record Coordinates(BigDecimal latitude, BigDecimal longitude) {}
}

package ro.wasteless.business.infrastructure;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.*;
import org.springframework.transaction.event.*;
import ro.wasteless.business.domain.*;
import ro.wasteless.common.event.UserRegisteredEvent;

/** Accepts A's event both from a transaction and from A's after-commit publisher. */
@Component
public class BusinessProvisioning {
  private final BusinessRepository businesses;
  private final BusinessLocationRepository locations;
  private final JdbcTemplate jdbc;

  public BusinessProvisioning(
      BusinessRepository businesses, BusinessLocationRepository locations, JdbcTemplate jdbc) {
    this.businesses = businesses;
    this.locations = locations;
    this.jdbc = jdbc;
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void provision(UserRegisteredEvent event) {
    if (event.businessName() == null || event.businessName().isBlank()) return;
    jdbc.queryForList("select id from users where id=? for update", event.userId());
    var businessRole =
        jdbc.queryForObject(
            "select count(*) from user_roles ur join roles r on r.id=ur.role_id where ur.user_id=? and r.code in ('BUSINESS','ROLE_BUSINESS')",
            Long.class,
            event.userId());
    if (businessRole == null
        || businessRole == 0
        || businesses.findByOwnerUserId(event.userId()).isPresent()) return;
    if (event.businessName().trim().length() > 120)
      throw new IllegalArgumentException("Invalid business name in registration event");
    var b = businesses.saveAndFlush(new Business(event.userId(), event.businessName().trim()));
    if (event.latitude() != null && event.longitude() != null) {
      if (event.latitude().abs().doubleValue() > 90 || event.longitude().abs().doubleValue() > 180)
        throw new IllegalArgumentException("Invalid registration coordinates");
      var l = new BusinessLocation();
      l.businessId = b.getId();
      l.label = "Registration location";
      // A's event has no postal address or country. ZZ means unspecified; B3 completes these
      // fields.
      l.addressLine1 = "";
      l.city = "";
      l.countryCode = "ZZ";
      l.latitude = event.latitude();
      l.longitude = event.longitude();
      l.isPrimary = true;
      locations.save(l);
    }
  }
}

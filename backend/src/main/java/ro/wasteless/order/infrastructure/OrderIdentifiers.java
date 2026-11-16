package ro.wasteless.order.infrastructure;

import java.security.SecureRandom;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.*;
import ro.wasteless.common.api.*;

@Component
public class OrderIdentifiers {
  private final JdbcTemplate jdbc;
  private final SecureRandom random = new SecureRandom();

  public OrderIdentifiers(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public String nextReference() {
    return "WL-"
        + String.format(
            java.util.Locale.ROOT,
            "%05d",
            jdbc.queryForObject("select nextval('order_reference_seq')", Long.class));
  }

  @Transactional(propagation = Propagation.MANDATORY)
  public String pickupCode(UUID business) {
    // Serializes allocation per business; retained until order insert commits.
    jdbc.queryForObject("select id from businesses where id=? for update", UUID.class, business);
    int first = random.nextInt(10000);
    for (int i = 0; i < 10000; i++) {
      String code = String.format(java.util.Locale.ROOT, "%04d", (first + i) % 10000);
      if (jdbc.queryForObject(
              "select count(*) from orders where business_id=? and status='ACTIVE' and pickup_code=?",
              Integer.class,
              business,
              code)
          == 0) return code;
    }
    throw new DomainException(ErrorCode.CONFLICT, HttpStatus.CONFLICT, "No pickup codes available");
  }
}

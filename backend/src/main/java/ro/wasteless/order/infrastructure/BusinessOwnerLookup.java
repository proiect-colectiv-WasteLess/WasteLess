package ro.wasteless.order.infrastructure;

import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import ro.wasteless.common.api.*;
import ro.wasteless.common.security.CurrentUser;

@Component
public class BusinessOwnerLookup {
  private final JdbcTemplate jdbc;
  private final CurrentUser user;

  public BusinessOwnerLookup(JdbcTemplate jdbc, CurrentUser user) {
    this.jdbc = jdbc;
    this.user = user;
  }

  public UUID currentBusiness() {
    return jdbc
        .query(
            "select id from businesses where owner_user_id=? and deleted_at is null and status='ACTIVE'",
            (r, n) -> r.getObject(1, UUID.class),
            user.id())
        .stream()
        .findFirst()
        .orElseThrow(
            () ->
                new DomainException(
                    ErrorCode.NOT_FOUND, HttpStatus.NOT_FOUND, "Business not found"));
  }

  public String customerName(UUID customer) {
    var names =
        jdbc.query(
            "select first_name,last_name from users where id=?",
            (r, n) -> new String[] {r.getString(1), r.getString(2)},
            customer);
    if (names.isEmpty()) return "Customer";
    String first = names.getFirst()[0], last = names.getFirst()[1];
    first = first == null || first.isBlank() ? "Customer" : first.strip();
    return last == null || last.isBlank()
        ? first
        : first + " " + new String(Character.toChars(last.strip().codePointAt(0))) + ".";
  }
}

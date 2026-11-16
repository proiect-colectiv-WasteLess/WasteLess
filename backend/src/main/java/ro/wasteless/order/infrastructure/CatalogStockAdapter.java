package ro.wasteless.order.infrastructure;

import java.sql.Timestamp;
import java.time.Clock;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.*;
import ro.wasteless.common.api.*;
import ro.wasteless.common.port.OfferStockPort;

/** Compatibility adapter for the P12 catalog until the B6 adapter is present. */
public class CatalogStockAdapter implements OfferStockPort {
  private final JdbcTemplate jdbc;
  private final Clock clock;

  public CatalogStockAdapter(JdbcTemplate jdbc, Clock clock) {
    this.jdbc = jdbc;
    this.clock = clock;
  }

  @Transactional(propagation = Propagation.MANDATORY)
  public void reserve(UUID offer, int quantity) {
    if (quantity <= 0) throw new IllegalArgumentException("Quantity must be positive");
    int updated =
        jdbc.update(
            """
            update offers set quantity_available=quantity_available-?,
            status=case when quantity_available=? then 'SOLD_OUT' else status end,
            updated_at=?,version=version+1
            where id=? and status='ACTIVE' and deleted_at is null and quantity_available>=?
              and pickup_end_at>? and (expires_at is null or expires_at>?)
            """,
            quantity,
            quantity,
            Timestamp.from(clock.instant()),
            offer,
            quantity,
            Timestamp.from(clock.instant()),
            Timestamp.from(clock.instant()));
    if (updated == 0) {
      var available =
          jdbc.query(
              "select quantity_available from offers where id=? and status in ('ACTIVE','SOLD_OUT') and deleted_at is null and pickup_end_at>? and (expires_at is null or expires_at>?)",
              (r, n) -> r.getInt(1),
              offer,
              Timestamp.from(clock.instant()),
              Timestamp.from(clock.instant()));
      throw new DomainException(
          available.isEmpty() ? ErrorCode.OFFER_NOT_AVAILABLE : ErrorCode.OFFER_SOLD_OUT,
          HttpStatus.CONFLICT,
          available.isEmpty() ? "Offer is not available" : "Insufficient stock");
    }
  }

  @Transactional(propagation = Propagation.MANDATORY)
  public void release(UUID offer, int quantity) {
    if (offer == null) return; // Snapshot order remains valid after hard deletion.
    if (quantity <= 0) throw new IllegalArgumentException("Quantity must be positive");
    jdbc.update(
        """
            update offers set quantity_available=quantity_available+?,
              status=case when status='SOLD_OUT' then
                case when pickup_end_at>? and (expires_at is null or expires_at>?) then 'ACTIVE' else 'EXPIRED' end
                else status end,updated_at=?,version=version+1 where id=?
            """,
        quantity,
        Timestamp.from(clock.instant()),
        Timestamp.from(clock.instant()),
        Timestamp.from(clock.instant()),
        offer);
  }
}

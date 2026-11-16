package ro.wasteless.order.infrastructure;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import ro.wasteless.common.api.*;

@Repository
public class OfferSnapshotReader {
  public record Snapshot(
      UUID id,
      UUID businessId,
      UUID locationId,
      String itemName,
      String businessName,
      String imageUrl,
      BigDecimal price,
      BigDecimal originalPrice,
      String currency,
      Instant pickupStart,
      Instant pickupEnd) {}

  private final JdbcTemplate jdbc;

  public OfferSnapshotReader(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public Snapshot read(UUID offer, boolean lock) {
    var result =
        jdbc.query(
            """
            select o.*,b.name business_name from offers o join businesses b on b.id=o.business_id
            where o.id=? and o.deleted_at is null and b.deleted_at is null and b.status='ACTIVE'
            """
                + (lock ? " for update of o" : ""),
            (r, n) ->
                new Snapshot(
                    r.getObject("id", UUID.class),
                    r.getObject("business_id", UUID.class),
                    r.getObject("location_id", UUID.class),
                    r.getString("title"),
                    r.getString("business_name"),
                    r.getString("image_url"),
                    r.getBigDecimal("price"),
                    r.getBigDecimal("original_price"),
                    r.getString("currency"),
                    r.getTimestamp("pickup_start_at").toInstant(),
                    r.getTimestamp("pickup_end_at").toInstant()),
            offer);
    if (result.isEmpty())
      throw new DomainException(
          ErrorCode.OFFER_NOT_AVAILABLE, HttpStatus.CONFLICT, "Offer is not available");
    return result.getFirst();
  }
}

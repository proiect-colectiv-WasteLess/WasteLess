package ro.wasteless.offer.domain;

import jakarta.persistence.*;
import java.math.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import ro.wasteless.common.api.*;
import ro.wasteless.common.domain.BaseEntity;

@Entity
@Table(name = "offers")
public class Offer extends BaseEntity {
  public UUID businessId;
  public UUID locationId;
  public UUID categoryId;
  public String title;
  public String description;
  public String imageUrl;

  @Column(precision = 10, scale = 2)
  public BigDecimal originalPrice;

  @Column(precision = 10, scale = 2)
  public BigDecimal price;

  @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.CHAR)
  @Column(columnDefinition = "char(3)", length = 3)
  public String currency = "EUR";

  public int quantityTotal;
  public int quantityAvailable;
  public Instant pickupStartAt;
  public Instant pickupEndAt;

  @Enumerated(EnumType.STRING)
  private OfferStatus status = OfferStatus.ACTIVE;

  public Instant publishedAt;
  public Instant pausedAt;
  public Instant soldOutAt;
  public Instant expiresAt;
  public Instant deletedAt;

  public OfferStatus getStatus() {
    return deletedAt == null ? status : OfferStatus.DELETED;
  }

  public String stockLevel() {
    return quantityAvailable <= 10 ? "low" : quantityAvailable <= 25 ? "medium" : "high";
  }

  public int discountPercent() {
    if (originalPrice == null || originalPrice.signum() <= 0) return 0;
    return originalPrice
        .subtract(price)
        .multiply(BigDecimal.valueOf(100))
        .divide(originalPrice, 0, RoundingMode.HALF_UP)
        .intValue();
  }

  public String pickupWindow() {
    return time(pickupStartAt) + "–" + time(pickupEndAt);
  }

  public static String time(Instant time) {
    return DateTimeFormatter.ofPattern("HH:mm")
        .withZone(ZoneId.of("Europe/Bucharest"))
        .format(time);
  }

  public boolean orderable(Instant now) {
    return deletedAt == null
        && status == OfferStatus.ACTIVE
        && quantityAvailable > 0
        && pickupEndAt.isAfter(now);
  }

  public void pause(Instant now) {
    requireLive(now);
    if (status == OfferStatus.PAUSED) return;
    if (status != OfferStatus.ACTIVE && status != OfferStatus.SOLD_OUT) conflict();
    status = OfferStatus.PAUSED;
    pausedAt = now;
  }

  public void resume(Instant now) {
    requireLive(now);
    status = quantityAvailable == 0 ? OfferStatus.SOLD_OUT : OfferStatus.ACTIVE;
    pausedAt = null;
  }

  public void delete(Instant now) {
    if (deletedAt != null) return;
    deletedAt = now;
    status = OfferStatus.PAUSED; // P11 status check has no DELETED; deleted_at is authoritative.
  }

  public void reserve(int quantity, Instant now) {
    positive(quantity);
    if (!orderable(now))
      throw new DomainException(
          quantityAvailable == 0 ? ErrorCode.OFFER_SOLD_OUT : ErrorCode.OFFER_NOT_AVAILABLE,
          HttpStatus.CONFLICT,
          "Offer is not available");
    if (quantity > quantityAvailable)
      throw new DomainException(
          ErrorCode.OFFER_SOLD_OUT, HttpStatus.CONFLICT, "Insufficient stock");
    quantityAvailable -= quantity;
    if (quantityAvailable == 0) {
      status = OfferStatus.SOLD_OUT;
      soldOutAt = now;
    }
  }

  public void restore(int quantity, Instant now) {
    positive(quantity);
    if (deletedAt != null) return;
    quantityAvailable = (int) Math.min(quantityTotal, (long) quantityAvailable + quantity);
    reconcile(now);
  }

  public void changeQuantity(int total) {
    int reserved = quantityTotal - quantityAvailable;
    if (total < reserved)
      throw new DomainException(
          ErrorCode.CONFLICT,
          HttpStatus.CONFLICT,
          "Quantity cannot be lower than already reserved units");
    quantityTotal = total;
    quantityAvailable = total - reserved;
  }

  public boolean reconcile(Instant now) {
    if (deletedAt != null) return false;
    var before = status;
    if (!pickupEndAt.isAfter(now)) status = OfferStatus.EXPIRED;
    else if (status != OfferStatus.PAUSED && status != OfferStatus.EXPIRED) {
      if (quantityAvailable == 0) {
        status = OfferStatus.SOLD_OUT;
        if (soldOutAt == null) soldOutAt = now;
      } else {
        status = OfferStatus.ACTIVE;
        soldOutAt = null;
      }
    }
    return before != status;
  }

  private void requireLive(Instant now) {
    if (deletedAt != null || status == OfferStatus.EXPIRED || !pickupEndAt.isAfter(now)) conflict();
  }

  private void positive(int quantity) {
    if (quantity <= 0) throw new IllegalArgumentException("Quantity must be positive");
  }

  private void conflict() {
    throw new DomainException(
        ErrorCode.CONFLICT, HttpStatus.CONFLICT, "Invalid offer state transition");
  }
}

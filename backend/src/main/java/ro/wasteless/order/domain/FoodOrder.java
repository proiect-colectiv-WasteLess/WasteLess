package ro.wasteless.order.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import ro.wasteless.common.api.*;

@Entity
@Table(name = "orders")
public class FoodOrder {
  @Id public UUID id = UUID.randomUUID();

  @Column(nullable = false, updatable = false)
  public String reference;

  @Column(nullable = false, updatable = false)
  public UUID customerUserId;

  @Column(nullable = false, updatable = false)
  public UUID businessId;

  @Column(nullable = false, updatable = false)
  public UUID businessLocationId;

  public UUID offerId;

  @Enumerated(EnumType.STRING)
  public OrderStatus status = OrderStatus.ACTIVE;

  @Column(updatable = false, columnDefinition = "char(3)")
  public String currency;

  @Column(updatable = false)
  public int quantity;

  @Column(updatable = false, precision = 10, scale = 2)
  public BigDecimal unitPrice;

  @Column(updatable = false, precision = 10, scale = 2)
  public BigDecimal originalUnitPrice;

  @Column(updatable = false, precision = 10, scale = 2)
  public BigDecimal totalPrice;

  @Column(updatable = false)
  public String itemName;

  @Column(updatable = false)
  public String businessName;

  @Column(updatable = false, columnDefinition = "text")
  public String imageUrl;

  @Column(updatable = false)
  public Instant pickupStartAt;

  @Column(updatable = false)
  public Instant pickupEndAt;

  @Column(updatable = false)
  public String pickupCode;

  public Instant canceledAt;
  public String cancelReason;
  public Instant completedAt;
  public UUID completedByUserId;

  @Column(updatable = false)
  public Instant createdAt;

  public Instant updatedAt;
  @Version public long version;

  public FoodOrder() {}

  public void complete(UUID actor, Instant now) {
    requireActive();
    status = OrderStatus.COMPLETED;
    completedAt = now;
    completedByUserId = actor;
    updatedAt = now;
  }

  public void cancel(String reason, Instant now) {
    requireActive();
    status = OrderStatus.CANCELED;
    canceledAt = now;
    cancelReason = reason;
    updatedAt = now;
  }

  private void requireActive() {
    if (status != OrderStatus.ACTIVE)
      throw new DomainException(
          ErrorCode.CONFLICT, HttpStatus.CONFLICT, "Order is in a terminal state");
  }
}

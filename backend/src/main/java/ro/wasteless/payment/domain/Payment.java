package ro.wasteless.payment.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payments")
public class Payment {
  public enum Status {
    PENDING,
    AUTHORIZED,
    CAPTURED,
    FAILED,
    REFUNDED
  }

  @Id public UUID id = UUID.randomUUID();

  @Column(updatable = false)
  public UUID orderId;

  @Column(updatable = false)
  public UUID userId;

  @Enumerated(EnumType.STRING)
  @Column(updatable = false)
  public PaymentMethod method;

  @Enumerated(EnumType.STRING)
  public Status status = Status.PENDING;

  @Column(updatable = false, precision = 10, scale = 2)
  private BigDecimal amount;

  @Column(updatable = false, columnDefinition = "char(3)")
  @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.CHAR)
  private String currency;

  public String provider;
  public String providerReference;
  public String idempotencyKey;
  public String failureCode;
  public Instant capturedAt;

  @Column(updatable = false)
  public Instant createdAt;

  public Instant updatedAt;

  public Payment() {}

  public Payment(BigDecimal amount, String currency) {
    this.amount = amount;
    this.currency = currency;
  }

  public BigDecimal getAmount() {
    return amount;
  }

  public String getCurrency() {
    return currency;
  }
}

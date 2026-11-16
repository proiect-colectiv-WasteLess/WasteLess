package ro.wasteless.payment.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "refunds")
public class Refund {
  public enum Status {
    PENDING,
    SUCCEEDED,
    FAILED
  }

  @Id public UUID id;

  @Column(updatable = false)
  public UUID paymentId;

  @Column(updatable = false)
  public UUID orderId;

  @Column(updatable = false, precision = 10, scale = 2)
  public BigDecimal amount;

  @Column(updatable = false, columnDefinition = "char(3)")
  @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.CHAR)
  public String currency;

  @Enumerated(EnumType.STRING)
  public Status status;

  public String reason;
  public String providerReference;

  @Column(updatable = false)
  public Instant requestedAt;

  public Instant processedAt;
  public String failureReason;

  @Column(updatable = false)
  public Instant createdAt;

  public Instant updatedAt;

  protected Refund() {}
}

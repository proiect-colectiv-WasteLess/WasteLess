package ro.wasteless.business.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "business_locations")
public class BusinessLocation {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  public UUID id;

  public UUID businessId;
  public String label;
  public String addressLine1;
  public String addressLine2;
  public String city;
  public String postalCode;

  @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.CHAR)
  @Column(columnDefinition = "char(2)", length = 2)
  public String countryCode;

  @Column(precision = 9, scale = 6)
  public BigDecimal latitude;

  @Column(precision = 9, scale = 6)
  public BigDecimal longitude;

  public boolean isPrimary;
  public Instant createdAt;
  public Instant updatedAt;

  @PrePersist
  void create() {
    if (createdAt == null) createdAt = Instant.now();
    if (updatedAt == null) updatedAt = createdAt;
  }

  @PreUpdate
  void update() {
    updatedAt = Instant.now();
  }
}

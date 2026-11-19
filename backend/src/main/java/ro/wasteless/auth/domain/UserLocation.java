package ro.wasteless.auth.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "user_locations")
public class UserLocation {
  @Id public UUID userId;

  @Column(precision = 9, scale = 6)
  public BigDecimal latitude;

  @Column(precision = 9, scale = 6)
  public BigDecimal longitude;

  @Column(name = "accuracy_m", precision = 7, scale = 2)
  public BigDecimal accuracyMeters;

  public Instant createdAt;
  public Instant updatedAt;
}

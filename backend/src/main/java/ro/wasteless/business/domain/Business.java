package ro.wasteless.business.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import ro.wasteless.common.domain.BaseEntity;

@Entity
@Table(name = "businesses")
public class Business extends BaseEntity {
  @Column(nullable = false, unique = true)
  public UUID ownerUserId;

  public String name;
  public String slug;
  public String description;
  public String phone;

  @Column(columnDefinition = "citext")
  public String email;

  public String logoUrl;
  public String coverUrl;
  public String vatNumber;
  public BigDecimal ratingAvg = BigDecimal.ZERO;
  public int ratingCount;
  public boolean isVerified;
  public String status = "ACTIVE";
  public Instant deletedAt;

  protected Business() {}

  public Business(UUID owner, String name) {
    ownerUserId = owner;
    this.name = name;
    String stem = name.toLowerCase(java.util.Locale.ROOT).replaceAll("[^a-z0-9]+", "-");
    slug = stem.substring(0, Math.min(90, stem.length())) + "-" + owner;
  }
}

package ro.wasteless.auth.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@MappedSuperclass
public abstract class ActionToken {
  @Id public UUID id;
  public UUID userId;

  @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.CHAR)
  @Column(columnDefinition = "char(64)", length = 64)
  public String tokenHash;

  public Instant expiresAt;
  public Instant usedAt;
  public Instant createdAt;
}

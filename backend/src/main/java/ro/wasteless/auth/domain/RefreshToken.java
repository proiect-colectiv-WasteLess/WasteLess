package ro.wasteless.auth.domain;

import jakarta.persistence.*;
import java.net.InetAddress;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "refresh_tokens")
public class RefreshToken {
  @Id public UUID id;
  public UUID userId;

  @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.CHAR)
  @Column(columnDefinition = "char(64)", length = 64)
  public String tokenHash;

  public String deviceId;
  public String userAgent;
  public InetAddress ipAddress;
  public Instant issuedAt;
  public Instant expiresAt;
  public Instant revokedAt;
  public UUID replacedByTokenId;
  public Instant createdAt;
}

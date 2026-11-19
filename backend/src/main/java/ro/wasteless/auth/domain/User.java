package ro.wasteless.auth.domain;

import jakarta.persistence.*;
import java.time.Instant;
import ro.wasteless.common.domain.BaseEntity;

@Entity
@Table(name = "users")
public class User extends BaseEntity {
  @Column(columnDefinition = "citext", nullable = false)
  public String email;

  @Column(nullable = false, length = 100)
  @com.fasterxml.jackson.annotation.JsonIgnore
  public String passwordHash;

  @Column(length = 80)
  public String firstName;

  @Column(length = 80)
  public String lastName;

  @Column(length = 30)
  public String phone;

  public String avatarUrl;

  @Column(length = 10)
  public String preferredLocale = "en";

  @Enumerated(EnumType.STRING)
  @Column(length = 20)
  public UserStatus status = UserStatus.ACTIVE;

  public boolean emailVerified;
  public Instant emailVerifiedAt;
  public short failedLoginAttempts;
  public Instant lockedUntil;
  public Instant lastLoginAt;
  public Instant passwordChangedAt;
  public boolean locationSharingEnabled;
  public boolean marketingOptIn;
  public Instant termsAcceptedAt;
  public Instant deletedAt;

  public boolean canAuthenticate(Instant now) {
    return deletedAt == null
        && (status == UserStatus.ACTIVE
            || (status == UserStatus.LOCKED && lockedUntil != null && !lockedUntil.isAfter(now)));
  }
}

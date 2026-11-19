package ro.wasteless.auth.domain;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "user_roles")
@IdClass(UserRole.Key.class)
public class UserRole {
  @Id public UUID userId;
  @Id public UUID roleId;
  public Instant assignedAt;
  public UUID assignedBy;

  public record Key(UUID userId, UUID roleId) implements Serializable {}
}

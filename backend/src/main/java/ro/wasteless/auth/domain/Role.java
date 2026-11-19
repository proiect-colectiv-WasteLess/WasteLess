package ro.wasteless.auth.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "roles")
public class Role {
  @Id public UUID id;

  @Column(length = 40)
  public String code;

  @Column(length = 80)
  public String name;

  public String description;
  public boolean isSystem;
  public Instant createdAt;
  public Instant updatedAt;
}

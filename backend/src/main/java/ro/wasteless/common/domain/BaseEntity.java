package ro.wasteless.common.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {
  @jakarta.persistence.Id
  @GeneratedValue(strategy = GenerationType.UUID)
  protected UUID id;

  @CreatedDate
  @Column(nullable = false, updatable = false)
  protected Instant createdAt;

  @LastModifiedDate
  @Column(nullable = false)
  protected Instant updatedAt;

  @jakarta.persistence.Version protected long version;

  public UUID getId() {
    return id;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public long getVersion() {
    return version;
  }
}

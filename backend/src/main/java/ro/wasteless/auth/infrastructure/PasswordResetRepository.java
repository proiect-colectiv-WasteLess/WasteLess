package ro.wasteless.auth.infrastructure;

import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;
import ro.wasteless.auth.domain.PasswordResetToken;

public interface PasswordResetRepository extends JpaRepository<PasswordResetToken, UUID> {
  Optional<PasswordResetToken> findByTokenHash(String hash);

  List<PasswordResetToken> findByUserIdAndUsedAtIsNull(UUID id);

  long deleteByExpiresAtBefore(java.time.Instant now);
}

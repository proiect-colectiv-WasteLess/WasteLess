package ro.wasteless.auth.infrastructure;

import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;
import ro.wasteless.auth.domain.EmailVerificationToken;

public interface EmailVerificationRepository extends JpaRepository<EmailVerificationToken, UUID> {
  Optional<EmailVerificationToken> findByTokenHash(String hash);

  List<EmailVerificationToken> findByUserIdAndUsedAtIsNull(UUID id);

  long deleteByExpiresAtBefore(java.time.Instant now);
}

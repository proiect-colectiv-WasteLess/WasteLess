package ro.wasteless.auth.infrastructure;

import java.time.Instant;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import ro.wasteless.auth.domain.RefreshToken;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {
  Optional<RefreshToken> findByTokenHash(String hash);

  List<RefreshToken> findByUserIdAndRevokedAtIsNullAndExpiresAtAfter(UUID id, Instant now);

  List<RefreshToken> findByUserId(UUID id);

  @org.springframework.transaction.annotation.Transactional
  @Modifying
  @Query(
      value = "delete from refresh_tokens where expires_at < :now and replaced_by_token_id is null",
      nativeQuery = true)
  int deleteByExpiresAtBefore(@org.springframework.data.repository.query.Param("now") Instant now);
}

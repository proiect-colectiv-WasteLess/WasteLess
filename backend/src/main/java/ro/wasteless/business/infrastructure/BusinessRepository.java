package ro.wasteless.business.infrastructure;

import jakarta.persistence.LockModeType;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import ro.wasteless.business.domain.Business;

public interface BusinessRepository extends JpaRepository<Business, UUID> {
  Optional<Business> findByOwnerUserIdAndDeletedAtIsNull(UUID owner);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select b from Business b where b.ownerUserId = :owner and b.deletedAt is null")
  Optional<Business> lockByOwner(UUID owner);
}

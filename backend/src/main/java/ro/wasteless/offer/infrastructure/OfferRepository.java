package ro.wasteless.offer.infrastructure;

import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import ro.wasteless.offer.domain.Offer;

public interface OfferRepository
    extends JpaRepository<Offer, UUID>, JpaSpecificationExecutor<Offer> {
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select o from Offer o where o.id=:id")
  Optional<Offer> lockById(UUID id);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select o from Offer o where o.id=:id and o.businessId=:business")
  Optional<Offer> lockOwned(UUID id, UUID business);

  Optional<Offer> findByIdAndBusinessId(UUID id, UUID businessId);

  @Query(
      "select o from Offer o where o.status=ro.wasteless.offer.domain.OfferStatus.ACTIVE and o.deletedAt is null and o.quantityAvailable>0 and o.pickupEndAt>:now")
  Page<Offer> visible(Instant now, Pageable pageable);
}

package ro.wasteless.repositories;
import ro.wasteless.entities.Offer;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.domain.*;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.*;
public interface OfferRepository extends JpaRepository<Offer, UUID> {
    Page<Offer> findByQuantityGreaterThanAndPickupEndAfter(int quantity, Instant now, Pageable pageable);
    Page<Offer> findByBusinessId(UUID businessId, Pageable pageable);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from Offer o where o.id = :id")
    Optional<Offer> lockById(UUID id);
}
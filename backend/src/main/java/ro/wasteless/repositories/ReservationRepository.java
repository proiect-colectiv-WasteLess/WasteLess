package ro.wasteless.repositories;
import ro.wasteless.entities.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.*;
import java.util.UUID;
public interface ReservationRepository extends JpaRepository<Reservation, UUID> {
    Page<Reservation> findByClientId(UUID clientId, Pageable pageable);
    Page<Reservation> findByOfferBusinessId(UUID businessId, Pageable pageable);
}
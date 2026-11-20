package ro.wasteless.business.infrastructure;

import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;
import ro.wasteless.business.domain.BusinessLocation;

public interface BusinessLocationRepository extends JpaRepository<BusinessLocation, UUID> {
  List<BusinessLocation> findByBusinessIdOrderByCreatedAtAscIdAsc(UUID businessId);

  Optional<BusinessLocation> findByIdAndBusinessId(UUID id, UUID businessId);

  Optional<BusinessLocation> findByBusinessIdAndIsPrimaryTrue(UUID businessId);
}

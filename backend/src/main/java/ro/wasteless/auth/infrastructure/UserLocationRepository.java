package ro.wasteless.auth.infrastructure;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import ro.wasteless.auth.domain.UserLocation;

public interface UserLocationRepository extends JpaRepository<UserLocation, UUID> {}

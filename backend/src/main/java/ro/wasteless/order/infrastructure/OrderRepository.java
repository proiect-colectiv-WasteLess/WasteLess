package ro.wasteless.order.infrastructure;

import jakarta.persistence.LockModeType;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import ro.wasteless.order.domain.*;

public interface OrderRepository extends JpaRepository<FoodOrder, UUID> {
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select o from FoodOrder o where o.reference=:reference")
  Optional<FoodOrder> lockByReference(String reference);

  Optional<FoodOrder> findByReference(String reference);

  Page<FoodOrder> findByCustomerUserId(UUID user, Pageable pageable);

  Page<FoodOrder> findByCustomerUserIdAndStatus(UUID user, OrderStatus status, Pageable pageable);

  Page<FoodOrder> findByBusinessId(UUID business, Pageable pageable);

  Page<FoodOrder> findByBusinessIdAndStatus(UUID business, OrderStatus status, Pageable pageable);
}

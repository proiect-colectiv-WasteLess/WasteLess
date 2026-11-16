package ro.wasteless.payment.infrastructure;

import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;
import ro.wasteless.payment.domain.Payment;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {
  Optional<Payment> findByOrderId(UUID orderId);
}

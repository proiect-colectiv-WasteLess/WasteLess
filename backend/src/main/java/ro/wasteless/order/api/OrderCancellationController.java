package ro.wasteless.order.api;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import ro.wasteless.order.service.*;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderCancellationController {
  private final OrderCancellationService cancellation;
  private final IdempotentOrders idempotency;

  public OrderCancellationController(
      OrderCancellationService cancellation, IdempotentOrders idempotency) {
    this.cancellation = cancellation;
    this.idempotency = idempotency;
  }

  @PostMapping("/{id}/cancel")
  @PreAuthorize("hasRole('CLIENT')")
  public ResponseEntity<String> cancel(
      @PathVariable String id,
      @Valid @RequestBody OrderRequests.Cancel request,
      @RequestHeader("Idempotency-Key") String key) {
    return idempotency.execute(
        "orders:cancel:" + id, key, request, 200, () -> cancellation.cancel(id, request));
  }
}

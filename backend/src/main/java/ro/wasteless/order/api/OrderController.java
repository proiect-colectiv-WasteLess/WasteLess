package ro.wasteless.order.api;

import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import ro.wasteless.order.service.*;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {
  private final OrderPlacementService placement;
  private final IdempotentOrders idempotency;

  public OrderController(OrderPlacementService placement, IdempotentOrders idempotency) {
    this.placement = placement;
    this.idempotency = idempotency;
  }

  @PostMapping
  @PreAuthorize("hasRole('CLIENT')")
  public ResponseEntity<String> place(
      @Valid @RequestBody OrderRequests.Place request,
      @RequestHeader("Idempotency-Key") String key) {
    return idempotency.execute(
        "orders:create", key, request, 201, () -> placement.place(request, key));
  }
}

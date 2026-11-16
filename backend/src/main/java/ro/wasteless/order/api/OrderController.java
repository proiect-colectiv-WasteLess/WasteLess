package ro.wasteless.order.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.*;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ro.wasteless.order.service.OrderPlacementService;

@RestController
@RequestMapping("/api/v1/orders")
@Validated
public class OrderController {
  private final OrderPlacementService placement;

  public OrderController(OrderPlacementService placement) {
    this.placement = placement;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public OrderResponse place(
      @Valid @RequestBody OrderRequests.Place request,
      @RequestHeader("Idempotency-Key") @NotBlank @Size(max = 64) String key) {
    return placement.place(request, key);
  }
}

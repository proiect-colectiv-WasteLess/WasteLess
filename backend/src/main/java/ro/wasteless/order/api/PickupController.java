package ro.wasteless.order.api;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import ro.wasteless.order.service.PickupService;

@RestController
@RequestMapping("/api/v1/businesses/me/orders")
public class PickupController {
  private final PickupService pickup;

  public PickupController(PickupService pickup) {
    this.pickup = pickup;
  }

  @PostMapping({"/{id}/validate", "/{id}/validate-pickup"})
  public OrderResponse validate(
      @PathVariable String id, @Valid @RequestBody OrderRequests.Validate request) {
    return pickup.validate(id, request.code());
  }
}

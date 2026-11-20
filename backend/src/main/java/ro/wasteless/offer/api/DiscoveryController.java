package ro.wasteless.offer.api;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.UUID;
import org.springframework.web.bind.annotation.*;
import ro.wasteless.common.api.PageResponse;
import ro.wasteless.offer.infrastructure.DiscoveryService;

@RestController
@RequestMapping("/api/v1/offers")
public class DiscoveryController {
  private final DiscoveryService discovery;

  public DiscoveryController(DiscoveryService discovery) {
    this.discovery = discovery;
  }

  @GetMapping("/nearby")
  public PageResponse<DealResponse> nearby(
      @RequestParam(required = false) Double latitude,
      @RequestParam(required = false) Double longitude,
      @RequestParam(defaultValue = "1") double radiusKm,
      @RequestParam(required = false) String query,
      @RequestParam(required = false) String category,
      @RequestParam(required = false) BigDecimal minDiscount,
      @RequestParam(required = false) BigDecimal maxPrice,
      @RequestParam(required = false) LocalTime pickupBefore,
      @RequestParam(defaultValue = "false") boolean pickupToday,
      @RequestParam(defaultValue = "NEAREST") String sort,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    return discovery.nearby(
        new DiscoveryRequest(
            latitude,
            longitude,
            radiusKm,
            query,
            category,
            minDiscount,
            maxPrice,
            pickupBefore,
            pickupToday,
            sort,
            page,
            size));
  }

  @GetMapping("/{id}")
  public DealResponse detail(@PathVariable UUID id) {
    return discovery.detail(id);
  }
}

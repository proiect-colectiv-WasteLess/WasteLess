package ro.wasteless.order.api;

import org.springframework.web.bind.annotation.*;
import ro.wasteless.common.api.PageResponse;
import ro.wasteless.order.domain.OrderStatus;
import ro.wasteless.order.service.OrderQueries;

@RestController
@RequestMapping("/api/v1/orders")
public class CustomerOrderController {
  private final OrderQueries queries;

  public CustomerOrderController(OrderQueries queries) {
    this.queries = queries;
  }

  @GetMapping
  public PageResponse<OrderResponse> list(
      @RequestParam(required = false) OrderStatus status,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    return queries.customer(status, page, size);
  }

  @GetMapping("/{id}")
  public OrderResponse detail(@PathVariable String id) {
    return queries.detail(id);
  }
}

package ro.wasteless.order.api;

import org.springframework.web.bind.annotation.*;
import ro.wasteless.common.api.PageResponse;
import ro.wasteless.order.domain.OrderStatus;
import ro.wasteless.order.service.BusinessOrderQueries;

@RestController
@RequestMapping("/api/v1/businesses/me/orders")
public class BusinessOrderController {
  private final BusinessOrderQueries queries;

  public BusinessOrderController(BusinessOrderQueries queries) {
    this.queries = queries;
  }

  @GetMapping
  public PageResponse<BusinessOrderResponse> list(
      @RequestParam(required = false) OrderStatus status,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    return queries.list(status, page, size);
  }
}

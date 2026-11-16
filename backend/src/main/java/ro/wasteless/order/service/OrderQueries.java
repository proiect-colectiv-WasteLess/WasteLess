package ro.wasteless.order.service;

import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ro.wasteless.common.api.*;
import ro.wasteless.common.security.CurrentUser;
import ro.wasteless.order.api.OrderResponse;
import ro.wasteless.order.domain.*;
import ro.wasteless.order.infrastructure.OrderRepository;

@Service
@Transactional(readOnly = true)
public class OrderQueries {
  private final OrderRepository orders;
  private final CurrentUser current;
  private final OrderViews views;

  public OrderQueries(OrderRepository orders, CurrentUser current, OrderViews views) {
    this.orders = orders;
    this.current = current;
    this.views = views;
  }

  public static Pageable page(int page, int size) {
    if (page < 0 || size < 1 || size > 100)
      throw new DomainException(
          ErrorCode.VALIDATION_FAILED,
          HttpStatus.BAD_REQUEST,
          "Page must be nonnegative; size must be 1 to 100");
    return PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by("id")));
  }

  @PreAuthorize("hasRole('CLIENT')")
  public PageResponse<OrderResponse> customer(OrderStatus status, int page, int size) {
    var user = current.id();
    var paging = page(page, size);
    var result =
        status == null
            ? orders.findByCustomerUserId(user, paging)
            : orders.findByCustomerUserIdAndStatus(user, status, paging);
    return PageResponse.from(result.map(views::view));
  }

  @PreAuthorize("hasRole('CLIENT')")
  public OrderResponse detail(String reference) {
    var user = current.id();
    var order =
        orders
            .findByReference(reference)
            .filter(o -> o.customerUserId.equals(user))
            .orElseThrow(
                () ->
                    new DomainException(
                        ErrorCode.NOT_FOUND, HttpStatus.NOT_FOUND, "Order not found"));
    return views.view(order);
  }
}

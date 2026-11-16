package ro.wasteless.order.service;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ro.wasteless.common.api.PageResponse;
import ro.wasteless.order.api.BusinessOrderResponse;
import ro.wasteless.order.domain.OrderStatus;
import ro.wasteless.order.infrastructure.*;

@Service
@Transactional(readOnly = true)
public class BusinessOrderQueries {
  private final OrderRepository orders;
  private final BusinessOwnerLookup business;
  private final OrderViews views;

  public BusinessOrderQueries(
      OrderRepository orders, BusinessOwnerLookup business, OrderViews views) {
    this.orders = orders;
    this.business = business;
    this.views = views;
  }

  @PreAuthorize("hasRole('BUSINESS')")
  public PageResponse<BusinessOrderResponse> list(OrderStatus status, int page, int size) {
    var id = business.currentBusiness();
    var paging = OrderQueries.page(page, size);
    var result =
        status == null
            ? orders.findByBusinessId(id, paging)
            : orders.findByBusinessIdAndStatus(id, status, paging);
    return PageResponse.from(
        result.map(
            o -> BusinessOrderResponse.of(views.view(o), business.customerName(o.customerUserId))));
  }
}

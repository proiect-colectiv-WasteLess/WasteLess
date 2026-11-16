package ro.wasteless.order.service;

import java.time.Clock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ro.wasteless.common.api.*;
import ro.wasteless.common.port.OfferStockPort;
import ro.wasteless.common.security.CurrentUser;
import ro.wasteless.order.api.OrderRequests;
import ro.wasteless.order.domain.*;
import ro.wasteless.order.infrastructure.OrderRepository;
import ro.wasteless.payment.service.RefundProcessor;

@Service
public class OrderCancellationService {
  private final OrderRepository orders;
  private final CurrentUser current;
  private final OrderViews views;
  private final RefundProcessor refunds;
  private final OfferStockPort stock;
  private final Clock clock;
  private final long grace;

  public OrderCancellationService(
      OrderRepository orders,
      CurrentUser current,
      OrderViews views,
      RefundProcessor refunds,
      OfferStockPort stock,
      Clock clock,
      @Value("${app.orders.cancel-grace-seconds:0}") long grace) {
    if (grace < 0) throw new IllegalArgumentException("Cancel grace must not be negative");
    this.orders = orders;
    this.current = current;
    this.views = views;
    this.refunds = refunds;
    this.stock = stock;
    this.clock = clock;
    this.grace = grace;
  }

  @Transactional
  @PreAuthorize("hasRole('CLIENT')")
  public ResponseEntity<?> cancel(String reference, OrderRequests.Cancel request) {
    var actor = current.id();
    var order =
        orders
            .lockByReference(reference)
            .filter(o -> o.customerUserId.equals(actor))
            .orElseThrow(
                () ->
                    new DomainException(
                        ErrorCode.NOT_FOUND, HttpStatus.NOT_FOUND, "Order not found"));
    if (order.status == OrderStatus.CANCELED) return ResponseEntity.ok(views.view(order));
    if (order.status != OrderStatus.ACTIVE
        || !clock.instant().isBefore(order.pickupStartAt.plusSeconds(grace)))
      throw new DomainException(
          ErrorCode.ORDER_NOT_CANCELABLE, HttpStatus.CONFLICT, "Order cannot be canceled now");
    if (!refunds.refund(order.id, request.reason()))
      return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
          .body(
              ProblemResponses.of(
                  HttpStatus.BAD_GATEWAY,
                  ErrorCode.REFUND_FAILED,
                  "Refund failed; order remains active. Retry with a new request key."));
    order.cancel(request.reason(), clock.instant());
    stock.release(order.offerId, order.quantity);
    orders.saveAndFlush(order);
    return ResponseEntity.ok(views.view(order));
  }
}

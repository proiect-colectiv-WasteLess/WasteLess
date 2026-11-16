package ro.wasteless.order.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ro.wasteless.common.api.*;
import ro.wasteless.common.security.CurrentUser;
import ro.wasteless.order.api.*;
import ro.wasteless.order.domain.*;
import ro.wasteless.order.infrastructure.*;

@Service
public class PickupService {
  private final OrderRepository orders;
  private final BusinessOwnerLookup business;
  private final CurrentUser current;
  private final OrderViews views;
  private final PickupAttempts attempts;
  private final Clock clock;
  private final long grace;

  public PickupService(
      OrderRepository orders,
      BusinessOwnerLookup business,
      CurrentUser current,
      OrderViews views,
      PickupAttempts attempts,
      Clock clock,
      @Value("${app.orders.pickup-grace-seconds:900}") long grace) {
    if (grace < 0) throw new IllegalArgumentException("Pickup grace must not be negative");
    this.orders = orders;
    this.business = business;
    this.current = current;
    this.views = views;
    this.attempts = attempts;
    this.clock = clock;
    this.grace = grace;
  }

  @Transactional
  @PreAuthorize("hasRole('BUSINESS')")
  public OrderResponse validate(String reference, String code) {
    var owner = business.currentBusiness();
    var order =
        orders
            .lockByReference(reference)
            .filter(o -> o.businessId.equals(owner))
            .orElseThrow(
                () ->
                    new DomainException(
                        ErrorCode.NOT_FOUND, HttpStatus.NOT_FOUND, "Order not found"));
    attempts.check(order.id);
    if (!MessageDigest.isEqual(
        order.pickupCode.getBytes(StandardCharsets.US_ASCII),
        code.getBytes(StandardCharsets.US_ASCII))) {
      attempts.failed(order.id);
      throw new DomainException(
          ErrorCode.INVALID_PICKUP_CODE, HttpStatus.CONFLICT, "Invalid pickup code");
    }
    if (order.status == OrderStatus.COMPLETED) return views.view(order);
    if (order.status != OrderStatus.ACTIVE)
      throw new DomainException(ErrorCode.CONFLICT, HttpStatus.CONFLICT, "Order is not active");
    if (clock.instant().isBefore(order.pickupStartAt)
        || clock.instant().isAfter(order.pickupEndAt.plusSeconds(grace)))
      throw new DomainException(
          ErrorCode.CONFLICT, HttpStatus.CONFLICT, "Outside the pickup window");
    order.complete(current.id(), clock.instant());
    orders.saveAndFlush(order);
    attempts.clear(order.id);
    return views.view(order);
  }
}

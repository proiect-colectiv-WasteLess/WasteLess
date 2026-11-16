package ro.wasteless.order.service;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ro.wasteless.common.api.*;
import ro.wasteless.common.port.*;
import ro.wasteless.common.security.CurrentUser;
import ro.wasteless.order.api.*;
import ro.wasteless.order.domain.*;
import ro.wasteless.order.infrastructure.*;
import ro.wasteless.payment.domain.Payment;
import ro.wasteless.payment.infrastructure.PaymentRepository;

@Service
public class OrderPlacementService {
  private final OrderRepository orders;
  private final PaymentRepository payments;
  private final OfferSnapshotReader offers;
  private final OfferStockPort stock;
  private final PaymentGateway gateway;
  private final OrderIdentifiers ids;
  private final CurrentUser current;
  private final Clock clock;
  private final OrderAudit audit;

  public OrderPlacementService(
      OrderRepository orders,
      PaymentRepository payments,
      OfferSnapshotReader offers,
      OfferStockPort stock,
      PaymentGateway gateway,
      OrderIdentifiers ids,
      CurrentUser current,
      Clock clock,
      OrderAudit audit) {
    this.orders = orders;
    this.payments = payments;
    this.offers = offers;
    this.stock = stock;
    this.gateway = gateway;
    this.ids = ids;
    this.current = current;
    this.clock = clock;
    this.audit = audit;
  }

  @Transactional
  @PreAuthorize("hasRole('CLIENT')")
  public OrderResponse place(OrderRequests.Place request, String key) {
    UUID user = current.id();
    var initial = offers.read(request.offerId(), false);
    String code = ids.pickupCode(initial.businessId());
    var snapshot = offers.read(request.offerId(), true);
    if (!snapshot.currency().equals("EUR"))
      throw new DomainException(
          ErrorCode.VALIDATION_FAILED, HttpStatus.BAD_REQUEST, "Only EUR orders are supported");
    BigDecimal total = snapshot.price().multiply(BigDecimal.valueOf(request.quantity()));
    if (total.compareTo(new BigDecimal("99999999.99")) > 0)
      throw new DomainException(
          ErrorCode.VALIDATION_FAILED,
          HttpStatus.BAD_REQUEST,
          "Order total exceeds supported limit");
    stock.reserve(request.offerId(), request.quantity());
    FoodOrder o = new FoodOrder();
    o.reference = ids.nextReference();
    o.customerUserId = user;
    o.businessId = snapshot.businessId();
    o.businessLocationId = snapshot.locationId();
    o.offerId = snapshot.id();
    o.currency = snapshot.currency();
    o.quantity = request.quantity();
    o.unitPrice = snapshot.price();
    o.originalUnitPrice = snapshot.originalPrice();
    o.totalPrice = total;
    o.itemName = snapshot.itemName();
    o.businessName = snapshot.businessName();
    o.imageUrl = snapshot.imageUrl();
    o.pickupStartAt = snapshot.pickupStart();
    o.pickupEndAt = snapshot.pickupEnd();
    o.pickupCode = code;
    o.createdAt = clock.instant();
    o.updatedAt = o.createdAt;
    orders.saveAndFlush(o);
    String providerKey = "order:" + o.id;
    var authorization =
        gateway.authorize(o.id, total, o.currency, request.paymentMethod().name(), providerKey);
    if (!authorization.status().equals("AUTHORIZED")) throw paymentFailure(authorization);
    var result = gateway.capture(o.id, authorization.reference(), total, o.currency, providerKey);
    if (!result.status().equals("CAPTURED")) throw paymentFailure(result);
    Payment p = new Payment(total, o.currency);
    p.orderId = o.id;
    p.userId = user;
    p.method = request.paymentMethod();
    p.status = Payment.Status.CAPTURED;
    p.provider = "configured";
    p.providerReference = result.reference();
    p.idempotencyKey = providerKey;
    p.capturedAt = clock.instant();
    p.createdAt = p.capturedAt;
    p.updatedAt = p.capturedAt;
    payments.saveAndFlush(p);
    audit.append(o.id, null, "ACTIVE", user, "Order placed");
    return OrderResponse.of(o, clock);
  }

  private DomainException paymentFailure(PaymentGateway.Result result) {
    return new DomainException(
        ErrorCode.CONFLICT,
        HttpStatus.PAYMENT_REQUIRED,
        result.status().equals("TIMEOUT") ? "Payment timed out" : "Payment was declined");
  }
}

package ro.wasteless.order.api;

import java.math.BigDecimal;
import java.time.Instant;
import ro.wasteless.order.domain.OrderStatus;

public record BusinessOrderResponse(
    String id,
    int quantity,
    String itemName,
    String shop,
    String imageAsset,
    String pickupWindow,
    String code,
    BigDecimal price,
    String timeAgo,
    boolean isCompleted,
    OrderStatus status,
    Instant createdAt,
    Instant canceledAt,
    OrderResponse.RefundView refund,
    String customerName) {
  public static BusinessOrderResponse of(OrderResponse o, String customer) {
    return new BusinessOrderResponse(
        o.id(),
        o.quantity(),
        o.itemName(),
        o.shop(),
        o.imageAsset(),
        o.pickupWindow(),
        o.code(),
        o.price(),
        o.timeAgo(),
        o.isCompleted(),
        o.status(),
        o.createdAt(),
        o.canceledAt(),
        o.refund(),
        customer);
  }
}

package ro.wasteless.order.api;

import java.math.BigDecimal;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import ro.wasteless.order.domain.*;

public record OrderResponse(
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
    Instant canceledAt) {
  public static OrderResponse of(FoodOrder o, Clock clock) {
    ZoneId zone = ZoneId.of("Europe/Bucharest");
    var start = o.pickupStartAt.atZone(zone);
    var end = o.pickupEndAt.atZone(zone);
    var today = clock.instant().atZone(zone).toLocalDate();
    String prefix = start.toLocalDate().equals(today) ? "Today" : start.toLocalDate().toString();
    var fmt = DateTimeFormatter.ofPattern("HH:mm", Locale.ROOT);
    String window =
        prefix
            + ", "
            + fmt.format(start)
            + "-"
            + (start.toLocalDate().equals(end.toLocalDate()) ? "" : end.toLocalDate() + " ")
            + fmt.format(end);
    long minutes = Math.max(0, Duration.between(o.createdAt, clock.instant()).toMinutes());
    String ago =
        minutes == 0
            ? "Just now"
            : minutes < 60
                ? minutes + " min ago"
                : minutes < 1440 ? (minutes / 60) + " h ago" : (minutes / 1440) + " d ago";
    return new OrderResponse(
        o.reference,
        o.quantity,
        o.itemName,
        o.businessName,
        o.imageUrl,
        window,
        o.pickupCode,
        o.totalPrice,
        ago,
        o.status == OrderStatus.COMPLETED,
        o.status,
        o.createdAt,
        o.canceledAt);
  }
}

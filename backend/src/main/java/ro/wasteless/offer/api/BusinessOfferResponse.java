package ro.wasteless.offer.api;

import java.math.BigDecimal;
import java.util.UUID;
import ro.wasteless.offer.domain.*;

public record BusinessOfferResponse(
    UUID id,
    String title,
    String imageAsset,
    int available,
    BigDecimal price,
    BigDecimal originalPrice,
    String stockLevel,
    String category,
    String description,
    String pickupWindow,
    boolean isPaused,
    OfferStatus status) {
  public static BusinessOfferResponse from(Offer o, String category) {
    return new BusinessOfferResponse(
        o.getId(),
        o.title,
        o.imageUrl,
        o.quantityAvailable,
        o.price,
        o.originalPrice,
        o.stockLevel(),
        category,
        o.description == null ? "" : o.description,
        o.pickupWindow(),
        o.getStatus() == OfferStatus.PAUSED,
        o.getStatus());
  }
}

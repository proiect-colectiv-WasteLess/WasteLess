package ro.wasteless.offer.api;

import java.math.BigDecimal;
import java.util.UUID;

/** Immutable fetch-time snapshot; never exposes persistence entities or coordinates. */
public record DealResponse(
    UUID id,
    String name,
    String shop,
    BigDecimal oldPrice,
    BigDecimal price,
    BigDecimal distanceKm,
    String pickupStart,
    String pickupEnd,
    int left,
    BigDecimal rating,
    String imageAsset,
    String category,
    String description) {}

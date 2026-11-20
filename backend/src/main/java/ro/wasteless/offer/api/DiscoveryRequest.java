package ro.wasteless.offer.api;

import java.math.BigDecimal;
import java.time.LocalTime;

public record DiscoveryRequest(
    Double latitude,
    Double longitude,
    double radiusKm,
    String query,
    String category,
    BigDecimal minDiscount,
    BigDecimal maxPrice,
    LocalTime pickupBefore,
    boolean pickupToday,
    String sort,
    int page,
    int size) {}

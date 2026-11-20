package ro.wasteless.offer.api;

import jakarta.validation.Validator;
import java.net.URI;
import java.time.*;
import java.util.UUID;
import org.springframework.stereotype.Service;
import ro.wasteless.business.api.CatalogValidationException;
import ro.wasteless.business.infrastructure.BusinessLocationRepository;
import ro.wasteless.offer.domain.*;
import ro.wasteless.offer.infrastructure.OfferCategoryRepository;

@Service
public class OfferRules {
  private final Validator validator;
  private final OfferCategoryRepository categories;
  private final BusinessLocationRepository locations;
  private final Clock clock;

  public OfferRules(
      Validator validator,
      OfferCategoryRepository categories,
      BusinessLocationRepository locations,
      Clock clock) {
    this.validator = validator;
    this.categories = categories;
    this.locations = locations;
    this.clock = clock;
  }

  public void apply(Offer o, UUID businessId, OfferInput r) {
    var errors = validator.validate(r);
    if (!errors.isEmpty()) {
      var e = errors.iterator().next();
      fail(e.getPropertyPath().toString(), e.getMessage());
    }
    validateValues(r, clock.instant());
    var category =
        categories.findByIsActiveTrueOrderBySortOrderAscSlugAsc().stream()
            .filter(
                c ->
                    c.getSlug().equals(r.category())
                        || OfferCategory.frontendName(c.getSlug()).equals(r.category())
                        || c.getId().toString().equals(r.category())
                        || c.getDisplayName().equalsIgnoreCase(r.category()))
            .findFirst()
            .orElseThrow(() -> new CatalogValidationException("category", "Unknown category"));
    var location =
        r.locationId() == null
            ? locations.findByBusinessIdAndIsPrimaryTrue(businessId)
            : locations.findByIdAndBusinessId(r.locationId(), businessId);
    var l =
        location.orElseThrow(
            () ->
                new CatalogValidationException(
                    "locationId", "A pickup location owned by this business is required"));
    if (l.addressLine1.isBlank() || l.city.isBlank() || "ZZ".equals(l.countryCode))
      fail("locationId", "Complete the pickup address before publishing");
    o.businessId = businessId;
    o.locationId = l.id;
    o.categoryId = category.getId();
    o.title = r.title().trim();
    o.description = r.description() == null ? "" : r.description().trim();
    o.price = r.price();
    o.originalPrice = r.originalPrice();
    o.currency = "EUR";
    o.changeQuantity(r.quantity());
    o.pickupStartAt = r.pickupStartAt();
    o.pickupEndAt = r.pickupEndAt();
    o.expiresAt = r.pickupEndAt();
    o.imageUrl = r.imageAsset().trim();
    if (o.publishedAt == null) o.publishedAt = clock.instant();
    o.reconcile(clock.instant());
  }

  public static void validateValues(OfferInput r, Instant now) {
    if (r.title() == null || r.title().trim().length() < 3 || r.title().trim().length() > 120)
      fail("title", "Must contain 3 to 120 characters");
    if (r.description() != null && r.description().length() > 2000)
      fail("description", "Must contain at most 2000 characters");
    if (r.price() == null || r.price().signum() <= 0 || r.price().scale() > 2)
      fail("price", "Must be positive with at most two decimal places");
    if (r.originalPrice() == null || r.originalPrice().compareTo(r.price()) < 0)
      fail("originalPrice", "Must be at least price");
    if (r.quantity() == null || r.quantity() < 1 || r.quantity() > 10000)
      fail("quantity", "Must be between 1 and 10000");
    if (r.pickupStartAt() == null || !r.pickupStartAt().isAfter(now))
      fail("pickupStartAt", "Must be in the future");
    if (r.pickupEndAt() == null
        || !r.pickupEndAt().isAfter(r.pickupStartAt())
        || Duration.between(r.pickupStartAt(), r.pickupEndAt()).compareTo(Duration.ofHours(24)) > 0)
      fail("pickupEndAt", "Must be after pickup start, with a window of at most 24 hours");
    if (r.currency() != null && !"EUR".equals(r.currency()))
      fail("currency", "Only EUR is supported");
    try {
      var uri = URI.create(r.imageAsset());
      if (!("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
          || uri.getHost() == null
          || uri.getUserInfo() != null) fail("imageAsset", "Must be an absolute HTTP(S) image URL");
    } catch (IllegalArgumentException | NullPointerException e) {
      fail("imageAsset", "Must be an absolute HTTP(S) image URL");
    }
  }

  private static void fail(String field, String message) {
    throw new CatalogValidationException(field, message);
  }
}

package ro.wasteless.offer.api;

import java.time.Clock;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ro.wasteless.business.api.*;
import ro.wasteless.common.api.*;
import ro.wasteless.offer.domain.*;
import ro.wasteless.offer.infrastructure.*;
import tools.jackson.databind.json.JsonMapper;

@Service
@Transactional
public class BusinessOfferService {
  private final BusinessAccess access;
  private final OfferRepository offers;
  private final OfferCategoryRepository categories;
  private final OfferRules rules;
  private final JsonMapper mapper;
  private final Clock clock;

  public BusinessOfferService(
      BusinessAccess access,
      OfferRepository offers,
      OfferCategoryRepository categories,
      OfferRules rules,
      JsonMapper mapper,
      Clock clock) {
    this.access = access;
    this.offers = offers;
    this.categories = categories;
    this.rules = rules;
    this.mapper = mapper;
    this.clock = clock;
  }

  @Transactional(readOnly = true)
  public PageResponse<BusinessOfferResponse> list(String status, int page, int size) {
    if (page < 0 || size < 1 || size > 50)
      throw new CatalogValidationException("page", "page must be >= 0 and size between 1 and 50");
    var business = access.current().getId();
    OfferStatus requested;
    try {
      requested = "ALL".equals(status) ? null : OfferStatus.valueOf(status);
    } catch (IllegalArgumentException e) {
      throw new CatalogValidationException("status", "Unknown offer status");
    }
    Specification<Offer> spec =
        (root, q, cb) -> {
          var owner = cb.equal(root.get("businessId"), business);
          if (requested == null) return owner;
          if (requested == OfferStatus.DELETED)
            return cb.and(owner, cb.isNotNull(root.get("deletedAt")));
          return cb.and(
              owner, cb.isNull(root.get("deletedAt")), cb.equal(root.get("status"), requested));
        };
    var names =
        categories.findAll().stream()
            .collect(Collectors.toMap(OfferCategory::getId, OfferCategory::getDisplayName));
    return PageResponse.from(
        offers
            .findAll(
                spec,
                PageRequest.of(
                    page, size, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"))))
            .map(o -> BusinessOfferResponse.from(o, names.getOrDefault(o.categoryId, "Other"))));
  }

  @Transactional(readOnly = true)
  public BusinessOfferResponse get(UUID id) {
    return response(
        offers
            .findByIdAndBusinessId(id, access.current().getId())
            .orElseThrow(BusinessAccess::notFound));
  }

  public BusinessOfferResponse create(OfferInput input) {
    var business = access.lockCurrent();
    var o = new Offer();
    rules.apply(o, business.getId(), input);
    return response(offers.saveAndFlush(o));
  }

  public BusinessOfferResponse replace(UUID id, OfferInput input) {
    var o = owned(id);
    editable(o);
    rules.apply(o, o.businessId, input);
    offers.flush();
    return response(o);
  }

  public BusinessOfferResponse patch(UUID id, Map<String, Object> patch) {
    var o = owned(id);
    editable(o);
    var values = new LinkedHashMap<String, Object>();
    values.put("title", o.title);
    values.put("category", o.categoryId.toString());
    values.put("description", o.description);
    values.put("price", o.price);
    values.put("originalPrice", o.originalPrice);
    values.put("quantity", o.quantityTotal);
    values.put("pickupStartAt", o.pickupStartAt);
    values.put("pickupEndAt", o.pickupEndAt);
    values.put("imageAsset", o.imageUrl);
    values.put("locationId", o.locationId);
    values.put("currency", o.currency);
    for (var key : patch.keySet())
      if (!values.containsKey(key) && !"imageUrl".equals(key))
        throw new CatalogValidationException(key, "Unknown offer field");
    values.putAll(patch);
    if (patch.containsKey("imageUrl")) {
      values.put("imageAsset", patch.get("imageUrl"));
      values.remove("imageUrl");
    }
    OfferInput input;
    try {
      input = mapper.convertValue(values, OfferInput.class);
    } catch (IllegalArgumentException e) {
      throw new CatalogValidationException("offer", "Invalid offer value");
    }
    rules.apply(o, o.businessId, input);
    offers.flush();
    return response(o);
  }

  public void delete(UUID id) {
    owned(id).delete(clock.instant());
  }

  public BusinessOfferResponse pause(UUID id) {
    var o = owned(id);
    o.pause(clock.instant());
    return response(o);
  }

  public BusinessOfferResponse resume(UUID id) {
    var o = owned(id);
    o.resume(clock.instant());
    return response(o);
  }

  private Offer owned(UUID id) {
    var business = access.lockCurrent().getId();
    return offers.lockOwned(id, business).orElseThrow(BusinessAccess::notFound);
  }

  private void editable(Offer o) {
    if (o.getStatus() == OfferStatus.DELETED
        || o.getStatus() == OfferStatus.EXPIRED
        || !o.pickupEndAt.isAfter(clock.instant()))
      throw new DomainException(
          ErrorCode.CONFLICT, HttpStatus.CONFLICT, "Offer cannot be edited in its current state");
  }

  private BusinessOfferResponse response(Offer o) {
    var category = categories.findById(o.categoryId).orElseThrow(BusinessAccess::notFound);
    return BusinessOfferResponse.from(o, category.getDisplayName());
  }
}

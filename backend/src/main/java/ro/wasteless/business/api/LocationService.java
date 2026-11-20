package ro.wasteless.business.api;

import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ro.wasteless.business.domain.BusinessLocation;
import ro.wasteless.business.infrastructure.BusinessLocationRepository;
import ro.wasteless.common.api.*;

@Service
@Transactional
public class LocationService {
  public void lockCurrent() {
    access.lockCurrent();
  }

  private final BusinessAccess access;
  private final BusinessLocationRepository locations;
  private final JdbcTemplate jdbc;

  public LocationService(
      BusinessAccess access, BusinessLocationRepository locations, JdbcTemplate jdbc) {
    this.access = access;
    this.locations = locations;
    this.jdbc = jdbc;
  }

  public List<BusinessLocation> list() {
    return locations.findByBusinessIdOrderByCreatedAtAscIdAsc(access.current().getId());
  }

  public BusinessLocation save(UUID id, LocationRequest r, boolean primaryUpsert) {
    var business = access.lockCurrent(); // Serializes all primary-location changes for one owner.
    var all = locations.findByBusinessIdOrderByCreatedAtAscIdAsc(business.getId());
    BusinessLocation l;
    if (primaryUpsert)
      l = all.stream().filter(x -> x.isPrimary).findFirst().orElseGet(BusinessLocation::new);
    else
      l =
          id == null
              ? new BusinessLocation()
              : locations
                  .findByIdAndBusinessId(id, business.getId())
                  .orElseThrow(BusinessAccess::notFound);
    boolean primary =
        primaryUpsert
            || all.isEmpty()
            || Boolean.TRUE.equals(r.isPrimary())
            || (l.isPrimary && r.isPrimary() == null);
    if (Boolean.FALSE.equals(r.isPrimary()) && l.isPrimary && !primaryUpsert)
      throw new CatalogValidationException(
          "isPrimary", "Promote another location before demoting the primary");
    if (primary) {
      for (var old : all) if (old.isPrimary && !Objects.equals(old.id, l.id)) old.isPrimary = false;
      locations.flush(); // Clear the partial unique index before promoting.
    }
    l.businessId = business.getId();
    l.label = trim(r.label());
    l.addressLine1 = r.addressLine1().trim();
    l.addressLine2 = trim(r.addressLine2());
    l.city = r.city().trim();
    l.postalCode = trim(r.postalCode());
    l.countryCode = r.countryCode().toUpperCase(Locale.ROOT);
    if (!Set.of(Locale.getISOCountries()).contains(l.countryCode))
      throw new CatalogValidationException("countryCode", "Must be an ISO 3166-1 country code");
    l.latitude = r.latitude();
    l.longitude = r.longitude();
    l.isPrimary = primary;
    // geography is a generated column in P11; updating coordinates recomputes it atomically.
    return locations.saveAndFlush(l);
  }

  public void delete(UUID id) {
    var business = access.lockCurrent();
    var l =
        locations.findByIdAndBusinessId(id, business.getId()).orElseThrow(BusinessAccess::notFound);
    // Historical offers and orders retain their pickup location.
    var references =
        jdbc.queryForObject(
            "select (select count(*) from offers where location_id=?) + (select count(*) from orders where business_location_id=?)",
            Long.class,
            id,
            id);
    if (references != null && references > 0)
      throw new DomainException(
          ErrorCode.CONFLICT, HttpStatus.CONFLICT, "Location is referenced by offers or orders");
    locations.delete(l);
    locations.flush();
    if (l.isPrimary)
      locations.findByBusinessIdOrderByCreatedAtAscIdAsc(business.getId()).stream()
          .findFirst()
          .ifPresent(x -> x.isPrimary = true);
    locations.flush();
  }

  private static String trim(String s) {
    return s == null ? null : s.trim();
  }
}

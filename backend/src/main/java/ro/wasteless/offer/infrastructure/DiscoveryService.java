package ro.wasteless.offer.infrastructure;

import java.math.*;
import java.sql.*;
import java.time.*;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.namedparam.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ro.wasteless.business.api.*;
import ro.wasteless.common.api.*;
import ro.wasteless.common.security.CurrentUser;
import ro.wasteless.offer.api.*;
import ro.wasteless.offer.domain.*;

@Service
@Transactional(readOnly = true)
public class DiscoveryService {
  private static final String FROM =
      " from offers o join businesses b on b.id=o.business_id join business_locations l on l.id=o.location_id and l.business_id=o.business_id join offer_categories c on c.id=o.category_id ";
  private static final String VISIBLE =
      "o.status='ACTIVE' and o.deleted_at is null and o.quantity_available>0 and o.pickup_end_at>:now and b.deleted_at is null and b.status='ACTIVE' and c.is_active and c.slug in ('bakery','meals','groceries','desserts','fruit-and-veg','drinks')";
  private static final String POINT = "ST_SetSRID(ST_MakePoint(:lng,:lat),4326)::geography";
  private final NamedParameterJdbcTemplate jdbc;
  private final CurrentUser current;
  private final Clock clock;
  private final DealMapper dealMapper;

  public DiscoveryService(
      NamedParameterJdbcTemplate jdbc, CurrentUser current, Clock clock, DealMapper dealMapper) {
    this.jdbc = jdbc;
    this.current = current;
    this.clock = clock;
    this.dealMapper = dealMapper;
  }

  public PageResponse<DealResponse> nearby(DiscoveryRequest r) {
    validate(r);
    var coordinates = coordinates(r.latitude(), r.longitude());
    var p =
        new MapSqlParameterSource()
            .addValue("lat", coordinates[0])
            .addValue("lng", coordinates[1])
            .addValue("now", Timestamp.from(clock.instant()))
            .addValue("radius", r.radiusKm() * 1000);
    var where = new StringBuilder(VISIBLE + " and ST_DWithin(l.location," + POINT + ",:radius)");
    if (r.query() != null && !r.query().isBlank()) {
      where.append(
          " and (o.title ilike :query or o.description ilike :query or b.name ilike :query)");
      p.addValue(
          "query",
          "%"
              + r.query().trim().replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")
              + "%");
    }
    if (r.category() != null && !r.category().isBlank()) {
      where.append(" and c.slug=:category");
      p.addValue("category", "fruitAndVeg".equals(r.category()) ? "fruit-and-veg" : r.category());
    }
    if (r.minDiscount() != null) {
      where.append(" and (1-o.price/o.original_price)*100>=:discount");
      p.addValue("discount", r.minDiscount());
    }
    if (r.maxPrice() != null) {
      where.append(" and o.price<=:maxPrice");
      p.addValue("maxPrice", r.maxPrice());
    }
    if (r.pickupBefore() != null) {
      where.append(
          " and (o.pickup_start_at at time zone 'Europe/Bucharest')::time<=cast(:before as time)");
      p.addValue("before", r.pickupBefore().toString());
    }
    if (r.pickupToday()) {
      where.append(
          " and (o.pickup_start_at at time zone 'Europe/Bucharest')::date=cast(:today as date)");
      p.addValue("today", LocalDate.now(clock.withZone(ZoneId.of("Europe/Bucharest"))).toString());
    }
    var order =
        switch (r.sort()) {
          case "NEAREST" -> "distance_m asc";
          case "BIGGEST_DISCOUNT" -> "(1-o.price/o.original_price) desc";
          case "LOWEST_PRICE" -> "o.price asc";
          case "ENDING_SOON" -> "o.pickup_end_at asc";
          default -> throw new CatalogValidationException("sort", "Unknown sort order");
        };
    long count = jdbc.queryForObject("select count(*)" + FROM + "where " + where, p, Long.class);
    p.addValue("limit", r.size()).addValue("offset", (long) r.page() * r.size());
    var rows =
        jdbc.query(
            "select o.*,b.name as business_name,b.rating_avg,c.slug,ST_Distance(l.location,"
                + POINT
                + ") as distance_m"
                + FROM
                + "where "
                + where
                + " order by "
                + order
                + ",o.id asc limit :limit offset :offset",
            p,
            dealMapper);
    return new PageResponse<>(
        rows, r.page(), r.size(), count, (int) ((count + r.size() - 1) / r.size()));
  }

  public DealResponse detail(UUID id) {
    var p =
        new MapSqlParameterSource()
            .addValue("id", id)
            .addValue("now", Timestamp.from(clock.instant()));
    return jdbc
        .query(
            "select o.*,b.name as business_name,b.rating_avg,c.slug,0 as distance_m"
                + FROM
                + "where "
                + VISIBLE
                + " and o.id=:id",
            p,
            dealMapper)
        .stream()
        .findFirst()
        .orElseThrow(BusinessAccess::notFound);
  }

  private double[] coordinates(Double lat, Double lng) {
    if ((lat == null) != (lng == null))
      throw new CatalogValidationException("location", "Provide both latitude and longitude");
    if (lat == null) {
      var auth = SecurityContextHolder.getContext().getAuthentication();
      if (auth != null
          && auth.getAuthorities().stream().anyMatch(a -> "ROLE_CLIENT".equals(a.getAuthority()))) {
        var locations =
            jdbc.query(
                "select l.latitude,l.longitude from user_locations l join users u on u.id=l.user_id where l.user_id=:id and u.location_sharing_enabled=true and u.deleted_at is null and u.status='ACTIVE'",
                Map.of("id", current.id()),
                (rs, row) -> new double[] {rs.getDouble(1), rs.getDouble(2)});
        if (!locations.isEmpty()) return locations.getFirst();
      }
      throw new DomainException(
          ErrorCode.MISSING_LOCATION,
          HttpStatus.BAD_REQUEST,
          "Provide latitude and longitude or save a consented location");
    }
    if (!Double.isFinite(lat) || !Double.isFinite(lng) || Math.abs(lat) > 90 || Math.abs(lng) > 180)
      throw new CatalogValidationException("location", "Invalid coordinates");
    return new double[] {lat, lng};
  }

  public static void validate(DiscoveryRequest r) {
    if (!Double.isFinite(r.radiusKm()) || r.radiusKm() <= 0 || r.radiusKm() > 50)
      throw new CatalogValidationException("radiusKm", "Must be greater than 0 and at most 50");
    if (r.page() < 0 || r.size() < 1 || r.size() > 50)
      throw new CatalogValidationException("page", "page must be >=0 and size between 1 and 50");
    if (r.query() != null && r.query().length() > 200)
      throw new CatalogValidationException("query", "Must contain at most 200 characters");
    if (r.category() != null
        && !r.category().isBlank()
        && !Set.of(
                "bakery",
                "meals",
                "groceries",
                "desserts",
                "fruit-and-veg",
                "fruitAndVeg",
                "drinks",
                "other")
            .contains(r.category()))
      throw new CatalogValidationException("category", "Unknown category");
    if (r.minDiscount() != null
        && (r.minDiscount().signum() < 0 || r.minDiscount().compareTo(BigDecimal.valueOf(100)) > 0))
      throw new CatalogValidationException("minDiscount", "Must be between 0 and 100");
    if (r.maxPrice() != null && r.maxPrice().signum() < 0)
      throw new CatalogValidationException("maxPrice", "Must be non-negative");
  }
}

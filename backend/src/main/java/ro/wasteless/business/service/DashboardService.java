package ro.wasteless.business.service;

import java.math.*;
import java.sql.Timestamp;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ro.wasteless.business.api.DashboardResponse;
import ro.wasteless.common.api.*;
import ro.wasteless.order.infrastructure.BusinessOwnerLookup;

@Service
public class DashboardService {
  private final JdbcTemplate jdbc;
  private final BusinessOwnerLookup owner;
  private final Clock clock;

  public DashboardService(JdbcTemplate jdbc, BusinessOwnerLookup owner, Clock clock) {
    this.jdbc = jdbc;
    this.owner = owner;
    this.clock = clock;
  }

  @Transactional(readOnly = true)
  @PreAuthorize("hasRole('BUSINESS')")
  public DashboardResponse dashboard(LocalDate from, LocalDate to) {
    ZoneId zone = ZoneId.of("Europe/Bucharest");
    from = from == null ? clock.instant().atZone(zone).toLocalDate() : from;
    to = to == null ? from : to;
    if (to.isBefore(from) || ChronoUnit.DAYS.between(from, to) > 366)
      throw new DomainException(
          ErrorCode.VALIDATION_FAILED,
          HttpStatus.BAD_REQUEST,
          "Date range must be ordered and at most 367 days inclusive");
    var business = owner.currentBusiness();
    var parameters =
        Map.of(
            "business",
            business,
            "start",
            Timestamp.from(from.atStartOfDay(zone).toInstant()),
            "end",
            Timestamp.from(to.plusDays(1).atStartOfDay(zone).toInstant()));
    var aggregate =
        new NamedParameterJdbcTemplate(jdbc)
            .queryForMap(
                """
            select coalesce(sum(total_price) filter(where status='COMPLETED' and completed_at>=:start and completed_at<:end),0) revenue,
              coalesce(sum(quantity) filter(where status='COMPLETED' and completed_at>=:start and completed_at<:end),0) products,
              count(*) filter(where status='ACTIVE' and created_at>=:start and created_at<:end) active,
              count(*) filter(where status='COMPLETED' and completed_at>=:start and completed_at<:end) completed,
              count(*) filter(where status='CANCELED' and canceled_at>=:start and canceled_at<:end) canceled
            from orders where business_id=:business and currency='EUR'
            """,
                parameters);
    var offers =
        jdbc.queryForMap(
            """
            select count(*) filter(where status='ACTIVE' and quantity_available>0 and pickup_end_at>?
              and (expires_at is null or expires_at>?)) active,
              count(*) filter(where status='PAUSED') paused from offers where business_id=? and deleted_at is null
            """,
            Timestamp.from(clock.instant()),
            Timestamp.from(clock.instant()),
            business);
    BigDecimal revenue = ((BigDecimal) aggregate.get("revenue")).setScale(2);
    BigDecimal commission =
        revenue.multiply(new BigDecimal("0.10")).setScale(2, RoundingMode.HALF_UP);
    long products = ((Number) aggregate.get("products")).longValue();
    return new DashboardResponse(
        revenue,
        commission,
        revenue.subtract(commission),
        products,
        BigDecimal.valueOf(products).multiply(new BigDecimal("1.2")),
        ((Number) offers.get("active")).longValue(),
        ((Number) offers.get("paused")).longValue(),
        ((Number) aggregate.get("active")).longValue(),
        ((Number) aggregate.get("completed")).longValue(),
        ((Number) aggregate.get("canceled")).longValue(),
        "EUR",
        from,
        to);
  }
}

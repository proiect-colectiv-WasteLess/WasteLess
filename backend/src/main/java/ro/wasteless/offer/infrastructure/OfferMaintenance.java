package ro.wasteless.offer.infrastructure;

import io.micrometer.core.instrument.MeterRegistry;
import java.sql.Timestamp;
import java.time.Clock;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.*;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@EnableScheduling
public class OfferMaintenance {
  private final JdbcTemplate jdbc;
  private final Clock clock;
  private final MeterRegistry metrics;

  public OfferMaintenance(JdbcTemplate jdbc, Clock clock, MeterRegistry metrics) {
    this.jdbc = jdbc;
    this.clock = clock;
    this.metrics = metrics;
  }

  @Scheduled(fixedDelayString = "${app.offer-maintenance-delay-ms:300000}")
  @Transactional
  public void reconcile() {
    if (!Boolean.TRUE.equals(
        jdbc.queryForObject("select pg_try_advisory_xact_lock(193847523)", Boolean.class))) return;
    var sample = io.micrometer.core.instrument.Timer.start(metrics);
    try {
      var now = Timestamp.from(clock.instant());
      int expired =
          jdbc.update(
              "update offers set status='EXPIRED',updated_at=?,version=version+1 where deleted_at is null and status in ('ACTIVE','PAUSED','SOLD_OUT') and pickup_end_at<=?",
              now,
              now);
      int sold =
          jdbc.update(
              "update offers set status='SOLD_OUT',sold_out_at=coalesce(sold_out_at,?),updated_at=?,version=version+1 where deleted_at is null and status='ACTIVE' and quantity_available=0 and pickup_end_at>?",
              now,
              now,
              now);
      int restored =
          jdbc.update(
              "update offers set status='ACTIVE',sold_out_at=null,updated_at=?,version=version+1 where deleted_at is null and status='SOLD_OUT' and quantity_available>0 and pickup_end_at>?",
              now,
              now);
      metrics.counter("offers.expired").increment(expired);
      metrics.counter("offers.reconciled", "state", "sold_out").increment(sold);
      metrics.counter("offers.reconciled", "state", "restored").increment(restored);
      LoggerFactory.getLogger(getClass())
          .info("Offer reconciliation expired={} soldOut={} restored={}", expired, sold, restored);
    } finally {
      sample.stop(metrics.timer("offers.maintenance.duration"));
    }
  }
}

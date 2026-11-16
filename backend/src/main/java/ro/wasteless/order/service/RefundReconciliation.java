package ro.wasteless.order.service;

import org.slf4j.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.*;
import org.springframework.stereotype.Component;

@Component
public class RefundReconciliation {
  private static final Logger log = LoggerFactory.getLogger(RefundReconciliation.class);
  private final JdbcTemplate jdbc;
  private final OrderCancellationService cancellation;

  public RefundReconciliation(JdbcTemplate jdbc, OrderCancellationService cancellation) {
    this.jdbc = jdbc;
    this.cancellation = cancellation;
  }

  @Scheduled(
      fixedDelayString = "${app.orders.refund-reconciliation-delay-ms:60000}",
      initialDelayString = "${app.orders.refund-reconciliation-delay-ms:60000}")
  public void reconcile() {
    var references =
        jdbc.queryForList(
            """
            select distinct o.reference from refunds r join orders o on o.id=r.order_id
            where o.status='ACTIVE' and (r.status='PENDING' or
              (r.status='FAILED' and r.failure_reason like 'TRANSIENT_FAILURE%')) order by o.reference limit 100
            """,
            String.class);
    for (String reference : references) {
      try {
        cancellation.reconcile(reference);
      } catch (RuntimeException e) {
        log.warn("Refund reconciliation deferred for order {}", reference);
      }
    }
  }
}

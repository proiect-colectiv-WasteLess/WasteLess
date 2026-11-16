package ro.wasteless.order.infrastructure;

import java.sql.Timestamp;
import java.time.Clock;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.*;
import ro.wasteless.common.port.OrderAudit;

@Repository
public class JdbcOrderAudit implements OrderAudit {
  private final JdbcTemplate jdbc;
  private final Clock clock;

  public JdbcOrderAudit(JdbcTemplate jdbc, Clock clock) {
    this.jdbc = jdbc;
    this.clock = clock;
  }

  @Transactional(propagation = Propagation.MANDATORY)
  public void append(UUID order, String from, String to, UUID actor, String reason) {
    jdbc.update(
        "insert into order_status_history(order_id,from_status,to_status,changed_by_user_id,reason,created_at) values(?,?,?,?,?,?)",
        order,
        from,
        to,
        actor,
        reason,
        Timestamp.from(clock.instant()));
  }
}

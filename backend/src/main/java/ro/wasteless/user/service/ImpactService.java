package ro.wasteless.user.service;

import java.math.BigDecimal;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ro.wasteless.common.security.CurrentUser;
import ro.wasteless.user.api.ImpactResponse;

@Service
public class ImpactService {
  private final JdbcTemplate jdbc;
  private final CurrentUser current;

  public ImpactService(JdbcTemplate jdbc, CurrentUser current) {
    this.jdbc = jdbc;
    this.current = current;
  }

  @Transactional(readOnly = true)
  @PreAuthorize("hasRole('CLIENT')")
  public ImpactResponse impact() {
    return jdbc.queryForObject(
        """
            select coalesce(sum(quantity),0) meals,coalesce(sum((original_unit_price-unit_price)*quantity),0) saved
            from orders where customer_user_id=? and status='COMPLETED' and currency='EUR'
            """,
        (r, n) -> {
          long meals = r.getLong("meals");
          return new ImpactResponse(
              meals,
              r.getBigDecimal("saved").setScale(2),
              BigDecimal.valueOf(meals).multiply(new BigDecimal("1.2")),
              ImpactLevels.level(meals),
              ImpactLevels.remaining(meals),
              "EUR");
        },
        current.id());
  }
}

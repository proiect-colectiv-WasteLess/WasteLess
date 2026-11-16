package ro.wasteless.order.service;

import java.time.Clock;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import ro.wasteless.order.api.OrderResponse;
import ro.wasteless.order.domain.FoodOrder;

@Component
public class OrderViews {
  private final JdbcTemplate jdbc;
  private final Clock clock;

  public OrderViews(JdbcTemplate jdbc, Clock clock) {
    this.jdbc = jdbc;
    this.clock = clock;
  }

  public OrderResponse view(FoodOrder order) {
    var refund =
        jdbc.query(
            "select status,amount,currency,failure_reason from refunds where order_id=? order by requested_at desc,id desc limit 1",
            (r, n) ->
                new OrderResponse.RefundView(
                    r.getString(1), r.getBigDecimal(2), r.getString(3), r.getString(4)),
            order.id);
    return OrderResponse.of(order, clock).withRefund(refund.isEmpty() ? null : refund.getFirst());
  }
}

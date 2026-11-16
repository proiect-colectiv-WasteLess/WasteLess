package ro.wasteless.payment.service;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Clock;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import ro.wasteless.common.port.PaymentGateway;

@Service
public class RefundProcessor {
  private final JdbcTemplate jdbc;
  private final PaymentGateway gateway;
  private final Clock clock;

  public RefundProcessor(JdbcTemplate jdbc, PaymentGateway gateway, Clock clock) {
    this.jdbc = jdbc;
    this.gateway = gateway;
    this.clock = clock;
  }

  @Transactional(propagation = Propagation.MANDATORY)
  public boolean refund(UUID order, String reason) {
    var payment = jdbc.queryForMap("select * from payments where order_id=? for update", order);
    UUID paymentId = (UUID) payment.get("id");
    BigDecimal captured = (BigDecimal) payment.get("amount");
    var refunds =
        jdbc.queryForList(
            "select * from refunds where order_id=? order by requested_at,id for update", order);
    if (refunds.stream().anyMatch(r -> "SUCCEEDED".equals(r.get("status")))) return true;
    UUID refundId = refunds.isEmpty() ? UUID.randomUUID() : (UUID) refunds.getFirst().get("id");
    if (!"CAPTURED".equals(payment.get("status")) && !"SUCCEEDED".equals(payment.get("status")))
      return false;
    if (refunds.isEmpty())
      jdbc.update(
          """
            insert into refunds(id,payment_id,order_id,amount,currency,status,reason,requested_at)
            values(?,?,?,?,?,'PENDING',?,?)
            """,
          refundId,
          paymentId,
          order,
          captured,
          payment.get("currency"),
          reason,
          Timestamp.from(clock.instant()));
    else
      jdbc.update(
          "update refunds set status='PENDING',reason=?,failure_reason=null where id=?",
          reason,
          refundId);
    var result =
        gateway.refund((String) payment.get("provider_reference"), captured, "refund:" + order);
    boolean success = "SUCCEEDED".equals(result.status());
    jdbc.update(
        "update refunds set status=?,provider_reference=?,processed_at=?,failure_reason=?,updated_at=? where id=?",
        success ? "SUCCEEDED" : "FAILED",
        result.reference(),
        Timestamp.from(clock.instant()),
        success ? null : result.status(),
        Timestamp.from(clock.instant()),
        refundId);
    if (success)
      jdbc.update(
          "update payments set status='REFUNDED',updated_at=? where id=?",
          Timestamp.from(clock.instant()),
          paymentId);
    return success;
  }
}

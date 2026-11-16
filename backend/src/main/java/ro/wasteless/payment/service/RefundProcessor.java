package ro.wasteless.payment.service;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import ro.wasteless.common.port.PaymentGateway;

@Service
public class RefundProcessor {
  private final JdbcTemplate jdbc;
  private final PaymentGateway gateway;
  private final Clock clock;
  private final int maxAttempts;
  private final long backoff;
  private final long pendingAge;
  private final ro.wasteless.common.port.OrderAudit audit;

  public RefundProcessor(
      JdbcTemplate jdbc,
      PaymentGateway gateway,
      Clock clock,
      @Value("${app.orders.refund-max-attempts:3}") int maxAttempts,
      @Value("${app.orders.refund-backoff-seconds:30}") long backoff,
      @Value("${app.orders.refund-pending-age-seconds:300}") long pendingAge,
      ro.wasteless.common.port.OrderAudit audit) {
    if (maxAttempts < 1 || maxAttempts > 8 || backoff < 1 || pendingAge < 0)
      throw new IllegalArgumentException("Invalid refund retry policy");
    this.jdbc = jdbc;
    this.gateway = gateway;
    this.clock = clock;
    this.maxAttempts = maxAttempts;
    this.backoff = backoff;
    this.pendingAge = pendingAge;
    this.audit = audit;
  }

  @Transactional(propagation = Propagation.MANDATORY)
  public boolean refund(UUID order, String reason) {
    var payment = jdbc.queryForMap("select * from payments where order_id=? for update", order);
    UUID paymentId = (UUID) payment.get("id");
    BigDecimal captured = (BigDecimal) payment.get("amount");
    BigDecimal returned =
        jdbc.queryForObject(
            "select coalesce(sum(amount),0) from refunds where payment_id=? and status='SUCCEEDED'",
            BigDecimal.class,
            paymentId);
    if (returned.compareTo(captured) >= 0) return returned.compareTo(captured) == 0;
    if (!List.of("CAPTURED", "SUCCEEDED").contains(payment.get("status"))) return false;
    var rows =
        jdbc.queryForList(
            "select * from refunds where order_id=? and status<>'SUCCEEDED' order by requested_at,id for update",
            order);
    Map<String, Object> previous = rows.isEmpty() ? null : rows.getFirst();
    if (previous != null && !due(previous)) return false;
    UUID refundId = previous == null ? UUID.randomUUID() : (UUID) previous.get("id");
    BigDecimal amount = captured.subtract(returned);
    int attempt = previous == null ? 1 : attempts((String) previous.get("failure_reason")) + 1;
    if (previous == null)
      jdbc.update(
          """
            insert into refunds(id,payment_id,order_id,amount,currency,status,reason,requested_at)
            values(?,?,?,?,?,'PENDING',?,?)
            """,
          refundId,
          paymentId,
          order,
          amount,
          payment.get("currency"),
          reason,
          Timestamp.from(clock.instant()));
    else
      jdbc.update(
          "update refunds set status='PENDING',amount=?,reason=?,failure_reason=null where id=?",
          amount,
          reason,
          refundId);
    audit.append(
        order,
        previous == null ? null : "REFUND_" + previous.get("status"),
        "REFUND_PENDING",
        (UUID) payment.get("user_id"),
        "Refund requested");
    var result =
        gateway.refund((String) payment.get("provider_reference"), amount, "refund:" + refundId);
    boolean success = "SUCCEEDED".equals(result.status());
    boolean transientFailure = List.of("TRANSIENT_FAILURE", "TIMEOUT").contains(result.status());
    String failure =
        success
            ? null
            : (transientFailure && attempt < maxAttempts
                    ? "TRANSIENT_FAILURE"
                    : "PERMANENT_FAILURE")
                + "#"
                + attempt;
    jdbc.update(
        "update refunds set status=?,provider_reference=?,processed_at=?,failure_reason=?,updated_at=? where id=?",
        success ? "SUCCEEDED" : "FAILED",
        result.reference(),
        Timestamp.from(clock.instant()),
        failure,
        Timestamp.from(clock.instant()),
        refundId);
    audit.append(
        order,
        "REFUND_PENDING",
        success ? "REFUND_SUCCEEDED" : "REFUND_FAILED",
        (UUID) payment.get("user_id"),
        success ? "Refund processed" : failure);
    if (success)
      jdbc.update(
          "update payments set status='REFUNDED',updated_at=? where id=?",
          Timestamp.from(clock.instant()),
          paymentId);
    return success;
  }

  public boolean retryDue(UUID order) {
    return jdbc
        .queryForList("select * from refunds where order_id=? and status<>'SUCCEEDED'", order)
        .stream()
        .anyMatch(this::due);
  }

  private boolean due(Map<String, Object> row) {
    String failure = (String) row.get("failure_reason");
    if ("FAILED".equals(row.get("status"))
        && (failure == null || !failure.startsWith("TRANSIENT_FAILURE"))) return false;
    int attempt = attempts(failure);
    if (attempt >= maxAttempts) return false;
    Instant processed = instant(row.get("processed_at"));
    Instant requested = instant(row.get("requested_at"));
    long delay =
        processed == null ? pendingAge : Math.min(1800, backoff * (1L << Math.max(0, attempt - 1)));
    return !clock
        .instant()
        .isBefore((processed == null ? requested : processed).plusSeconds(delay));
  }

  private Instant instant(Object value) {
    if (value == null) return null;
    if (value instanceof java.sql.Timestamp t) return t.toInstant();
    return ((OffsetDateTime) value).toInstant();
  }

  private int attempts(String failure) {
    if (failure == null) return 0;
    try {
      return Integer.parseInt(failure.substring(failure.lastIndexOf('#') + 1));
    } catch (RuntimeException e) {
      return maxAttempts;
    }
  }
}

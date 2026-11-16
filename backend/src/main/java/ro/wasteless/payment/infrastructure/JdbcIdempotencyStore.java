package ro.wasteless.payment.infrastructure;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.*;
import ro.wasteless.common.port.IdempotencyStore;

@Repository
@Transactional(propagation = Propagation.MANDATORY)
public class JdbcIdempotencyStore implements IdempotencyStore {
  private final JdbcTemplate jdbc;
  private final Clock clock;

  public JdbcIdempotencyStore(JdbcTemplate jdbc, Clock clock) {
    this.jdbc = jdbc;
    this.clock = clock;
  }

  public boolean claim(UUID user, String scope, String key, String hash, Instant expiresAt) {
    jdbc.update(
        "delete from idempotency_keys where user_id=? and scope=? and key=? and expires_at<=?",
        user,
        scope,
        key,
        Timestamp.from(clock.instant()));
    return jdbc.update(
            """
            insert into idempotency_keys(user_id,scope,key,request_hash,expires_at)
            values(?,?,?,?,?) on conflict (user_id,scope,key) do nothing
            """,
            user,
            scope,
            key,
            hash,
            Timestamp.from(expiresAt))
        == 1;
  }

  public Optional<Result> find(UUID user, String scope, String key) {
    return jdbc
        .query(
            "select request_hash,response_status,response_body::text from idempotency_keys where user_id=? and scope=? and key=? and expires_at>?",
            (r, n) -> new Result(r.getString(1), r.getObject(2, Integer.class), r.getString(3)),
            user,
            scope,
            key,
            Timestamp.from(clock.instant()))
        .stream()
        .findFirst();
  }

  public void complete(UUID user, String scope, String key, int status, String body) {
    if (jdbc.update(
            "update idempotency_keys set response_status=?,response_body=?::jsonb where user_id=? and scope=? and key=? and response_status is null",
            status,
            body,
            user,
            scope,
            key)
        != 1) throw new IllegalStateException("Idempotency claim missing");
  }

  public int purgeExpired() {
    return jdbc.update(
        "delete from idempotency_keys where expires_at<=?", Timestamp.from(clock.instant()));
  }
}

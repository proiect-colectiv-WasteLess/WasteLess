package ro.wasteless.order;

import static org.assertj.core.api.Assertions.*;

import java.util.*;
import org.junit.jupiter.api.Test;

class OrderIdempotencyIT extends OrderTestSupport {
  @Test
  void replayMismatchAndExpiry() throws Exception {
    var c = catalog();
    var u = client();
    String key = UUID.randomUUID().toString();
    var first = place(u, c.offer(), 1, key);
    var again = place(u, c.offer(), 1, key);
    assertThat(first.getResponse().getStatus()).isEqualTo(201);
    assertThat(again.getResponse().getStatus()).isEqualTo(201);
    assertThat(body(again)).isEqualTo(body(first));
    assertThat(stock(c.offer())).isEqualTo(9);
    var mismatch = place(u, c.offer(), 2, key);
    assertThat(mismatch.getResponse().getStatus()).isEqualTo(409);
    assertThat(body(mismatch).get("code").asText()).isEqualTo("IDEMPOTENCY_MISMATCH");
    jdbc.update(
        "update idempotency_keys set expires_at=? where user_id=? and key=?",
        java.sql.Timestamp.from(ro.wasteless.support.TestFoundation.NOW.minusSeconds(1)),
        u,
        key);
    assertThat(place(u, c.offer(), 1, key).getResponse().getStatus()).isEqualTo(201);
    assertThat(stock(c.offer())).isEqualTo(8);
    assertThat(
            jdbc.queryForObject("select count(*) from payments where user_id=?", Integer.class, u))
        .isEqualTo(2);
  }
}

package ro.wasteless.order;

import static org.assertj.core.api.Assertions.*;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import ro.wasteless.common.api.DomainException;
import ro.wasteless.order.domain.*;

class OrderStateTest {
  @Test
  void everyAllowedAndRejectedTransition() {
    var now = Instant.parse("2026-11-16T12:00:00Z");
    var actor = UUID.randomUUID();
    var done = new FoodOrder();
    done.itemName = "Snapshot";
    done.offerId = null;
    done.complete(actor, now);
    assertThat(done.status).isEqualTo(OrderStatus.COMPLETED);
    assertThat(done.completedAt).isEqualTo(now);
    assertThat(done.completedByUserId).isEqualTo(actor);
    assertThat(done.itemName).isEqualTo("Snapshot");
    assertThatThrownBy(() -> done.cancel("reason", now)).isInstanceOf(DomainException.class);
    assertThatThrownBy(() -> done.complete(actor, now)).isInstanceOf(DomainException.class);
    var canceled = new FoodOrder();
    canceled.cancel("reason", now);
    assertThat(canceled.status).isEqualTo(OrderStatus.CANCELED);
    assertThat(canceled.canceledAt).isEqualTo(now);
    assertThatThrownBy(() -> canceled.complete(actor, now)).isInstanceOf(DomainException.class);
    assertThatThrownBy(() -> canceled.cancel("again", now)).isInstanceOf(DomainException.class);
  }
}

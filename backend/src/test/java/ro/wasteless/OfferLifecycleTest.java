package ro.wasteless;

import static org.assertj.core.api.Assertions.*;

import java.math.BigDecimal;
import java.time.*;
import org.junit.jupiter.api.Test;
import ro.wasteless.common.api.DomainException;
import ro.wasteless.offer.domain.*;

class OfferLifecycleTest {
  static final Instant NOW = Instant.parse("2026-11-20T12:00:00Z");

  static Offer offer() {
    var o = new Offer();
    o.quantityTotal = 10;
    o.quantityAvailable = 10;
    o.originalPrice = new BigDecimal("12.00");
    o.price = new BigDecimal("4.00");
    o.pickupStartAt = NOW.plusSeconds(3600);
    o.pickupEndAt = NOW.plusSeconds(7200);
    return o;
  }

  @Test
  void reserveAndRestoreAreBounded() {
    var o = offer();
    o.reserve(10, NOW);
    assertThat(o.getStatus()).isEqualTo(OfferStatus.SOLD_OUT);
    assertThatThrownBy(() -> o.reserve(1, NOW)).isInstanceOf(DomainException.class);
    o.restore(4, NOW);
    assertThat(o.quantityAvailable).isEqualTo(4);
    assertThat(o.getStatus()).isEqualTo(OfferStatus.ACTIVE);
    o.restore(Integer.MAX_VALUE, NOW);
    assertThat(o.quantityAvailable).isEqualTo(10);
    assertThatThrownBy(() -> o.reserve(0, NOW)).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> o.restore(-1, NOW)).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void pausedAndDeletedOffersCannotBeSold() {
    var o = offer();
    o.reserve(3, NOW);
    o.pause(NOW);
    var paused = o.pausedAt;
    o.pause(NOW.plusSeconds(1));
    assertThat(o.pausedAt).isEqualTo(paused);
    assertThatThrownBy(() -> o.reserve(1, NOW)).isInstanceOf(DomainException.class);
    o.restore(1, NOW);
    assertThat(o.getStatus()).isEqualTo(OfferStatus.PAUSED);
    o.resume(NOW);
    assertThat(o.getStatus()).isEqualTo(OfferStatus.ACTIVE);
    o.delete(NOW);
    o.restore(5, NOW);
    assertThat(o.quantityAvailable).isEqualTo(8);
    assertThatThrownBy(() -> o.resume(NOW)).isInstanceOf(DomainException.class);
    assertThatThrownBy(() -> o.pause(NOW)).isInstanceOf(DomainException.class);
  }

  @Test
  void expiryIsTerminalAndReconciliationIdempotent() {
    var o = offer();
    o.pause(NOW);
    assertThat(o.reconcile(o.pickupEndAt)).isTrue();
    assertThat(o.reconcile(o.pickupEndAt)).isFalse();
    assertThat(o.getStatus()).isEqualTo(OfferStatus.EXPIRED);
    assertThatThrownBy(() -> o.resume(NOW)).isInstanceOf(DomainException.class);
    o.restore(2, NOW.plusSeconds(10000));
    assertThat(o.getStatus()).isEqualTo(OfferStatus.EXPIRED);
  }

  @Test
  void quantitiesRetainExistingReservationsAndDisplayIsConsistent() {
    var o = offer();
    o.reserve(4, NOW);
    o.changeQuantity(15);
    assertThat(o.quantityAvailable).isEqualTo(11);
    assertThatThrownBy(() -> o.changeQuantity(3)).isInstanceOf(DomainException.class);
    assertThat(o.stockLevel()).isEqualTo("medium");
    assertThat(o.discountPercent()).isEqualTo(67);
    assertThat(o.pickupWindow()).isEqualTo("15:00–16:00");
    o.quantityAvailable = 26;
    assertThat(o.stockLevel()).isEqualTo("high");
  }
}

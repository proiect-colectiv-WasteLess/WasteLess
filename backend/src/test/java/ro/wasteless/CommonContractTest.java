package ro.wasteless;

import static org.assertj.core.api.Assertions.*;

import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import ro.wasteless.common.api.DomainException;
import ro.wasteless.common.domain.Money;
import ro.wasteless.common.security.*;

class CommonContractTest {
  @Test
  void moneyRejectsSilentRounding() {
    assertThatThrownBy(() -> Money.eur(new BigDecimal("1.001")))
        .isInstanceOf(ArithmeticException.class);
    assertThatThrownBy(() -> Money.eur(new BigDecimal("-1")))
        .isInstanceOf(IllegalArgumentException.class);
    assertThat(Money.eur(new BigDecimal("1"))).isEqualTo(new BigDecimal("1.00"));
  }

  @Test
  void ownershipRequiresAuthenticatedMatchingUser() {
    var user = new CurrentUser();
    var owner = UUID.randomUUID();
    try {
      assertThatThrownBy(user::id).isInstanceOf(DomainException.class);
      SecurityContextHolder.getContext()
          .setAuthentication(
              new UsernamePasswordAuthenticationToken(owner.toString(), null, java.util.List.of()));
      new OwnershipGuard(user).requireOwner(owner);
      assertThatThrownBy(() -> new OwnershipGuard(user).requireOwner(UUID.randomUUID()))
          .isInstanceOf(DomainException.class);
    } finally {
      SecurityContextHolder.clearContext();
    }
  }
}

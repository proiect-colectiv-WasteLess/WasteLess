package ro.wasteless;

import static org.assertj.core.api.Assertions.*;

import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import ro.wasteless.auth.domain.*;
import ro.wasteless.auth.infrastructure.AuthRateLimiter;
import ro.wasteless.common.api.*;
import ro.wasteless.common.security.*;
import ro.wasteless.support.TestFoundation;

class AuthPolicyTest {
  @Test
  void perIpBudgetCannotBeBypassedByChangingAccounts() {
    var limiter = new AuthRateLimiter(Clock.fixed(TestFoundation.NOW, ZoneOffset.UTC));
    for (int i = 0; i < 100; i++) limiter.ip("192.0.2.1");
    assertThatThrownBy(() -> limiter.ip("192.0.2.1"))
        .isInstanceOf(AuthRateLimiter.ThrottledException.class);
    assertThatCode(() -> limiter.ip("192.0.2.2")).doesNotThrowAnyException();
  }

  @Test
  void accountBudgetCannotBeBypassedByChangingCaseOrIp() {
    var limiter = new AuthRateLimiter(Clock.fixed(TestFoundation.NOW, ZoneOffset.UTC));
    for (int i = 0; i < 20; i++) limiter.account("login", "USER@example.test");
    assertThatThrownBy(() -> limiter.account("login", "user@example.test"))
        .isInstanceOf(AuthRateLimiter.ThrottledException.class);
    assertThatCode(() -> limiter.account("login", "other@example.test")).doesNotThrowAnyException();
  }

  @Test
  void crossAccountResourcesReturnNotFound() throws Exception {
    var owner = UUID.randomUUID();
    try (var ignored = TestFoundation.asUser(UUID.randomUUID(), "BUSINESS")) {
      var guard = new OwnershipGuard(new CurrentUser());
      assertThatThrownBy(() -> guard.requireOwner(owner))
          .isInstanceOfSatisfying(
              DomainException.class,
              e -> {
                assertThat(e.status().value()).isEqualTo(404);
                assertThat(e.code()).isEqualTo(ErrorCode.NOT_FOUND);
              });
    }
  }

  @Test
  void administrativeAndTimedLocksHaveDifferentLifecycles() {
    var u = new User();
    u.status = UserStatus.LOCKED;
    assertThat(u.canAuthenticate(TestFoundation.NOW)).isFalse();
    u.lockedUntil = TestFoundation.NOW.plusSeconds(1);
    assertThat(u.canAuthenticate(TestFoundation.NOW)).isFalse();
    u.lockedUntil = TestFoundation.NOW;
    assertThat(u.canAuthenticate(TestFoundation.NOW)).isTrue();
    u.status = UserStatus.SUSPENDED;
    assertThat(u.canAuthenticate(TestFoundation.NOW)).isFalse();
    u.status = UserStatus.ACTIVE;
    u.deletedAt = TestFoundation.NOW;
    assertThat(u.canAuthenticate(TestFoundation.NOW)).isFalse();
  }
}

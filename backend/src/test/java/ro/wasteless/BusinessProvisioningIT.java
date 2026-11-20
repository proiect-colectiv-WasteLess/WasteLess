package ro.wasteless;

import static org.assertj.core.api.Assertions.*;

import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import ro.wasteless.common.event.UserRegisteredEvent;
import ro.wasteless.support.*;

class BusinessProvisioningIT extends PostgisIntegrationTest {
  @Autowired JdbcTemplate jdbc;
  @Autowired ApplicationEventPublisher events;
  @Autowired PlatformTransactionManager manager;

  @Test
  void committedBusinessRegistrationProvisionsOnceAndRollbackProvisionsNothing() {
    var transaction = new TransactionTemplate(manager);
    var fixtures = new Fixtures(jdbc);
    var owner = fixtures.user();
    var role =
        jdbc.queryForObject(
            "insert into roles(code,name) values ('ROLE_BUSINESS','Business') on conflict(code) do update set name=excluded.name returning id",
            UUID.class);
    jdbc.update("insert into user_roles(user_id,role_id) values (?,?)", owner, role);
    var event =
        new UserRegisteredEvent(
            owner,
            TestFoundation.NOW,
            "Registration Bakery",
            new BigDecimal("44.4268"),
            new BigDecimal("26.1025"));
    transaction.executeWithoutResult(s -> events.publishEvent(event));
    events.publishEvent(event); // A also supports publication after its transaction has committed.
    assertThat(
            jdbc.queryForObject(
                "select count(*) from businesses where owner_user_id=?", Integer.class, owner))
        .isEqualTo(1);
    assertThat(
            jdbc.queryForObject(
                "select count(*) from business_locations l join businesses b on b.id=l.business_id where b.owner_user_id=? and l.is_primary",
                Integer.class,
                owner))
        .isEqualTo(1);
    var rolledBack = fixtures.user();
    jdbc.update("insert into user_roles(user_id,role_id) values (?,?)", rolledBack, role);
    transaction.executeWithoutResult(
        s -> {
          events.publishEvent(
              new UserRegisteredEvent(
                  rolledBack, TestFoundation.NOW, "Rollback Bakery", null, null));
          s.setRollbackOnly();
        });
    assertThat(
            jdbc.queryForObject(
                "select count(*) from businesses where owner_user_id=?", Integer.class, rolledBack))
        .isZero();
    var client = fixtures.user();
    events.publishEvent(
        new UserRegisteredEvent(client, TestFoundation.NOW, "Not a business", null, null));
    assertThat(
            jdbc.queryForObject(
                "select count(*) from businesses where owner_user_id=?", Integer.class, client))
        .isZero();
  }
}

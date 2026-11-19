package ro.wasteless;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import ro.wasteless.support.*;

class SchemaIntegrityIT extends PostgisIntegrationTest {
  @Autowired JdbcTemplate jdbc;
  @Autowired PlatformTransactionManager manager;

  void rollback(java.util.function.Consumer<Fixtures> work) {
    new TransactionTemplate(manager)
        .executeWithoutResult(
            s -> {
              work.accept(new Fixtures(jdbc));
              s.setRollbackOnly();
            });
  }

  @Test
  void rejectsCrossBusinessOfferLocation() {
    assertThatThrownBy(
            () ->
                rollback(
                    f -> {
                      var a = f.catalog();
                      var b = f.catalog();
                      f.offer(a.business(), b.location(), f.category());
                    }))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void rejectsInvalidStock() {
    assertThatThrownBy(
            () ->
                rollback(
                    f -> {
                      var a = f.catalog();
                      jdbc.update("update offers set quantity_available=11 where id=?", a.offer());
                    }))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void rejectsDuplicatePayment() {
    assertThatThrownBy(
            () ->
                rollback(
                    f -> {
                      var a = f.catalog();
                      var customer = f.user();
                      var order = f.order(customer, a.business(), a.location(), a.offer());
                      f.payment(order, customer);
                      f.payment(order, customer);
                    }))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void financialHistorySurvivesOfferDeletion() {
    rollback(
        f -> {
          var a = f.catalog();
          var customer = f.user();
          var order = f.order(customer, a.business(), a.location(), a.offer());
          var payment = f.payment(order, customer);
          f.refund(payment, order);
          jdbc.update("delete from offers where id=?", a.offer());
          assertThat(
                  jdbc.queryForObject(
                      "select offer_id from orders where id=?", java.util.UUID.class, order))
              .isNull();
          assertThat(
                  jdbc.queryForObject(
                      "select item_name from orders where id=?", String.class, order))
              .isEqualTo("Bag");
        });
  }

  @Test
  void rejectsUserDeletionWithFinancialHistory() {
    assertThatThrownBy(
            () ->
                rollback(
                    f -> {
                      var a = f.catalog();
                      var customer = f.user();
                      f.order(customer, a.business(), a.location(), a.offer());
                      jdbc.update("delete from users where id=?", customer);
                    }))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void requiredIndexesExist() {
    for (var name :
        java.util.List.of(
            "ix_location_geo",
            "ix_offer_title_search",
            "ix_orders_customer_status",
            "ix_orders_business_status",
            "uq_payment_idempotency",
            "ix_idempotency_expiry"))
      assertThat(
              jdbc.queryForObject(
                  "select count(*) from pg_indexes where schemaname='public' and indexname=?",
                  Integer.class,
                  name))
          .as(name)
          .isEqualTo(1);
  }

  @Test
  void fixedClockIsInjected(@Autowired java.time.Clock clock) {
    assertThat(clock.instant()).isEqualTo(TestFoundation.NOW);
  }
}

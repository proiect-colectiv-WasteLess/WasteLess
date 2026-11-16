package ro.wasteless;

import static org.assertj.core.api.Assertions.*;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import ro.wasteless.support.PostgisIntegrationTest;

class DemoDataIT extends PostgisIntegrationTest {
  @Autowired JdbcTemplate jdbc;

  @Test
  void productionDatabaseHasNoDemoAccounts() {
    assertThat(
            jdbc.queryForObject(
                "select count(*) from users where email in ('noa@wasteless.app','freshbakery@wasteless.app')",
                Integer.class))
        .isZero();
  }

  @Test
  void devSeedsAreRepeatableAndFinanciallyConsistent() {
    String schema = "demo_" + java.util.UUID.randomUUID().toString().replace("-", "");
    var flyway =
        Flyway.configure()
            .dataSource(DB.getJdbcUrl(), DB.getUsername(), DB.getPassword())
            .schemas(schema)
            .locations("classpath:db/migration", "classpath:db/dev")
            .placeholders(java.util.Map.of("demoSeedEnabled", "true"))
            .load();
    try {
      flyway.migrate();
      flyway.migrate();
      var data =
          new org.springframework.jdbc.datasource.SingleConnectionDataSource(
              DB.getJdbcUrl(), DB.getUsername(), DB.getPassword(), true);
      try {
        var db = new JdbcTemplate(data);
        db.execute("set search_path to " + schema + ",public");
        assertThat(
                db.queryForObject(
                    "select count(*) from orders where status='ACTIVE'", Integer.class))
            .isEqualTo(3);
        assertThat(
                db.queryForObject(
                    "select count(*) from orders where status='COMPLETED'", Integer.class))
            .isEqualTo(3);
        assertThat(
                db.queryForObject(
                    "select count(*) from orders where status='CANCELED'", Integer.class))
            .isEqualTo(1);
        assertThat(db.queryForObject("select count(*) from payments", Integer.class)).isEqualTo(7);
        assertThat(
                db.queryForObject(
                    "select count(*) from refunds where status='SUCCEEDED'", Integer.class))
            .isEqualTo(1);
        assertThat(db.queryForObject("select count(*) from offers", Integer.class)).isEqualTo(4);
        var hash =
            db.queryForObject(
                "select password_hash from users where email='noa@wasteless.app'", String.class);
        assertThat(new BCryptPasswordEncoder().matches("WasteLess-Dev-Only-2026!", hash)).isTrue();
        assertThat(
                db.queryForObject(
                    "select count(*) from orders o join payments p on p.order_id=o.id where p.amount<>o.total_price",
                    Integer.class))
            .isZero();
      } finally {
        data.destroy();
      }
    } finally {
      Flyway.configure()
          .dataSource(DB.getJdbcUrl(), DB.getUsername(), DB.getPassword())
          .schemas(schema)
          .cleanDisabled(false)
          .load()
          .clean();
    }
  }
}

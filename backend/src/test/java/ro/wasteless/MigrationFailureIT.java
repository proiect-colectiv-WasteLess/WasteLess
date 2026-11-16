package ro.wasteless;

import static org.assertj.core.api.Assertions.*;

import java.nio.file.*;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import ro.wasteless.support.PostgisIntegrationTest;

class MigrationFailureIT extends PostgisIntegrationTest {
  @TempDir Path directory;

  @Test
  void brokenMigrationIsIdentifiedAndBaselineUpgradeWorks() throws Exception {
    String schema = "migration_" + java.util.UUID.randomUUID().toString().replace("-", "");
    var first =
        Flyway.configure()
            .dataSource(DB.getJdbcUrl(), DB.getUsername(), DB.getPassword())
            .schemas(schema)
            .locations("classpath:db/migration")
            .target("2")
            .load();
    try {
      first.migrate();
      var upgrade =
          Flyway.configure()
              .dataSource(DB.getJdbcUrl(), DB.getUsername(), DB.getPassword())
              .schemas(schema)
              .locations("classpath:db/migration")
              .load();
      upgrade.migrate();
      assertThat(
              java.util.Arrays.stream(upgrade.info().applied())
                  .map(m -> m.getScript())
                  .filter(script -> script.startsWith("V"))
                  .toList())
          .containsExactly(
              "V1__identity.sql",
              "V2__catalog.sql",
              "V3__orders_payments.sql",
              "V4__search_indexes.sql",
              "V5__order_indexes.sql",
              "V6__four_digit_pickup_codes.sql");
      Files.writeString(directory.resolve("V7__deliberately_broken.sql"), "THIS IS NOT SQL;");
      var broken =
          Flyway.configure()
              .dataSource(DB.getJdbcUrl(), DB.getUsername(), DB.getPassword())
              .schemas(schema)
              .locations("classpath:db/migration", "filesystem:" + directory)
              .load();
      assertThatThrownBy(broken::migrate).hasStackTraceContaining("V7__deliberately_broken.sql");
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

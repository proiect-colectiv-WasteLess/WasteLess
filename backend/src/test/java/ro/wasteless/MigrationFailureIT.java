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
      assertThat(upgrade.info().applied()).hasSize(5);
      Files.writeString(directory.resolve("V6__deliberately_broken.sql"), "THIS IS NOT SQL;");
      var broken =
          Flyway.configure()
              .dataSource(DB.getJdbcUrl(), DB.getUsername(), DB.getPassword())
              .schemas(schema)
              .locations("classpath:db/migration", "filesystem:" + directory)
              .load();
      assertThatThrownBy(broken::migrate).hasStackTraceContaining("V6__deliberately_broken.sql");
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

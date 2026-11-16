package ro.wasteless.support;

import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

public final class IntegrationDatabase {
  private static PostgreSQLContainer container;
  private static final Settings SETTINGS = create();

  private IntegrationDatabase() {}

  public record Settings(String getJdbcUrl, String getUsername, String getPassword) {}

  private static Settings create() {
    String url = System.getenv("TEST_DB_URL");
    if (url != null)
      return new Settings(
          url,
          System.getenv().getOrDefault("TEST_DB_USERNAME", "postgres"),
          System.getenv().getOrDefault("TEST_DB_PASSWORD", ""));
    container =
        new PostgreSQLContainer(
            DockerImageName.parse("postgis/postgis:16-3.4").asCompatibleSubstituteFor("postgres"));
    container.start();
    return new Settings(container.getJdbcUrl(), container.getUsername(), container.getPassword());
  }

  public static Settings settings() {
    return SETTINGS;
  }
}

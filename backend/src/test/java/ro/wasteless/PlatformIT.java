package ro.wasteless;

import static org.assertj.core.api.Assertions.*;

import java.net.*;
import java.net.http.*;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import ro.wasteless.support.PostgisIntegrationTest;

class PlatformIT extends PostgisIntegrationTest {
  @Autowired JdbcTemplate jdbc;
  @Autowired Flyway flyway;
  @LocalServerPort int port;

  @Test
  void migratesEmptyDatabaseAndValidatesChecksums() {
    flyway.validate();
    assertThat(flyway.info().applied()).hasSize(6);
    assertThat(jdbc.queryForObject("select PostGIS_Version()", String.class)).isNotBlank();
    assertThat(
            jdbc.queryForObject(
                "select count(*) from information_schema.tables where table_schema='public' and table_name in ('users','orders','payments','refunds','offers')",
                Integer.class))
        .isEqualTo(5);
  }

  @Test
  void emailUniquenessIsCaseInsensitive() {
    var email = java.util.UUID.randomUUID() + "@example.test";
    jdbc.update("insert into users(email,password_hash) values (?,?)", email, "hash");
    assertThatThrownBy(
            () ->
                jdbc.update(
                    "insert into users(email,password_hash) values (?,?)",
                    email.toUpperCase(),
                    "hash"))
        .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
  }

  @Test
  void publicHealthAndProtectedApi() throws Exception {
    var client = HttpClient.newHttpClient();
    var health =
        client.send(
            HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/actuator/health"))
                .build(),
            HttpResponse.BodyHandlers.ofString());
    assertThat(health.statusCode()).isEqualTo(200);
    var denied =
        client.send(
            HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/v1/users/me"))
                .header("X-Request-Id", "test-request")
                .build(),
            HttpResponse.BodyHandlers.ofString());
    assertThat(denied.statusCode()).isEqualTo(401);
    assertThat(denied.body()).contains("UNAUTHORIZED", "test-request");
  }
}

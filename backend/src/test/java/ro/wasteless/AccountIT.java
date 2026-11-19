package ro.wasteless;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.event.*;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import ro.wasteless.auth.api.UserLocationAccess;
import ro.wasteless.auth.infrastructure.JwtTokens;
import ro.wasteless.common.event.*;
import ro.wasteless.common.port.MailerPort;
import ro.wasteless.support.*;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@AutoConfigureMockMvc
@RecordApplicationEvents
class AccountIT extends PostgisIntegrationTest {
  @Autowired MockMvc mvc;
  @Autowired JsonMapper json;
  @Autowired JdbcTemplate jdbc;
  @Autowired ApplicationEvents events;
  @Autowired UserLocationAccess locationAccess;
  @Autowired ro.wasteless.auth.infrastructure.RefreshTokenRepository refreshRepository;
  @MockitoBean MailerPort mailer;
  private static final AtomicInteger NETWORK = new AtomicInteger();
  private String address;
  private static final String PASSWORD = "Secret123456";

  @BeforeEach
  void seedRoles() {
    address = "10.0.0." + NETWORK.incrementAndGet();
    for (var role : List.of("CLIENT", "BUSINESS", "ADMIN"))
      jdbc.update(
          "insert into roles(code,name) values (?,?) on conflict(code) do nothing",
          "ROLE_" + role,
          role);
  }

  private ResultActions call(MockHttpServletRequestBuilder request) throws Exception {
    return mvc.perform(
        request.with(
            r -> {
              r.setRemoteAddr(address);
              return r;
            }));
  }

  private MockHttpServletRequestBuilder body(MockHttpServletRequestBuilder request, Object value) {
    return request.contentType("application/json").content(json.writeValueAsString(value));
  }

  private JsonNode tree(ResultActions result) throws Exception {
    return json.readTree(result.andReturn().getResponse().getContentAsString());
  }

  private String email() {
    return UUID.randomUUID() + "@example.test";
  }

  private JsonNode register(String email, String role) throws Exception {
    var request =
        new HashMap<String, Object>(
            Map.of(
                "email",
                email,
                "role",
                role,
                "password",
                PASSWORD,
                "firstName",
                "Luca",
                "lastName",
                "Test"));
    if (role.equals("BUSINESS")) request.put("businessName", "Bakery");
    return tree(call(body(post("/api/v1/auth/register"), request)).andExpect(status().isCreated()));
  }

  private JsonNode login(String email) throws Exception {
    return tree(
        call(body(
                post("/api/v1/auth/login"),
                Map.of("email", email, "password", PASSWORD, "deviceId", "test-device")))
            .andExpect(status().isOk()));
  }

  private String access(JsonNode session) {
    return session.path("accessToken").asString();
  }

  private String refresh(JsonNode session) {
    return session.path("refreshToken").asString();
  }

  private UUID id(JsonNode user) {
    return UUID.fromString(user.path("id").asString());
  }

  private ResultActions rotate(String refresh) throws Exception {
    return call(body(post("/api/v1/auth/refresh"), Map.of("refreshToken", refresh)));
  }

  private String mailedToken() {
    var text = ArgumentCaptor.forClass(String.class);
    verify(mailer).send(anyString(), anyString(), text.capture());
    return text.getValue().substring(text.getValue().lastIndexOf(' ') + 1);
  }

  private JsonNode admin(String email) throws Exception {
    var user = register(email, "CLIENT");
    jdbc.update(
        "insert into user_roles(user_id,role_id) select ?,id from roles where code='ROLE_ADMIN'",
        id(user));
    return login(email);
  }

  @Test
  void registrationNormalizesEmailHashesPasswordAndDoesNotLogIn() throws Exception {
    String email = email();
    var user = register(email.toUpperCase(Locale.ROOT), "CLIENT");
    assertThat(user.path("email").asString()).isEqualTo(email);
    assertThat(user.path("roles").toString()).contains("ROLE_CLIENT");
    assertThat(user.has("passwordHash")).isFalse();
    assertThat(user.has("accessToken")).isFalse();
    String hash =
        jdbc.queryForObject("select password_hash from users where id=?", String.class, id(user));
    assertThat(hash).startsWith("$2a$12$").isNotEqualTo(PASSWORD);
    call(body(
            post("/api/v1/auth/register"),
            Map.of(
                "email",
                email,
                "role",
                "CLIENT",
                "password",
                PASSWORD,
                "firstName",
                "Luca",
                "lastName",
                "Test")))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.fieldErrors[0].field").value("email"));
  }

  @Test
  void businessRegistrationEmitsOneCommittedEventWithProvisioningData() throws Exception {
    String email = email();
    var user = register(email, "BUSINESS");
    var published =
        events.stream(UserRegisteredEvent.class).filter(e -> e.userId().equals(id(user))).toList();
    assertThat(published).hasSize(1);
    assertThat(published.getFirst().businessName()).isEqualTo("Bakery");
    call(body(
            post("/api/v1/auth/register"),
            Map.of(
                "email",
                email,
                "role",
                "BUSINESS",
                "businessName",
                "Bakery",
                "password",
                PASSWORD,
                "firstName",
                "Luca",
                "lastName",
                "Test")))
        .andExpect(status().isConflict());
    assertThat(
            events.stream(UserRegisteredEvent.class)
                .filter(e -> e.userId().equals(id(user)))
                .count())
        .isEqualTo(1);
  }

  @Test
  void registrationValidatesPasswordRolePhoneAndBusinessFields() throws Exception {
    for (var entry :
        List.of(
            Map.entry("password", "short"),
            Map.entry("role", "ADMIN"),
            Map.entry("phone", "invalid"),
            Map.entry("email", "bad"))) {
      var payload =
          new HashMap<String, Object>(
              Map.of(
                  "email",
                  email(),
                  "role",
                  "CLIENT",
                  "password",
                  PASSWORD,
                  "firstName",
                  "Luca",
                  "lastName",
                  "Test"));
      payload.put(entry.getKey(), entry.getValue());
      call(body(post("/api/v1/auth/register"), payload)).andExpect(status().isBadRequest());
    }
    call(body(
            post("/api/v1/auth/register"),
            Map.of(
                "email",
                email(),
                "role",
                "BUSINESS",
                "password",
                PASSWORD,
                "firstName",
                "Luca",
                "lastName",
                "Test")))
        .andExpect(status().isBadRequest());
  }

  @Test
  void accessTokenWorksButTamperedExpiredAndWrongIssuerTokensFail() throws Exception {
    String email = email();
    var user = register(email, "CLIENT");
    var session = login(email);
    call(get("/api/v1/users/me").header("Authorization", "Bearer " + access(session)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.email").value(email));
    String[] parts = access(session).split("\\.");
    String signature = parts[2];
    String bad =
        parts[0]
            + "."
            + parts[1]
            + "."
            + (signature.charAt(0) == 'A' ? "B" : "A")
            + signature.substring(1);
    call(get("/api/v1/users/me").header("Authorization", "Bearer " + bad))
        .andExpect(status().isUnauthorized())
        .andExpect(content().contentTypeCompatibleWith("application/problem+json"));
    var expired =
        new JwtTokens(
                "test-only-placeholder-at-least-32-bytes",
                Clock.fixed(TestFoundation.NOW.minusSeconds(901), ZoneOffset.UTC))
            .issue(id(user), List.of("ROLE_CLIENT"));
    call(get("/api/v1/users/me").header("Authorization", "Bearer " + expired))
        .andExpect(status().isUnauthorized());
    String claims =
        new String(Base64.getUrlDecoder().decode(parts[1]), java.nio.charset.StandardCharsets.UTF_8)
            .replace("wasteless", "other");
    String content =
        parts[0]
            + "."
            + Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(claims.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    var mac = javax.crypto.Mac.getInstance("HmacSHA256");
    mac.init(
        new javax.crypto.spec.SecretKeySpec(
            "test-only-placeholder-at-least-32-bytes"
                .getBytes(java.nio.charset.StandardCharsets.UTF_8),
            "HmacSHA256"));
    String wrong =
        content
            + "."
            + Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(
                    mac.doFinal(content.getBytes(java.nio.charset.StandardCharsets.US_ASCII)));
    call(get("/api/v1/users/me").header("Authorization", "Bearer " + wrong))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void refreshRotationDetectsReplayRevokesFamilyAndKeepsOtherDevices() throws Exception {
    String email = email();
    register(email, "CLIENT");
    var first = login(email);
    var other = login(email);
    var second = tree(rotate(refresh(first)).andExpect(status().isOk()));
    assertThat(refresh(second)).isNotEqualTo(refresh(first));
    assertThat(jdbc.queryForList("select token_hash from refresh_tokens", String.class))
        .doesNotContain(refresh(first), refresh(second));
    rotate(refresh(first)).andExpect(status().isUnauthorized());
    rotate(refresh(second)).andExpect(status().isUnauthorized());
    rotate(refresh(other)).andExpect(status().isOk());
  }

  @Test
  void concurrentRefreshAllowsOnlyOneRotationAndRevokesReplayedFamily() throws Exception {
    String email = email();
    register(email, "CLIENT");
    var session = login(email);
    try (var executor = java.util.concurrent.Executors.newFixedThreadPool(2)) {
      var start = new java.util.concurrent.CountDownLatch(1);
      java.util.concurrent.Callable<MvcResult> action =
          () -> {
            start.await();
            return rotate(refresh(session)).andReturn();
          };
      var one = executor.submit(action);
      var two = executor.submit(action);
      start.countDown();
      var results = List.of(one.get(), two.get());
      assertThat(results.stream().map(r -> r.getResponse().getStatus()))
          .containsExactlyInAnyOrder(200, 401);
      var success =
          results.stream()
              .filter(r -> r.getResponse().getStatus() == 200)
              .findFirst()
              .orElseThrow();
      rotate(refresh(json.readTree(success.getResponse().getContentAsString())))
          .andExpect(status().isUnauthorized());
    }
  }

  @Test
  void logoutIsIdempotentAndLogoutAllRevokesEveryDevice() throws Exception {
    String email = email();
    register(email, "CLIENT");
    var one = login(email);
    var two = login(email);
    for (int i = 0; i < 2; i++)
      call(body(post("/api/v1/auth/logout"), Map.of("refreshToken", refresh(one))))
          .andExpect(status().isNoContent());
    rotate(refresh(one)).andExpect(status().isUnauthorized());
    call(get("/api/v1/users/me").header("Authorization", "Bearer " + access(one)))
        .andExpect(status().isOk());
    call(post("/api/v1/auth/logout-all").header("Authorization", "Bearer " + access(two)))
        .andExpect(status().isNoContent());
    rotate(refresh(two)).andExpect(status().isUnauthorized());
    login(email);
  }

  @Test
  void expiredRefreshFails() throws Exception {
    String email = email();
    var user = register(email, "CLIENT");
    var session = login(email);
    jdbc.update(
        "update refresh_tokens set expires_at=? where user_id=?",
        java.sql.Timestamp.from(TestFoundation.NOW.minusSeconds(1)),
        id(user));
    rotate(refresh(session)).andExpect(status().isUnauthorized());
  }

  @Test
  void cleanupKeepsExpiredAncestorsWhileAReplacementIsActive() throws Exception {
    String email = email();
    var user = register(email, "CLIENT");
    var first = login(email);
    var next = tree(rotate(refresh(first)).andExpect(status().isOk()));
    jdbc.update(
        "update refresh_tokens set expires_at=? where token_hash=?",
        java.sql.Timestamp.from(TestFoundation.NOW.minusSeconds(1)),
        ro.wasteless.auth.application.AccountService.hash(refresh(first)));
    refreshRepository.deleteByExpiresAtBefore(TestFoundation.NOW);
    assertThat(
            jdbc.queryForObject(
                "select count(*) from refresh_tokens where user_id=?", Integer.class, id(user)))
        .isEqualTo(2);
    rotate(refresh(first)).andExpect(status().isUnauthorized());
    rotate(refresh(next)).andExpect(status().isUnauthorized());
  }

  @Test
  void failedLoginLocksAfterFiveAndUnlocksAfterFifteenMinutes() throws Exception {
    String email = email();
    var user = register(email, "CLIENT");
    for (int i = 0; i < 5; i++)
      call(body(post("/api/v1/auth/login"), Map.of("email", email, "password", "wrong")))
          .andExpect(status().isUnauthorized())
          .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    assertThat(jdbc.queryForObject("select status from users where id=?", String.class, id(user)))
        .isEqualTo("LOCKED");
    call(body(post("/api/v1/auth/login"), Map.of("email", email, "password", PASSWORD)))
        .andExpect(status().isUnauthorized());
    jdbc.update(
        "update users set locked_until=? where id=?",
        java.sql.Timestamp.from(TestFoundation.NOW.minusSeconds(1)),
        id(user));
    login(email);
    assertThat(
            jdbc.queryForObject(
                "select failed_login_attempts from users where id=?", Integer.class, id(user)))
        .isZero();
  }

  @Test
  void missingAndInactiveAccountsUseGenericCredentialErrors() throws Exception {
    String email = email();
    var user = register(email, "CLIENT");
    call(body(post("/api/v1/auth/login"), Map.of("email", email(), "password", PASSWORD)))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.message").value("Invalid credentials"));
    for (String state : List.of("SUSPENDED", "DELETED", "LOCKED")) {
      jdbc.update("update users set status=?,locked_until=null where id=?", state, id(user));
      call(body(post("/api/v1/auth/login"), Map.of("email", email, "password", PASSWORD)))
          .andExpect(status().isUnauthorized())
          .andExpect(jsonPath("$.message").value("Invalid credentials"));
    }
  }

  @Test
  void resetIsSingleUseRevokesSessionsAndNeverEnumeratesAccounts() throws Exception {
    String email = email();
    var user = register(email, "CLIENT");
    var session = login(email);
    var known =
        tree(
            call(body(post("/api/v1/auth/password-reset/request"), Map.of("email", email)))
                .andExpect(status().isAccepted()));
    var unknown =
        tree(
            call(body(post("/api/v1/auth/password-reset/request"), Map.of("email", email())))
                .andExpect(status().isAccepted()));
    assertThat(known).isEqualTo(unknown);
    String token = mailedToken();
    assertThat(
            jdbc.queryForObject(
                "select token_hash from password_reset_tokens where user_id=?",
                String.class,
                id(user)))
        .doesNotContain(token);
    var payload = Map.of("token", token, "newPassword", "NewSecret12345");
    call(body(post("/api/v1/auth/password-reset/confirm"), payload))
        .andExpect(status().isNoContent());
    call(body(post("/api/v1/auth/password-reset/confirm"), payload))
        .andExpect(status().isBadRequest());
    rotate(refresh(session)).andExpect(status().isUnauthorized());
    call(body(post("/api/v1/auth/login"), Map.of("email", email, "password", "NewSecret12345")))
        .andExpect(status().isOk());
  }

  @Test
  void resetExpiryAndPasswordChangeAreEnforced() throws Exception {
    String email = email();
    var user = register(email, "CLIENT");
    var session = login(email);
    call(body(post("/api/v1/auth/password-reset/request"), Map.of("email", email)))
        .andExpect(status().isAccepted());
    String token = mailedToken();
    jdbc.update(
        "update password_reset_tokens set expires_at=? where user_id=?",
        java.sql.Timestamp.from(TestFoundation.NOW.minusSeconds(1)),
        id(user));
    call(body(
            post("/api/v1/auth/password-reset/confirm"),
            Map.of("token", token, "newPassword", "NewSecret12345")))
        .andExpect(status().isBadRequest());
    call(body(
            post("/api/v1/users/me/password").header("Authorization", "Bearer " + access(session)),
            Map.of("currentPassword", "wrong", "newPassword", "NewSecret12345")))
        .andExpect(status().isUnauthorized());
    call(body(
            post("/api/v1/users/me/password").header("Authorization", "Bearer " + access(session)),
            Map.of("currentPassword", PASSWORD, "newPassword", "NewSecret12345")))
        .andExpect(status().isNoContent());
    rotate(refresh(session)).andExpect(status().isUnauthorized());
  }

  @Test
  void verificationIsSingleUseAndExpires() throws Exception {
    String email = email();
    var user = register(email, "CLIENT");
    call(body(post("/api/v1/auth/email-verification/request"), Map.of("email", email)))
        .andExpect(status().isAccepted());
    String token = mailedToken();
    call(body(post("/api/v1/auth/email-verification/confirm"), Map.of("token", token)))
        .andExpect(status().isNoContent());
    call(body(post("/api/v1/auth/email-verification/confirm"), Map.of("token", token)))
        .andExpect(status().isBadRequest());
    assertThat(
            jdbc.queryForObject(
                "select email_verified from users where id=?", Boolean.class, id(user)))
        .isTrue();
    reset(mailer);
    String second = email();
    var secondUser = register(second, "CLIENT");
    call(body(post("/api/v1/auth/email-verification/request"), Map.of("email", second)))
        .andExpect(status().isAccepted());
    String expired = mailedToken();
    jdbc.update(
        "update email_verification_tokens set expires_at=? where user_id=?",
        java.sql.Timestamp.from(TestFoundation.NOW.minusSeconds(1)),
        id(secondUser));
    call(body(post("/api/v1/auth/email-verification/confirm"), Map.of("token", expired)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void profilePatchIsPartialRejectsSensitiveKeysAndValidatesValues() throws Exception {
    String email = email();
    register(email, "CLIENT");
    var session = login(email);
    call(body(
            patch("/api/v1/users/me").header("Authorization", "Bearer " + access(session)),
            Map.of("firstName", "Updated", "preferredLocale", "ro-RO")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.firstName").value("Updated"))
        .andExpect(jsonPath("$.lastName").value("Test"));
    for (var payload :
        List.of(
            Map.of("email", "new@example.test"),
            Map.of("roles", List.of("ROLE_ADMIN")),
            Map.of("preferredLocale", "bad_locale"),
            Map.of("avatarUrl", "javascript:alert(1)"),
            Map.of("phone", "bad"),
            Map.of("firstName", " "))) {
      call(body(
              patch("/api/v1/users/me").header("Authorization", "Bearer " + access(session)),
              payload))
          .andExpect(status().isBadRequest());
    }
  }

  @Test
  void locationRequiresConsentIsPrivateAndDeletedOnWithdrawal() throws Exception {
    String email = email();
    var user = register(email, "CLIENT");
    var session = login(email);
    var auth = "Bearer " + access(session);
    var coordinates = Map.of("latitude", 44.4, "longitude", 26.1, "accuracyMeters", 5);
    call(body(put("/api/v1/users/me/location").header("Authorization", auth), coordinates))
        .andExpect(status().isConflict());
    call(body(
            patch("/api/v1/users/me").header("Authorization", auth),
            Map.of("locationSharingEnabled", true)))
        .andExpect(status().isOk());
    call(body(
            put("/api/v1/users/me/location").header("Authorization", auth),
            Map.of("latitude", 91, "longitude", 26)))
        .andExpect(status().isBadRequest());
    call(body(put("/api/v1/users/me/location").header("Authorization", auth), coordinates))
        .andExpect(status().isNoContent());
    try (var ignored = TestFoundation.asUser(id(user), "CLIENT")) {
      assertThat(locationAccess.currentLocation()).isPresent();
    }
    try (var ignored = TestFoundation.asUser(UUID.randomUUID(), "CLIENT")) {
      assertThat(locationAccess.currentLocation()).isEmpty();
    }
    try (var ignored = TestFoundation.asUser(id(user), "BUSINESS")) {
      assertThatThrownBy(locationAccess::currentLocation)
          .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
    }
    call(body(
            patch("/api/v1/users/me").header("Authorization", auth),
            Map.of("locationSharingEnabled", false)))
        .andExpect(status().isOk());
    assertThat(
            jdbc.queryForObject(
                "select count(*) from user_locations where user_id=?", Integer.class, id(user)))
        .isZero();
  }

  @Test
  void adminSuspensionInvalidatesExistingAccessAndRefreshImmediately() throws Exception {
    var admin = admin(email());
    String email = email();
    var user = register(email, "CLIENT");
    var session = login(email);
    call(post("/api/v1/admin/users/" + id(user) + "/suspend")
            .header("Authorization", "Bearer " + access(admin)))
        .andExpect(status().isOk());
    call(get("/api/v1/users/me").header("Authorization", "Bearer " + access(session)))
        .andExpect(status().isUnauthorized());
    rotate(refresh(session)).andExpect(status().isUnauthorized());
    call(post("/api/v1/admin/users/" + id(user) + "/reactivate")
            .header("Authorization", "Bearer " + access(admin)))
        .andExpect(status().isOk());
    login(email);
  }

  @Test
  void adminFiltersPaginationRolesAndUnlockWork() throws Exception {
    var admin = admin(email());
    String email = email();
    var user = register(email, "CLIENT");
    var session = login(email);
    String auth = "Bearer " + access(admin);
    call(get("/api/v1/admin/users")
            .param("email", email)
            .param("role", "ROLE_CLIENT")
            .param("status", "ACTIVE")
            .header("Authorization", auth))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalElements").value(1))
        .andExpect(jsonPath("$.content[0].email").value(email));
    call(get("/api/v1/admin/users").param("size", "101").header("Authorization", auth))
        .andExpect(status().isBadRequest());
    call(body(
            post("/api/v1/admin/users/" + id(user) + "/roles").header("Authorization", auth),
            Map.of("add", List.of("ROLE_BUSINESS"), "remove", List.of("ROLE_CLIENT"))))
        .andExpect(status().isOk());
    var changed = tree(rotate(refresh(session)).andExpect(status().isOk()));
    assertThat(changed.path("user").path("roles").toString())
        .contains("ROLE_BUSINESS")
        .doesNotContain("ROLE_CLIENT");
    call(post("/api/v1/admin/users/" + id(user) + "/lock").header("Authorization", auth))
        .andExpect(status().isOk());
    call(get("/api/v1/users/me").header("Authorization", "Bearer " + access(changed)))
        .andExpect(status().isUnauthorized());
    call(post("/api/v1/admin/users/" + id(user) + "/unlock").header("Authorization", auth))
        .andExpect(status().isOk());
    login(email);
  }

  @Test
  void nonAdminsCannotUseAdminAndOppositeRoleRoutes() throws Exception {
    for (String role : List.of("CLIENT", "BUSINESS")) {
      String email = email();
      register(email, role);
      var session = login(email);
      String auth = "Bearer " + access(session);
      call(get("/api/v1/admin/users").header("Authorization", auth))
          .andExpect(status().isForbidden());
      call(post("/api/v1/admin/users/" + UUID.randomUUID() + "/lock").header("Authorization", auth))
          .andExpect(status().isForbidden());
      call(get(role.equals("CLIENT") ? "/api/v1/businesses/me/offers" : "/api/v1/orders")
              .header("Authorization", auth))
          .andExpect(status().isForbidden());
    }
    call(get("/api/v1/businesses/me")).andExpect(status().isUnauthorized());
  }

  @Test
  void accountDeletionIsIdempotentPreservesUserRowAndEmitsOneEvent() throws Exception {
    String email = email();
    var user = register(email, "CLIENT");
    var fixtures = new Fixtures(jdbc);
    var catalog = fixtures.catalog();
    var order = fixtures.order(id(user), catalog.business(), catalog.location(), catalog.offer());
    var payment = fixtures.payment(order, id(user));
    var session = login(email);
    String auth = "Bearer " + access(session);
    call(body(
            delete("/api/v1/users/me").header("Authorization", auth), Map.of("password", "wrong")))
        .andExpect(status().isUnauthorized());
    for (int i = 0; i < 2; i++)
      call(body(
              delete("/api/v1/users/me").header("Authorization", auth),
              Map.of("password", PASSWORD)))
          .andExpect(status().isAccepted());
    assertThat(
            events.stream(AccountDeletionRequestedEvent.class)
                .filter(e -> e.userId().equals(id(user)))
                .count())
        .isEqualTo(1);
    assertThat(jdbc.queryForObject("select status from users where id=?", String.class, id(user)))
        .isEqualTo("DELETED");
    assertThat(
            jdbc.queryForObject(
                "select customer_user_id from orders where id=?", UUID.class, order))
        .isEqualTo(id(user));
    assertThat(jdbc.queryForObject("select user_id from payments where id=?", UUID.class, payment))
        .isEqualTo(id(user));
    rotate(refresh(session)).andExpect(status().isUnauthorized());
    call(get("/api/v1/users/me").header("Authorization", auth))
        .andExpect(status().isUnauthorized());
    call(body(post("/api/v1/auth/login"), Map.of("email", email, "password", PASSWORD)))
        .andExpect(status().isUnauthorized());
    call(body(
            post("/api/v1/auth/register"),
            Map.of(
                "email",
                email,
                "role",
                "CLIENT",
                "password",
                PASSWORD,
                "firstName",
                "Luca",
                "lastName",
                "Test")))
        .andExpect(status().isConflict());
  }

  @Test
  void lastActiveAdministratorCannotBeRemovedLockedSuspendedOrDeleted() throws Exception {
    String email = email();
    var session = admin(email);
    UUID adminId = UUID.fromString(session.path("user").path("id").asString());
    var others =
        jdbc.queryForList(
            "select distinct u.id from users u join user_roles ur on ur.user_id=u.id join roles r on r.id=ur.role_id where r.code='ROLE_ADMIN' and u.status='ACTIVE' and u.id<>?",
            UUID.class,
            adminId);
    try {
      for (var other : others) jdbc.update("update users set status='SUSPENDED' where id=?", other);
      String auth = "Bearer " + access(session);
      call(body(
              post("/api/v1/admin/users/" + adminId + "/roles").header("Authorization", auth),
              Map.of("add", List.of(), "remove", List.of("ROLE_ADMIN"))))
          .andExpect(status().isConflict());
      for (String action : List.of("lock", "suspend"))
        call(post("/api/v1/admin/users/" + adminId + "/" + action).header("Authorization", auth))
            .andExpect(status().isConflict());
      call(body(
              delete("/api/v1/users/me").header("Authorization", auth),
              Map.of("password", PASSWORD)))
          .andExpect(status().isConflict());
      assertThat(jdbc.queryForObject("select status from users where id=?", String.class, adminId))
          .isEqualTo("ACTIVE");
    } finally {
      for (var other : others) jdbc.update("update users set status='ACTIVE' where id=?", other);
    }
  }

  @Test
  void everySuccessfulAdministrativeActionIsAuditedWithoutCredentials() throws Exception {
    var logger =
        (ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory.getLogger("security.audit");
    var appender =
        new ch.qos.logback.core.read.ListAppender<ch.qos.logback.classic.spi.ILoggingEvent>();
    appender.start();
    logger.addAppender(appender);
    try {
      var admin = admin(email());
      String email = email();
      var target = register(email, "CLIENT");
      String auth = "Bearer " + access(admin);
      for (String action : List.of("lock", "unlock", "suspend", "reactivate"))
        call(post("/api/v1/admin/users/" + id(target) + "/" + action).header("Authorization", auth))
            .andExpect(status().isOk());
      call(body(
              post("/api/v1/admin/users/" + id(target) + "/roles").header("Authorization", auth),
              Map.of("add", List.of("ROLE_BUSINESS"), "remove", List.of())))
          .andExpect(status().isOk());
      assertThat(appender.list).hasSize(5);
      for (var entry : appender.list)
        assertThat(entry.getFormattedMessage())
            .contains(
                "actor=" + admin.path("user").path("id").asString(),
                "subject=" + id(target),
                "at=" + TestFoundation.NOW)
            .doesNotContain(PASSWORD, access(admin), refresh(admin), email);
    } finally {
      logger.detachAppender(appender);
      appender.stop();
    }
  }

  @Test
  void profileAndLocationCannotTargetAnotherUsersId() throws Exception {
    String first = email();
    var one = register(first, "CLIENT");
    var session = login(first);
    String second = email();
    var two = register(second, "CLIENT");
    call(body(
            patch("/api/v1/users/me").header("Authorization", "Bearer " + access(session)),
            Map.of("id", id(two).toString(), "firstName", "Changed")))
        .andExpect(status().isBadRequest());
    call(get("/api/v1/users/" + id(two)).header("Authorization", "Bearer " + access(session)))
        .andExpect(status().isNotFound());
    assertThat(
            jdbc.queryForObject("select first_name from users where id=?", String.class, id(two)))
        .isEqualTo("Luca");
  }

  @Test
  void recoveryThrottleReturnsRetryAfterWithoutAccountEnumeration() throws Exception {
    String email = email();
    for (int i = 0; i < 5; i++)
      call(body(post("/api/v1/auth/password-reset/request"), Map.of("email", email)))
          .andExpect(status().isAccepted());
    call(body(post("/api/v1/auth/password-reset/request"), Map.of("email", email)))
        .andExpect(status().isTooManyRequests())
        .andExpect(header().exists("Retry-After"))
        .andExpect(jsonPath("$.code").value("RATE_LIMITED"));
  }

  @Test
  void sessionsAreOwnedAndCannotBeRevokedAcrossUsers() throws Exception {
    String first = email();
    register(first, "CLIENT");
    var one = login(first);
    String second = email();
    register(second, "CLIENT");
    var two = login(second);
    var sessions =
        tree(
            call(get("/api/v1/auth/sessions").header("Authorization", "Bearer " + access(one)))
                .andExpect(status().isOk()));
    String id = sessions.get(0).path("id").asString();
    call(delete("/api/v1/auth/sessions/" + id).header("Authorization", "Bearer " + access(two)))
        .andExpect(status().isNotFound());
    call(delete("/api/v1/auth/sessions/" + id).header("Authorization", "Bearer " + access(one)))
        .andExpect(status().isNoContent());
    rotate(refresh(one)).andExpect(status().isUnauthorized());
    rotate(refresh(two)).andExpect(status().isOk());
  }
}

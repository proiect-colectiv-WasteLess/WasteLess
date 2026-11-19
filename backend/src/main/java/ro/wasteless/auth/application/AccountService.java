package ro.wasteless.auth.application;

import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.*;
import java.util.*;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.*;
import ro.wasteless.auth.api.*;
import ro.wasteless.auth.api.AccountRequests.*;
import ro.wasteless.auth.domain.*;
import ro.wasteless.auth.infrastructure.*;
import ro.wasteless.common.api.*;
import ro.wasteless.common.event.*;
import ro.wasteless.common.port.MailerPort;
import ro.wasteless.common.security.CurrentUser;

@Service
@Transactional
public class AccountService implements UserAccountOperations {
  private final UserRepository users;
  private final RoleRepository roles;
  private final RefreshTokenRepository refresh;
  private final PasswordResetRepository resets;
  private final EmailVerificationRepository verifications;
  private final UserLocationRepository locations;
  private final PasswordEncoder passwords;
  private final JwtTokens jwt;
  private final Clock clock;
  private final CurrentUser current;
  private final MailerPort mailer;
  private final ApplicationEventPublisher events;
  private final SecureRandom random = new SecureRandom();
  private final String dummyHash;
  private static final org.slf4j.Logger AUDIT = org.slf4j.LoggerFactory.getLogger("security.audit");

  public AccountService(
      UserRepository users,
      RoleRepository roles,
      RefreshTokenRepository refresh,
      PasswordResetRepository resets,
      EmailVerificationRepository verifications,
      UserLocationRepository locations,
      PasswordEncoder passwords,
      JwtTokens jwt,
      Clock clock,
      CurrentUser current,
      MailerPort mailer,
      ApplicationEventPublisher events) {
    this.users = users;
    this.roles = roles;
    this.refresh = refresh;
    this.resets = resets;
    this.verifications = verifications;
    this.locations = locations;
    this.passwords = passwords;
    this.jwt = jwt;
    this.clock = clock;
    this.current = current;
    this.mailer = mailer;
    this.events = events;
    dummyHash = passwords.encode(UUID.randomUUID().toString());
  }

  public UserView register(Register request) {
    validatePassword(request.password());
    String email = normalize(request.email());
    if (users.existsByEmail(email)) throw new DuplicateEmailException();
    var user = new User();
    user.email = email;
    user.passwordHash = passwords.encode(request.password());
    user.firstName = request.firstName().trim();
    user.lastName = request.lastName().trim();
    user.phone = request.phone();
    try {
      users.saveAndFlush(user);
    } catch (org.springframework.dao.DataIntegrityViolationException e) {
      throw new DuplicateEmailException();
    }
    roles.add(user.getId(), "ROLE_" + request.role().name(), null, clock.instant());
    if (request.role() == AccountRequests.Role.BUSINESS)
      afterCommit(
          () ->
              events.publishEvent(
                  new UserRegisteredEvent(
                      user.getId(),
                      clock.instant(),
                      request.businessName().trim(),
                      request.latitude(),
                      request.longitude())));
    return view(user);
  }

  @Transactional(noRollbackFor = DomainException.class)
  public SessionTokens login(Login request, String agent, String address) {
    return issue(
        authenticateUser(request.email(), request.password()), request.deviceId(), agent, address);
  }

  @Transactional(noRollbackFor = DomainException.class)
  public UserView authenticate(String email, String password) {
    return view(authenticateUser(email, password));
  }

  private User authenticateUser(String email, String password) {
    var found = users.lockByEmail(normalize(email));
    var user = found.orElse(null);
    boolean validLength =
        password != null && password.getBytes(StandardCharsets.UTF_8).length <= 72;
    boolean matches =
        passwords.matches(
                validLength ? password : "invalid-password",
                user == null ? dummyHash : user.passwordHash)
            && validLength;
    var now = clock.instant();
    if (user == null) throw credentials();
    if (user.status == UserStatus.LOCKED
        && user.lockedUntil != null
        && !user.lockedUntil.isAfter(now)) {
      user.status = UserStatus.ACTIVE;
      user.lockedUntil = null;
      user.failedLoginAttempts = 0;
    }
    if (!user.canAuthenticate(now)) throw credentials();
    if (!matches) {
      user.failedLoginAttempts++;
      if (user.failedLoginAttempts >= 5) {
        user.status = UserStatus.LOCKED;
        user.lockedUntil = now.plusSeconds(900);
      }
      throw credentials();
    }
    user.failedLoginAttempts = 0;
    user.lockedUntil = null;
    user.lastLoginAt = now;
    return user;
  }

  @Transactional(noRollbackFor = DomainException.class)
  public SessionTokens rotate(String token, String agent, String address) {
    var original = refresh.findByTokenHash(hash(token)).orElseThrow(AccountService::credentials);
    var user = locked(original.userId);
    // The user lock serializes all session changes, including concurrent refresh and logout.
    var old = refresh.findById(original.id).orElseThrow(AccountService::credentials);
    // Refresh the managed entity after acquiring the user lock.
    entityManager.refresh(old);
    if (old.revokedAt != null) {
      if (old.replacedByTokenId != null) revokeFamily(old);
      throw credentials();
    }
    if (!old.expiresAt.isAfter(clock.instant()) || !user.canAuthenticate(clock.instant()))
      throw credentials();
    String raw = opaque();
    var next = newRefresh(user.getId(), raw, old.deviceId, agent, address);
    refresh.saveAndFlush(next);
    old.revokedAt = clock.instant();
    old.replacedByTokenId = next.id;
    return new SessionTokens(
        jwt.issue(user.getId(), roles.codes(user.getId())), raw, 900, "Bearer", view(user));
  }

  @jakarta.persistence.PersistenceContext private jakarta.persistence.EntityManager entityManager;

  public void logout(String token) {
    refresh
        .findByTokenHash(hash(token))
        .ifPresent(
            t -> {
              locked(t.userId);
              entityManager.refresh(t);
              if (t.revokedAt == null) t.revokedAt = clock.instant();
            });
  }

  @PreAuthorize("isAuthenticated()")
  public void logoutAll() {
    var u = locked(current.id());
    revokeAll(u.getId());
  }

  @Transactional(readOnly = true)
  @PreAuthorize("isAuthenticated()")
  public List<SessionView> sessions() {
    return refresh
        .findByUserIdAndRevokedAtIsNullAndExpiresAtAfter(current.id(), clock.instant())
        .stream()
        .map(t -> new SessionView(t.id, t.deviceId, t.userAgent, t.issuedAt, t.expiresAt))
        .toList();
  }

  @PreAuthorize("isAuthenticated()")
  public void revokeSession(UUID id) {
    var u = locked(current.id());
    var t = refresh.findById(id).orElseThrow(AccountService::notFound);
    if (!t.userId.equals(u.getId())) throw notFound();
    if (t.revokedAt == null) t.revokedAt = clock.instant();
  }

  public record SessionView(
      UUID id, String deviceId, String userAgent, Instant issuedAt, Instant expiresAt) {}

  public void requestReset(String email) {
    users
        .lockByEmail(normalize(email))
        .filter(u -> u.canAuthenticate(clock.instant()))
        .ifPresent(
            u -> {
              resets
                  .findByUserIdAndUsedAtIsNull(u.getId())
                  .forEach(t -> t.usedAt = clock.instant());
              String token = opaque();
              var row = new PasswordResetToken();
              initialize(row, u.getId(), token, Duration.ofMinutes(30));
              resets.save(row);
              afterCommit(
                  () -> mailer.send(u.email, "WasteLess password reset", "Reset token: " + token));
            });
  }

  public void confirmReset(String token, String password) {
    validatePassword(password);
    var row = resets.findByTokenHash(hash(token)).orElseThrow(AccountService::invalidToken);
    var user = locked(row.userId);
    entityManager.refresh(row);
    requireActionToken(row);
    if (!user.canAuthenticate(clock.instant())) throw invalidToken();
    user.passwordHash = passwords.encode(password);
    user.passwordChangedAt = clock.instant();
    user.failedLoginAttempts = 0;
    user.lockedUntil = null;
    user.status = UserStatus.ACTIVE;
    resets.findByUserIdAndUsedAtIsNull(user.getId()).forEach(t -> t.usedAt = clock.instant());
    revokeAll(user.getId());
  }

  @PreAuthorize("isAuthenticated()")
  public void changePassword(String old, String password) {
    validatePassword(password);
    var user = locked(current.id());
    if (!passwords.matches(old, user.passwordHash)) throw credentials();
    user.passwordHash = passwords.encode(password);
    user.passwordChangedAt = clock.instant();
    resets.findByUserIdAndUsedAtIsNull(user.getId()).forEach(t -> t.usedAt = clock.instant());
    revokeAll(user.getId());
  }

  public void requestVerification(String email) {
    users
        .lockByEmail(normalize(email))
        .filter(u -> u.canAuthenticate(clock.instant()) && !u.emailVerified)
        .ifPresent(
            u -> {
              verifications
                  .findByUserIdAndUsedAtIsNull(u.getId())
                  .forEach(t -> t.usedAt = clock.instant());
              String token = opaque();
              var row = new EmailVerificationToken();
              initialize(row, u.getId(), token, Duration.ofHours(24));
              verifications.save(row);
              afterCommit(
                  () ->
                      mailer.send(
                          u.email, "Verify your WasteLess email", "Verification token: " + token));
            });
  }

  public void confirmVerification(String token) {
    var row = verifications.findByTokenHash(hash(token)).orElseThrow(AccountService::invalidToken);
    var user = locked(row.userId);
    entityManager.refresh(row);
    requireActionToken(row);
    if (!user.canAuthenticate(clock.instant())) throw invalidToken();
    row.usedAt = clock.instant();
    user.emailVerified = true;
    user.emailVerifiedAt = clock.instant();
  }

  @Transactional(readOnly = true)
  @PreAuthorize("isAuthenticated()")
  public UserView me() {
    return view(users.findById(current.id()).orElseThrow(AccountService::notFound));
  }

  @PreAuthorize("isAuthenticated()")
  public UserView patch(Map<String, Object> values) {
    Set<String> allowed =
        Set.of(
            "firstName",
            "lastName",
            "phone",
            "avatarUrl",
            "preferredLocale",
            "locationSharingEnabled",
            "marketingOptIn");
    if (!allowed.containsAll(values.keySet())) throw invalid("Unsupported profile field");
    var u = locked(current.id());
    values.forEach(
        (key, value) -> {
          switch (key) {
            case "firstName" -> u.firstName = requiredName(value);
            case "lastName" -> u.lastName = requiredName(value);
            case "phone" -> {
              String s = nullableString(value, 30);
              if (s != null && !s.matches(AccountRequests.PHONE)) throw invalid("Invalid phone");
              u.phone = s;
            }
            case "avatarUrl" -> {
              String s = nullableString(value, 2048);
              if (s != null) {
                try {
                  var uri = java.net.URI.create(s);
                  if (!Set.of("https", "http").contains(uri.getScheme())
                      || uri.getHost() == null
                      || uri.getUserInfo() != null) throw new IllegalArgumentException();
                } catch (IllegalArgumentException e) {
                  throw invalid("Invalid avatar URL");
                }
              }
              u.avatarUrl = s;
            }
            case "preferredLocale" -> {
              String s = nullableString(value, 10);
              if (s == null || s.isBlank()) throw invalid("Locale required");
              try {
                new Locale.Builder().setLanguageTag(s).build();
                if (s.equalsIgnoreCase("und")) throw new IllformedLocaleException();
              } catch (IllformedLocaleException e) {
                throw invalid("Invalid locale");
              }
              u.preferredLocale = s;
            }
            case "locationSharingEnabled" -> {
              u.locationSharingEnabled = bool(value);
              if (!u.locationSharingEnabled) locations.deleteById(u.getId());
            }
            case "marketingOptIn" -> u.marketingOptIn = bool(value);
            default -> throw invalid("Unsupported profile field");
          }
        });
    return view(u);
  }

  @PreAuthorize("isAuthenticated()")
  public void putLocation(Location request) {
    var u = locked(current.id());
    if (!u.locationSharingEnabled) throw conflict("Location sharing consent required");
    var l = locations.findById(u.getId()).orElseGet(UserLocation::new);
    l.userId = u.getId();
    l.latitude = request.latitude();
    l.longitude = request.longitude();
    l.accuracyMeters = request.accuracyMeters();
    if (l.createdAt == null) l.createdAt = clock.instant();
    l.updatedAt = clock.instant();
    locations.save(l);
  }

  @PreAuthorize("isAuthenticated()")
  public void deleteLocation() {
    var u = locked(current.id());
    locations.deleteById(u.getId());
  }

  @PreAuthorize("isAuthenticated()")
  public void deleteAccount(String password) {
    roles.lockAdministration();
    var u = locked(current.id());
    if (!passwords.matches(password, u.passwordHash)) throw credentials();
    if (u.status == UserStatus.DELETED) return;
    protectLastAdmin(u, true);
    u.status = UserStatus.DELETED;
    u.deletedAt = clock.instant();
    u.locationSharingEnabled = false;
    locations.deleteById(u.getId());
    revokeAll(u.getId());
    resets.findByUserIdAndUsedAtIsNull(u.getId()).forEach(t -> t.usedAt = clock.instant());
    verifications.findByUserIdAndUsedAtIsNull(u.getId()).forEach(t -> t.usedAt = clock.instant());
    afterCommit(
        () -> events.publishEvent(new AccountDeletionRequestedEvent(u.getId(), clock.instant())));
  }

  @Transactional(readOnly = true)
  @PreAuthorize("hasRole('ADMIN')")
  public PageResponse<UserView> listUsers(
      String email, String status, String role, int page, int size) {
    if (page < 0 || size < 1 || size > 100) throw invalid("Invalid pagination");
    UserStatus state = null;
    if (status != null) {
      try {
        state = UserStatus.valueOf(status);
      } catch (IllegalArgumentException e) {
        throw invalid("Invalid status");
      }
    }
    if (role != null && !Set.of("ROLE_CLIENT", "ROLE_BUSINESS", "ROLE_ADMIN").contains(role))
      throw invalid("Invalid role");
    final var filter = state;
    var result =
        users.findAll(
            (root, query, cb) -> {
              var predicates = new ArrayList<jakarta.persistence.criteria.Predicate>();
              if (email != null) predicates.add(cb.equal(root.get("email"), normalize(email)));
              if (filter != null) predicates.add(cb.equal(root.get("status"), filter));
              if (role != null) {
                var sub = query.subquery(UUID.class);
                var ur = sub.from(UserRole.class);
                var r = sub.from(ro.wasteless.auth.domain.Role.class);
                sub.select(ur.get("userId"))
                    .where(cb.equal(ur.get("roleId"), r.get("id")), cb.equal(r.get("code"), role));
                predicates.add(root.get("id").in(sub));
              }
              return cb.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
            },
            PageRequest.of(page, size, Sort.by("createdAt").descending().and(Sort.by("id"))));
    return PageResponse.from(result.map(this::view));
  }

  @PreAuthorize("hasRole('ADMIN')")
  public UserView adminStatus(UUID id, String action) {
    roles.lockAdministration();
    var u = locked(id);
    if (u.status == UserStatus.DELETED) throw conflict("Deleted accounts cannot be changed");
    switch (action) {
      case "lock" -> {
        protectLastAdmin(u, true);
        u.status = UserStatus.LOCKED;
        u.lockedUntil = null;
        revokeAll(id);
      }
      case "suspend" -> {
        protectLastAdmin(u, true);
        u.status = UserStatus.SUSPENDED;
        u.lockedUntil = null;
        revokeAll(id);
      }
      case "unlock" -> {
        if (u.status != UserStatus.LOCKED && u.status != UserStatus.ACTIVE)
          throw conflict("Only locked users can be unlocked");
        activate(u);
      }
      case "reactivate" -> {
        if (u.status != UserStatus.SUSPENDED && u.status != UserStatus.ACTIVE)
          throw conflict("Only suspended users can be reactivated");
        activate(u);
      }
      default -> throw notFound();
    }
    audit(action, id);
    return view(u);
  }

  @PreAuthorize("hasRole('ADMIN')")
  public UserView adminRoles(UUID id, RoleChange change) {
    roles.lockAdministration();
    var u = locked(id);
    if (u.status == UserStatus.DELETED) throw conflict("Deleted accounts cannot be changed");
    if (change.remove().contains("ROLE_ADMIN")) protectLastAdmin(u, true);
    for (String role : change.add()) roles.add(id, role, current.id(), clock.instant());
    for (String role : change.remove()) roles.remove(id, role);
    audit("roles", id);
    return view(u);
  }

  private void audit(String action, UUID subject) {
    UUID actor = current.id();
    Instant at = clock.instant();
    afterCommit(
        () -> AUDIT.info("actor={} action={} subject={} at={}", actor, action, subject, at));
  }

  private void protectLastAdmin(User u, boolean removal) {
    if (removal
        && u.status == UserStatus.ACTIVE
        && roles.codes(u.getId()).contains("ROLE_ADMIN")
        && roles.activeAdmins() <= 1)
      throw conflict("Cannot remove or disable the last active administrator");
  }

  private void activate(User u) {
    u.status = UserStatus.ACTIVE;
    u.lockedUntil = null;
    u.failedLoginAttempts = 0;
  }

  private void revokeFamily(RefreshToken replay) {
    // The schema represents families as replacement chains rather than a family_id column.
    var tokens = refresh.findByUserId(replay.userId);
    var ids = new HashSet<UUID>();
    ids.add(replay.id);
    boolean changed;
    do {
      changed = false;
      for (var t : tokens)
        if (ids.contains(t.id) && t.replacedByTokenId != null)
          changed |= ids.add(t.replacedByTokenId);
      for (var t : tokens)
        if (t.replacedByTokenId != null && ids.contains(t.replacedByTokenId))
          changed |= ids.add(t.id);
    } while (changed);
    for (var t : tokens)
      if (ids.contains(t.id) && t.revokedAt == null) t.revokedAt = clock.instant();
  }

  private void revokeAll(UUID user) {
    refresh
        .findByUserIdAndRevokedAtIsNullAndExpiresAtAfter(user, clock.instant())
        .forEach(t -> t.revokedAt = clock.instant());
  }

  private SessionTokens issue(User user, String device, String agent, String address) {
    String token = opaque();
    refresh.save(newRefresh(user.getId(), token, device, agent, address));
    return new SessionTokens(
        jwt.issue(user.getId(), roles.codes(user.getId())), token, 900, "Bearer", view(user));
  }

  private RefreshToken newRefresh(
      UUID user, String raw, String device, String agent, String address) {
    var t = new RefreshToken();
    t.id = UUID.randomUUID();
    t.userId = user;
    t.tokenHash = hash(raw);
    t.deviceId = device;
    t.userAgent = agent == null ? null : agent.substring(0, Math.min(agent.length(), 1024));
    try {
      t.ipAddress = InetAddress.getByName(address);
    } catch (Exception e) {
      t.ipAddress = null;
    }
    t.issuedAt = clock.instant();
    t.createdAt = t.issuedAt;
    t.expiresAt = t.issuedAt.plus(Duration.ofDays(30));
    return t;
  }

  private void initialize(ActionToken t, UUID user, String token, Duration ttl) {
    t.id = UUID.randomUUID();
    t.userId = user;
    t.tokenHash = hash(token);
    t.createdAt = clock.instant();
    t.expiresAt = t.createdAt.plus(ttl);
  }

  private void requireActionToken(ActionToken t) {
    if (t.usedAt != null || !t.expiresAt.isAfter(clock.instant())) throw invalidToken();
  }

  private User locked(UUID id) {
    return users.lockById(id).orElseThrow(AccountService::notFound);
  }

  private UserView view(User u) {
    return new UserView(
        u.getId(),
        u.email,
        u.firstName,
        u.lastName,
        u.phone,
        u.avatarUrl,
        u.preferredLocale,
        u.status.name(),
        u.emailVerified,
        roles.codes(u.getId()),
        u.locationSharingEnabled,
        u.marketingOptIn,
        u.getCreatedAt());
  }

  private String opaque() {
    byte[] bytes = new byte[32];
    random.nextBytes(bytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }

  public static String hash(String value) {
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }

  private static String normalize(String s) {
    return s.trim().toLowerCase(Locale.ROOT);
  }

  private static void validatePassword(String s) {
    if (s == null
        || !s.matches(AccountRequests.PASSWORD)
        || s.getBytes(StandardCharsets.UTF_8).length > 72)
      throw invalid(
          "Password must have 10 characters, a letter and a number, and at most 72 UTF-8 bytes");
  }

  private static String requiredName(Object v) {
    String s = nullableString(v, 80);
    if (s == null || s.isBlank()) throw invalid("Name required");
    return s.trim();
  }

  private static String nullableString(Object v, int max) {
    if (v == null) return null;
    if (!(v instanceof String s) || s.length() > max) throw invalid("Invalid text field");
    return (String) v;
  }

  private static boolean bool(Object v) {
    if (!(v instanceof Boolean b)) throw invalid("Boolean required");
    return (Boolean) v;
  }

  private static DomainException credentials() {
    return new DomainException(
        ErrorCode.INVALID_CREDENTIALS, HttpStatus.UNAUTHORIZED, "Invalid credentials");
  }

  private static DomainException invalidToken() {
    return invalid("Invalid or expired token");
  }

  private static DomainException invalid(String message) {
    return new DomainException(ErrorCode.VALIDATION_FAILED, HttpStatus.BAD_REQUEST, message);
  }

  private static DomainException conflict(String message) {
    return new DomainException(ErrorCode.CONFLICT, HttpStatus.CONFLICT, message);
  }

  private static DomainException notFound() {
    return new DomainException(ErrorCode.NOT_FOUND, HttpStatus.NOT_FOUND, "Resource not found");
  }

  private static void afterCommit(Runnable action) {
    TransactionSynchronizationManager.registerSynchronization(
        new TransactionSynchronization() {
          @Override
          public void afterCommit() {
            action.run();
          }
        });
  }
}

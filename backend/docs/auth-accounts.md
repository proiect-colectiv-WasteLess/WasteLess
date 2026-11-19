# Authentication and accounts (WAS-15–WAS-27)

Branch: `luca/auth-account-tickets`, based on `main` at `56b5709`.

## Running and verification

Use Java 21 and Docker. From `backend`, run `./mvnw verify` (Windows: `mvnw.cmd verify`). The existing PostGIS Testcontainers foundation runs the identity mappings against the unmodified Flyway schema; MockMvc tests exercise the real security filter chain and database. `AccountIT` covers registration, sessions, replay/concurrency, recovery, profiles, consent, admin controls and deletion. `AuthPolicyTest` covers rate budgets and ownership denial. Production requires the three role rows from P15, a random `JWT_SECRET` of at least 32 UTF-8 bytes, explicit `CORS_ORIGINS`, and SMTP configuration.

The security configuration now belongs to `auth/infrastructure`, so the shared `common` package remains independent of feature code. `UserAccountOperations` is the API used by the user HTTP adapter. Entities, hashes and tokens are never returned as user DTOs.

## HTTP contract

All paths below start with `/api/v1`. Request bodies and responses use camelCase. Registration returns a public user and does **not** log in. This clarifies the earlier contract proposal's combined “login/register: tokens and user” row.

| Method/path | Body / result |
|---|---|
| POST `/auth/register` | `{role: CLIENT|BUSINESS,email,password,firstName,lastName,phone?,businessName?,latitude?,longitude?}` → 201 `UserView` |
| POST `/auth/login` | `{email,password,deviceId?}` → 200 `{accessToken,refreshToken,expiresIn:900,tokenType:Bearer,user}` |
| POST `/auth/refresh` | `{refreshToken}` → new token pair; prior refresh token becomes unusable |
| POST `/auth/logout` | `{refreshToken}` → 204, idempotent, no access token required |
| POST `/auth/logout-all` | authenticated → 204 |
| GET `/auth/sessions` | authenticated → own active session metadata; no token hashes or IPs |
| DELETE `/auth/sessions/{id}` | authenticated owner → 204; another user's session → 404 |
| POST `/auth/password-reset/request` | `{email}` → generic 202 for eligible and unknown addresses |
| POST `/auth/password-reset/confirm` | `{token,newPassword}` → 204 |
| POST `/auth/email-verification/request` | `{email}` → generic 202 |
| POST `/auth/email-verification/confirm` | `{token}` → 204 |
| GET `/users/me` | current `UserView` |
| PATCH `/users/me` | partial profile fields listed below → updated `UserView` |
| POST `/users/me/password` | `{currentPassword,newPassword}` → 204 |
| PUT `/users/me/location` | `{latitude,longitude,accuracyMeters?}` → 204; consent required |
| DELETE `/users/me/location` | 204 |
| DELETE `/users/me` | `{password}` → generic 202, soft deletion, one deletion event |
| GET `/admin/users` | ADMIN; exact normalized `email`, `status`, `role` filters; `page=0`, `size=20` (1–100) → `PageResponse<UserView>` |
| POST `/admin/users/{id}/lock`, `/unlock`, `/suspend`, `/reactivate` | ADMIN → updated `UserView` |
| POST `/admin/users/{id}/roles` | ADMIN; `{add:[ROLE_*],remove:[ROLE_*]}` → updated `UserView` |

`UserView`: id, email, firstName, lastName, phone, avatarUrl, preferredLocale, status, emailVerified, roles, locationSharingEnabled, marketingOptIn, memberSince.

PATCH accepts only firstName, lastName, phone, avatarUrl, preferredLocale, locationSharingEnabled and marketingOptIn. Null may clear phone/avatarUrl; names and locale cannot be null. Unknown fields, including id/email/roles/password, are rejected. No `/users/{id}` profile route exists. Both clients and business owners can manage their own profiles.

Passwords require at least 10 characters, an ASCII letter and a number, and at most 72 UTF-8 bytes (BCrypt's input limit). Hash cost is 12. Email is normalized to lowercase. Duplicate registration returns 409 CONFLICT with an email field error. Coordinates must be supplied together for business registration; they do not imply customer location consent.

## Security and lifecycle

JWTs are HS256 and require issuer `wasteless`, UUID subject, roles, jti, issued-at and expiry. Access tokens live 15 minutes, opaque 256-bit refresh tokens live 30 days, and only SHA-256 token hashes are persisted. Refresh changes serialize on the user row. The existing replacement chain identifies a device's token family; replay revokes that family without logging out unrelated devices. Failed-login counters and replay revocations commit even though the request returns 401.

Five failed logins lock an account for 15 minutes; a successful login after the window clears the counter. Administrative locks are indefinite until unlock. Every bearer request checks the current database status, so lock, suspension and deletion invalidate existing access immediately. Role claims change on the next refresh. Ordinary logout and password reset/change revoke refresh sessions; already-issued access tokens expire normally. Repeated deletion accepts the same unexpired access token only for DELETE `/users/me`, still requires the password, and cannot be used to read a deleted profile or place orders.

Password-reset tokens live 30 minutes; verification tokens live 24 hours. Both are single-use. New requests invalidate older unused tokens of the same kind. Password reset/change invalidate outstanding reset tokens and revoke all refresh sessions. Disabling location consent deletes the location in the same transaction. The `UserLocationAccess.currentLocation()` API only exposes the authenticated CLIENT's consented coordinates to discovery code.

Public GET routes cover categories, offer discovery/detail, business detail and health. `/businesses/me` is checked before public business detail. Business management routes require BUSINESS; orders and impact require CLIENT; admin routes require ADMIN. Shared `OwnershipGuard` returns 404 for another account's resources. Feature services must invoke it or filter queries by the authenticated owner. CORS uses explicit origins, stateless sessions, no cookies, no CSRF token, standard HSTS/anti-sniffing/frame-denial headers, and RFC 9457 error responses.

Bucket4j budgets: 100 authentication POSTs per source IP per 15 minutes; 20 login attempts per normalized account per 15 minutes; 5 registration/reset/verification requests per account per 15 minutes. The first 1–2 retries are allowed. Both filters and controllers return 429 RATE_LIMITED with Retry-After. Counters are bounded to 100,000 keys and live in this process; multiple replicas require shared counters or an equivalent gateway budget. Forwarded headers are not trusted by this implementation; configure a trusted proxy before deploying behind one.

Administrative role changes and account disable operations serialize using a PostgreSQL transaction advisory lock, protecting the last active administrator even across concurrent requests. Successful actions write actor, action, subject and timestamp to `security.audit` after commit. Route this logger to the deployment's durable audit sink. No password, raw token, email or coordinate is logged.

## Email and teammate integration

Production email uses `MailerPort` with SMTP_HOST, SMTP_PORT (587), SMTP_USERNAME, SMTP_PASSWORD and MAIL_FROM; STARTTLS is required and timeouts are bounded. The dev/test logger adapter announces skipped delivery without writing recovery secrets. Automated tests replace MailerPort and verify delivered tokens. To exercise recovery manually, configure a real SMTP adapter/profile; no unauthenticated development mailbox endpoint is exposed.

Events are published **after the account transaction commits**. A consumer writing data must use a new transaction (for example an `@EventListener` method calling a `REQUIRES_NEW` service). Consumers must be idempotent by userId. These are in-process events, consistent with the shared foundation; no durable outbox table exists in P10. A consumer using `@TransactionalEventListener` must account for publication after commit, rather than registering for a future commit that has already happened.

| Ticket | Implementation / remaining integration |
|---|---|
| WAS-15 | Filter chain, JWT validation, method security, PasswordEncoder and AuthenticationManager implemented. |
| WAS-16 | Identity/token/location entities and repositories map existing P10 tables; auditing uses the shared Clock and users have optimistic versions. No migrations modified. |
| WAS-17 | Registration and one committed business event tested. **B4 must consume `UserRegisteredEvent(userId,occurredAt,businessName,latitude,longitude)` and provision the business profile.** The old two-argument event constructor is retained. |
| WAS-18 | Login, JWT, rotating hashed refresh tokens, lockout and replay/concurrency tests implemented. |
| WAS-19 | Session lifecycle and owner-only session metadata/revocation implemented. P18 may use repository expiry cleanup methods. Keep expired ancestors until their replacement descendants expire to preserve replay-family history. |
| WAS-20 | Reset/change/verification implemented and tested, with a production SMTP adapter. |
| WAS-21 | Self-profile GET/PATCH implemented and tested. |
| WAS-22 | Consent, storage, withdrawal and own-location access API implemented. **B10 must call UserLocationAccess when discovery coordinates are omitted.** |
| WAS-23 | Per-IP/account throttles, generic credential errors, lockout, explicit CORS and tests implemented. |
| WAS-24 | MockMvc integration tests extend the existing P19 PostGIS foundation and run in the existing CI verify phase. |
| WAS-25 | Role boundaries, 404 ownership guard and denial tests implemented. **Real cross-business offer/location/order/dashboard tests need B/C feature handlers merged into the integration baseline. Main currently contains no such handlers.** |
| WAS-26 | Admin filters/pagination/status/roles, immediate suspension, last-admin protection and audit tests implemented. |
| WAS-27 | Password-confirmed idempotent soft deletion, session revocation, one deletion event and preservation of order/payment references tested. **P18 must consume AccountDeletionRequestedEvent for retained-data anonymization.** |

Production reference roles must be installed by the platform seed process; demo users must remain dev-only. An administrator must be provisioned through the team's secure bootstrap process, never through public registration. No frontend pages, teammate migrations, default branch or deployment are changed by this branch.

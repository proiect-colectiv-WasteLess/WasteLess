# WasteLess backend

Order, payment-mock and fulfilment backend for WAS-42–WAS-55, based on the
shared platform foundation at commit 20ae32f. Java 21, Spring Boot 4.1.2,
Spring Security, JPA, Flyway and PostgreSQL with PostGIS. Maven Wrapper retained.

Implemented: order snapshots/state machine, deterministic payment port,
atomic placement, durable idempotency, customer/business history, cancellation,
full refunds with bounded retry/reconciliation, pickup validation with rate limiting,
audit and after-commit events, customer impact and business dashboard aggregates.
Payment mode is a local mock: no real card charging, payouts or refund network.

## Project integration boundaries

These branches build on rari-platform-foundation, not the older backend prototype:
the shared P13 schema uses orders/payments/refunds/idempotency_keys.
Auth/JWT handlers and identity provisioning belong to team A; catalog CRUD and
OfferStockPort belong to B. CurrentUser expects an authenticated UUID principal
and ROLE_CLIENT/ROLE_BUSINESS. The foundation does not yet provide A's JWT filter:
a protected real HTTP request without that integration returns 401.
Tests exercise the real security boundary with authenticated test principals;
they do not disable security or claim to validate A's absent JWT implementation.

Until B6 is available, a conditional CatalogStockAdapter implements the existing
reserve/release port against the shared catalog schema, including availability
and database locks. A B6 bean replaces it. No colleague branch is modified, and
no Flutter integration or files are changed.

## Run

Requires JDK 21 and PostgreSQL with PostGIS/citext/pgcrypto/pg_trgm available.
At repository root the platform's docker-compose.yml provides a local environment.
Set PAYMENT_MODE=mock explicitly when using that Compose environment (its root
template may still default to stub). Root configuration files were not modified.

For host execution, export these environment variables and run from backend/:

    .\mvnw.cmd spring-boot:run

Linux/macOS: ./mvnw spring-boot:run.

| Variable | Purpose |
|---|---|
| DB_URL | JDBC URL, e.g. jdbc:postgresql://localhost:5432/wasteless |
| DB_USER | Database username |
| DB_PASSWORD | Database password, no real value in Git |
| JWT_SECRET | Secret consumed by platform/auth configuration |
| SPRING_PROFILES_ACTIVE | dev for local, prod for deployment |
| PAYMENT_MODE | mock for the delivered deterministic provider |
| CORS_ORIGINS | Exact comma-separated browser origin allowlist |

The dev profile defaults payment mode to mock, enables Swagger, and applies the
platform's opt-in demo data. Prod requires explicit database/JWT configuration
and disables docs. Flyway owns schema creation; Hibernate validates it.
V6 changes pickup codes to four digits, as requested. It aborts on ambiguous
active legacy code suffixes instead of silently reassigning issued codes.

## API

All paths are under /api/v1, JSON, authenticated. Idempotency-Key (1..100
characters) is mandatory on order creation and cancellation.

| Method | Path | Access |
|---|---|---|
| POST | /orders | CLIENT |
| GET | /orders?status=ACTIVE&page=0&size=20 | CLIENT, own orders |
| GET | /orders/{reference} | CLIENT owner; others 404 |
| POST | /orders/{reference}/cancel | CLIENT owner |
| GET | /businesses/me/orders?status=ACTIVE&page=0&size=20 | BUSINESS, owned business only |
| POST | /businesses/me/orders/{reference}/validate | BUSINESS owner |
| POST | /businesses/me/orders/{reference}/validate-pickup | Alias for platform contract |
| GET | /users/me/impact | CLIENT |
| GET | /businesses/me/dashboard?from=2026-11-16&to=2026-11-16 | BUSINESS |

Create JSON:

    {"offerId":"<UUID>","quantity":2,"paymentMethod":"CARD"}

Payment methods: CARD, APPLE_PAY, GOOGLE_PAY. Unit prices, total, identity,
business and snapshots are resolved on the server, never trusted from clients.
EUR only. Order response id is a WL-00001-style reference; internal UUIDs remain
private implementation identifiers. Existing Flutter fields are retained.

Cancellation JSON:

    {"reason":"Plans changed"}

Pickup JSON:

    {"code":"1234"}

Example shell requests, with an access token provided by team A's auth integration:

    curl -X POST http://localhost:8080/api/v1/orders \
      -H "Authorization: Bearer $CLIENT_TOKEN" \
      -H "Idempotency-Key: example-order-1" -H "Content-Type: application/json" \
      -d '{"offerId":"<UUID>","quantity":2,"paymentMethod":"CARD"}'
    curl http://localhost:8080/api/v1/orders \
      -H "Authorization: Bearer $CLIENT_TOKEN"
    curl -X POST http://localhost:8080/api/v1/orders/WL-00001/cancel \
      -H "Authorization: Bearer $CLIENT_TOKEN" \
      -H "Idempotency-Key: example-cancel-1" -H "Content-Type: application/json" \
      -d '{"reason":"Plans changed"}'

See docs/orders.md for response compatibility, refund failure policy, date ranges,
retry/grace/limiter configuration, concurrency semantics and ticket-by-ticket scope.
Errors use application/problem+json with the shared safe code/message/traceId format.
Lists have content/page/size/totalElements/totalPages; size is bounded to 100.

## Test and package

    .\mvnw.cmd clean verify

The default integration backend is Testcontainers postgis/postgis:16-3.4 (Docker
required). WireMock verifies the payment-port HTTP contract. P19 supplies a fixed
Clock. No DB/concurrency tests use H2, mocks of repositories or silent skips.

Without Docker, export TEST_DB_URL, TEST_DB_USERNAME and TEST_DB_PASSWORD for an
isolated PostgreSQL+PostGIS database, then run the same command. This database
will receive fixtures and temporary test schemas; never point at business data.
The Mockito test agent is preloaded for reliable JDK 21 execution on Windows.
Formatting (Spotless), unit/architecture tests, integration tests and JaCoCo run
through the existing Maven lifecycle.

Each ticket has a branch and matching short commit name:
was-42-order-model, was-43-payment-gateway, was-44-order-placement,
was-45-order-idempotency, was-46-customer-orders, was-47-order-cancellation,
was-48-refund-retries, was-49-business-orders, was-50-pickup-validation,
was-51-order-audit, was-52-customer-impact, was-53-business-dashboard,
was-54-order-integration-tests, was-55-order-concurrency.
Feature branches are cumulative in dependency order; the last includes the complete
C flow. Small follow-up commits keep earlier branches' schema checks compatible
without rewriting published history. main is not merged or pushed.

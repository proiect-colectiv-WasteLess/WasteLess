# WasteLess backend foundation

Java 21 LTS, Spring Boot 4.1.2, PostgreSQL 16 + PostGIS 3.4. Maven Wrapper is retained. Feature packages: auth, user, business, offer, order, payment, media; expose feature contracts through api and keep domain/infrastructure internal. common never depends on features (ArchUnit).

## Local start

At repository root, copy `.env.example` to `.env` and replace local credentials/JWT secret. Run `docker compose up -d --build`. Check `docker compose ps`, `docker compose logs -f backend` and http://localhost:8080/actuator/health/readiness. Database is healthy before backend starts; Flyway applies automatically. Only loopback ports are published. Run `docker compose down` to stop; `docker compose down -v` permanently resets local data.

To run on the host instead: `docker compose up -d db`, set DB_URL/DB_USER/DB_PASSWORD in the process environment, then from backend run `./mvnw spring-boot:run` (Windows: `.\mvnw.cmd spring-boot:run`). Maven does not automatically load root .env; Compose does. Default profile dev uses local database defaults. Startup automatically applies migrations; JPA validates mappings and never creates schema.

Profiles dev/test/prod: dev enables Swagger at /swagger-ui.html and /v3/api-docs; test is for isolated containers; prod requires explicit DB_URL, DB_USER, DB_PASSWORD, JWT_SECRET and disables docs. Set SPRING_PROFILES_ACTIVE=prod explicitly on deployment. CORS_ORIGINS is a comma-separated exact allowlist. STORAGE_PATH and PAYMENT_MODE are configuration hooks for feature owners. UTC Clock and JPA auditing are shared beans. ISO Instant values use the Boot Jackson defaults; money is BigDecimal.

## Verification

`./mvnw verify` runs formatting, unit/architecture tests and real PostGIS integration tests. Docker is required; failures are not silently skipped. `./mvnw verify -DskipITs` runs the subset without Docker. `./mvnw spotless:apply` formats Java. CI runs full verification and builds an image on PRs; pushes a SHA-tagged GHCR image on main using GITHUB_TOKEN. Configure repository required check `Backend / verify` and package write access before first publish. No production deployment is performed by this workflow.

See [database contract](docs/database.md) and [API proposal](docs/api-contract.md). Authentication/features remain assigned to A/B/C; this branch provides infrastructure and schema, not completed business endpoints. Contract review/signoff, feature adapter tests, coverage thresholds and load benchmarks remain follow-ups. See [test support](docs/testing.md) and [development demo data](docs/demo-data.md). No task should be marked Done solely because this branch exists.

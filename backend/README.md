# WasteLess backend

Minimal Spring Boot application using Java 17 and Maven Wrapper.
Only the application entry point and Spring Web are initialized.
No database connection or feature modules are required to start it.

## Run

Requires JDK 17 or newer. Maven is downloaded automatically on the first run,
so an internet connection is needed initially.

From this directory on Windows:

```powershell
.\mvnw.cmd spring-boot:run
```

On Linux or macOS:

```sh
./mvnw spring-boot:run
```

The server listens on port 8080 by default. No API endpoints are defined yet,
so a request to `/` returns HTTP 404.

## Verify and package

```powershell
.\mvnw.cmd verify
```

This runs the generated application-context smoke test and creates
`target/backend-0.0.1-SNAPSHOT.jar`.

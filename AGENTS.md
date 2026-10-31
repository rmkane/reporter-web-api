# Agent rules

- Java 21, Spring Boot 4.1.1. Use Jackson 3 `JsonMapper`. Do not construct `com.fasterxml.jackson.databind.ObjectMapper`.
- Keep the layering web, service, manager, repository. Do not catch exceptions in those layers. Handle them in one `@RestControllerAdvice`.
- Store reports in memory and seed them from the classpath JSON file. Do not add a database.
- Create and update bodies are partial request records with Jakarta validation. A null field on update leaves the stored value.
- Format Java with Spotless and Google Java Format (`make format`, `make lint`, or `.\make.ps1 format` and `.\make.ps1 lint`). `org.acme` imports are the last group. Import classes. Do not use fully qualified names.
- Integration tests live in `src/integration/test/java` and use `@Tag("integration")`. Surefire skips that tag unless `mvn test -Pintegration` or `make integration`.
- Call the API from tests with `RestClient`. `AbstractReportApiTest` in `src/test/java` holds the port, HTTP helpers, URL logging, `saveJson`, and `OUTPUT_PATH` (`target/integration`). `ReportApiIntegrationTest` extends it and starts its own server. `LocalhostReportIntegrationTest` in `src/integration/test/java` extends it, checks that the server accepts a connection, and then calls that server. Host, port, and path come from `reporter.integration.host`, `reporter.integration.port`, and `reporter.integration.path`, or `REPORTER_INTEGRATION_HOST`, `REPORTER_INTEGRATION_PORT`, and `REPORTER_INTEGRATION_PATH`. The default is `http://localhost:8080`.
- The reports page keeps the create form collapsed. Mark required fields. Open Swagger and OpenAPI links in a new tab.
- The startup log is a ready line, a blank line, then a bulleted list of URLs.

# Agent rules

## Stack

- Java 21 (`.java-version` pins it for jenv), Spring Boot 4.1.2.
- Use Jackson 3 `JsonMapper`. Do not construct `com.fasterxml.jackson.databind.ObjectMapper`.

## Architecture

- Keep the layering web, service, manager, repository. Do not catch exceptions in those layers. Handle them in one `@RestControllerAdvice`.
- Store reports in memory and seed them from the classpath JSON file. Do not add a database.
- Create and update bodies are partial request records with Jakarta validation. A null field on update leaves the stored value.

## UI and logging

- The reports page keeps the create form collapsed. Mark required fields. Open Swagger and OpenAPI links in a new tab.
- The startup log is a ready line, a blank line, then a bulleted list of URLs.

## Code style

- Format Java with Spotless and Google Java Format (`make format`, `make lint`, or `.\make.ps1 format` and `.\make.ps1 lint`).
- `org.acme` imports are the last group. Import classes. Do not use fully qualified names.
- Prefer Lombok to boilerplate: `@RequiredArgsConstructor` for constructor injection, `@Slf4j` for logging, and `@Getter`, `@Builder`, or `@Value` where a class needs them. Use records for plain data. Write a constructor by hand only when it does more than assign fields.
- A class of static helpers uses `@UtilityClass`, which makes it final, adds a private constructor, and makes every member static. Leave out `final` and `static` on it, and call its members through the class name, since static imports of them do not compile. Use `@NoArgsConstructor(access = AccessLevel.PRIVATE)` only on an instantiable class that needs a hidden no-argument constructor.

## Testing

- Integration tests live in `src/integration/test/java` and use `@Tag("integration")`. Surefire skips that tag unless `mvn test -Pintegration` or `make integration`.
- Call the API from tests with `RestClient`. `AbstractReportApiTest` in `src/test/java` holds the port, HTTP helpers, URL logging, `saveJson`, and `OUTPUT_PATH` (`target/integration`).
- `ReportApiIntegrationTest` extends it and starts its own server.
- `LocalhostReportIntegrationTest` in `src/integration/test/java` extends it, checks that the server accepts a connection, and then calls that server. Host, port, and path come from `reporter.integration.host`, `reporter.integration.port`, and `reporter.integration.path`, or `REPORTER_INTEGRATION_HOST`, `REPORTER_INTEGRATION_PORT`, and `REPORTER_INTEGRATION_PATH`. The default is `http://localhost:8080`.

## Commits

- Use [Conventional Commits](https://www.conventionalcommits.org/): `type(scope): summary`. Types are `feat`, `fix`, `docs`, `style`, `refactor`, `perf`, `test`, `build`, `ci`, and `chore`. Scope is optional, for example `build(deps)` or `feat(scan)`.
- Subject at most 50 characters, imperative mood, lowercase after the colon, no trailing period.
- Blank line after the subject. Wrap the body at 72 characters. Explain what changed and why, not how.
- Mark breaking changes with `!` after the type or a `BREAKING CHANGE:` footer.
- One logical change per commit. Commit only when asked.
- No attribution in commit messages or PR descriptions: no `Co-Authored-By` trailers, no "Generated with" lines.

## Changelog

- Follow [Keep a Changelog](https://keepachangelog.com/). Group entries under `Added`, `Changed`, `Fixed`, `Removed`, and `Security`.
- Add an entry for each user-visible `feat`, `fix`, `perf`, or `build(deps)` change. Skip `style`, `test`, `ci`, and `chore`.
- Unreleased work goes under the current `-SNAPSHOT` version. On release, drop `-SNAPSHOT` from the pom and the heading, add the date, and tag `vX.Y.Z`. Do not tag snapshots.

## Dependencies and security

- Pin CVE patches for Spring-managed dependencies in two places:
  - Spring Boot's version property under "Patch versions", with a comment giving the Spring-managed version.
  - An explicit `<dependencyManagement>` entry that uses that property, with a comment listing the CVEs it patches. Use a BOM import for Jackson 2 and 3. List each Tomcat artifact in the dependency tree.
- Keep the pins when they match the parent. Bump them with the parent.
- The explicit entries keep the pom working if it moves to importing `spring-boot-dependencies`, where property overrides do nothing. In that setup, declare the Jackson BOMs before the Spring BOM.
- Run `make scan` to check for vulnerable dependencies.

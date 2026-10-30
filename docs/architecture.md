# Architecture

Requests move down one layer at a time: web, service, manager, repository, then the in-memory store.

- `ReportController` is the JSON API.
- `ReportViewController` renders the Thymeleaf pages.
- `ReportService` is the use-case boundary and turns a missing id into a not-found error.
- `ReportManager` creates records, merges partial updates, checks query and sort fields, and deletes.
- `ReportRepository` extends `EntityRepository`, the stand-in for a later JDBC or JPA implementation.
- `ReportRepositoryImpl` keeps reports in a map for the life of the process.

`ApiExceptionHandler` is the only place a failure becomes an HTTP response. The catalog is loaded by `ReportSeed` through Jackson 3 `JsonMapper`. See [seed data](seed-data.md) and [create and update](create-and-update.md).

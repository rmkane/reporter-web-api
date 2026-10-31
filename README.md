# Reporter Web API

In-memory report catalog with a Jira-style search page. Spring Boot 4.1.1, Java 21.

## Getting started

Java 21 and Maven are required. `make dev` runs from source with DevTools. `make run` packages the jar and starts it in the background, and `make stop` ends that process. From PowerShell, `.\make.ps1` accepts the same targets.

The app listens on <http://localhost:8080/>.

- Application: <http://localhost:8080/>
- Reports: <http://localhost:8080/reports>
- Swagger UI: <http://localhost:8080/swagger-ui/index.html>
- OpenAPI: <http://localhost:8080/v3/api-docs>

Docker, formatting, and git hooks are covered in [Setup](docs/setup.md).

## Docs

- [Setup](docs/setup.md)
- [Architecture](docs/architecture.md)
- [Search query](docs/search-query.md)
- [Pagination and sorting](docs/pagination-and-sorting.md)
- [Create and update](docs/create-and-update.md)
- [Seed data](docs/seed-data.md)
- [OpenAPI](docs/openapi.md)
- [Changelog](CHANGELOG.md)

## License

This project is licensed under the [MIT License](LICENSE).

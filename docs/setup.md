# Setup

The app is a Spring Boot process. `make run` packages the jar and starts it in the background. `make stop` ends that process. `make debug` starts the same jar with the debugger listening on port **8787**. `make dev` runs from source with DevTools restart and LiveReload. From PowerShell, `.\make.ps1` accepts the same Maven targets (`test`, `integration`, `build`, `dev`, and the rest listed by `.\make.ps1 help`).

Docker:

- `make up` builds and starts the packaged jar.
- `make dev-up` mounts the source tree, publishes ports 8080, 8787, and 35729, and runs with the `dev` profile.
- `make down` and `make dev-down` stop those stacks.

`make format` rewrites Java with Google Java Format. `make lint` checks that formatting and fails when a file would change. `make hooks` points this clone at `.githooks`, where the pre-commit hook runs that check.

Logs are written under `logs/` and rolled once a day. That directory is gitignored.

Local URLs after startup:

- <http://localhost:8080/>
- <http://localhost:8080/reports>
- <http://localhost:8080/swagger-ui.html>
- <http://localhost:8080/v3/api-docs>

See also [architecture](architecture.md), [search query](search-query.md), and [OpenAPI](openapi.md).

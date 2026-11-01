# Setup

The app is a Spring Boot process. `make run` packages the jar and starts it in the background. `make stop` ends that process. `make dev` runs from source with DevTools restart and LiveReload. `make debug` does the same with the `dev` profile and the debugger listening on port **8787**. From PowerShell, `.\make.ps1` accepts the same Maven targets (`test`, `integration`, `build`, `dev`, and the rest listed by `.\make.ps1 help`).

Docker:

- `make up` builds and starts the packaged jar. The image checks `/actuator/health`.
- `make dev-up` mounts the source tree, publishes ports 8080, 8787, and 35729, and runs with the `dev` profile.
- `make down` and `make dev-down` stop those stacks.

`make format` rewrites Java with Google Java Format. `make lint` checks that formatting and fails when a file would change. `make hooks` points this clone at `.githooks`, where the pre-commit hook runs that check.

`make scan` packages the jar and runs [scripts/scan.sh](../scripts/scan.sh) ([scripts/scan.ps1](../scripts/scan.ps1) from `.\make.ps1 scan`), which scans it with [Trivy](https://trivy.dev) and writes the findings to `target/scan/trivy.json`. It uses `trivy rootfs` because `trivy fs` skips jar files. The scan fails on HIGH and CRITICAL findings. Set `TRIVY_SEVERITY` to change that, for example `make scan TRIVY_SEVERITY=MEDIUM,HIGH,CRITICAL`.

When Trivy finishes, with or without findings, the scan converts that JSON with `trivy convert` into `trivy.csv` ([scan/csv.tpl](../scan/csv.tpl)) and `report.html` (Trivy's stock template, vendored as [scan/html.tpl](../scan/html.tpl)). It also copies [scan/index.html](../scan/index.html), a viewer with dark mode and a filter, to `target/scan/` and writes `trivy.js`, which wraps the report for that page. Open `target/scan/index.html` straight from disk. No web server is needed. To work on the page against the sample report in [scan/trivy.json](../scan/trivy.json), serve that directory with `python3 -m http.server -d scan` and open <http://localhost:8000/>.

Logs are written under `logs/` and rolled once a day. That directory is gitignored.

Local URLs after startup:

- <http://localhost:8080/>
- <http://localhost:8080/reports>
- <http://localhost:8080/swagger-ui/index.html>
- <http://localhost:8080/v3/api-docs>
- <http://localhost:8080/actuator/health>

See also [JDK with jenv](jdk.md), [architecture](architecture.md), [search query](search-query.md), and [OpenAPI](openapi.md).

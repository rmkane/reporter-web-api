# Setup

The app is a Spring Boot process. `make run` packages the jar and starts it in the background. `make stop` ends that process. `make dev` runs from source with DevTools restart and LiveReload. `make debug` does the same with the `dev` profile and the debugger listening on port **8787**. From PowerShell, `.\make.ps1` accepts the same Maven targets (`test`, `integration`, `build`, `dev`, and the rest listed by `.\make.ps1 help`).

Docker:

- `make up` builds and starts the packaged jar. The image checks `/actuator/health`.
- `make dev-up` mounts the source tree, publishes ports 8080, 8787, and 35729, and runs with the `dev` profile.
- `make down` and `make dev-down` stop those stacks.

`make format` rewrites Java with Google Java Format. `make lint` checks that formatting and fails when a file would change. `make hooks` points this clone at `.githooks`, where the pre-commit hook runs that check.

`make scan` runs both Trivy scans. The reports list every severity, and the scan fails if either finds HIGH or CRITICAL vulnerabilities, which it prints to the terminal. `make scan-jar` packages the jar and scans it, and `make scan-image` builds the compose image (`reporter-web-api:latest`) and scans it, including OS packages. Both call [scripts/scan.sh](../scripts/scan.sh) ([scripts/scan.ps1](../scripts/scan.ps1) from `.\make.ps1`). The jar scan uses `trivy rootfs` because `trivy fs` skips jar files. Set `TRIVY_FAIL_ON` to change which severities fail the scan, for example `make scan TRIVY_FAIL_ON=MEDIUM,HIGH,CRITICAL`.

Each scan writes to `target/scan/jar/` or `target/scan/image/`: `trivy.json`, `trivy.csv` ([scan/csv.tpl](../scan/csv.tpl)), `report.html` (Trivy's stock template, vendored as [scan/html.tpl](../scan/html.tpl)), and `index.html`, a viewer from [scan/index.html](../scan/index.html) with dark mode and a filter. The viewer reads `trivy.js`, which wraps the JSON, so open `index.html` straight from disk. No web server is needed. To work on the page against the sample report in [scan/trivy.json](../scan/trivy.json), serve that directory with `python3 -m http.server -d scan` and open <http://localhost:8000/>.

Logs are written under `logs/` and rolled once a day. That directory is gitignored.

Local URLs after startup:

- <http://localhost:8080/>
- <http://localhost:8080/reports>
- <http://localhost:8080/swagger-ui/index.html>
- <http://localhost:8080/v3/api-docs>
- <http://localhost:8080/actuator/health>

See also [JDK with jenv](jdk.md), [architecture](architecture.md), [search query](search-query.md), and [OpenAPI](openapi.md).

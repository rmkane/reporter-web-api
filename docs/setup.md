# Setup

The app is a Spring Boot process. `make run` packages the jar and starts it in the background. `make stop` ends that process. `make dev` runs from source with DevTools restart and LiveReload. `make debug` does the same with the `dev` profile and the debugger listening on port **8787**. From PowerShell, `.\make.ps1` accepts the same Maven targets (`test`, `integration`, `build`, `dev`, and the rest listed by `.\make.ps1 help`).

Docker:

- `make up` builds and starts the packaged jar. The image checks `/actuator/health`.
- `make dev-up` mounts the source tree, publishes ports 8080, 8787, and 35729, and runs with the `dev` profile.
- `make down` and `make dev-down` stop those stacks.

`make format` rewrites Java with Google Java Format and puts class annotations in the tier order listed in [scripts/annotation_order.json](../scripts/annotation_order.json). `make lint` checks both and fails when a file would change, or when a class uses an annotation the script does not place yet. `make hooks` points this clone at `.githooks`, where the pre-commit hook runs that check.

`make scan` runs both Trivy scans. The reports list every severity, and the scan fails if either finds HIGH or CRITICAL vulnerabilities, which it prints to the terminal. `make scan-jar` packages the jar and scans it, and `make scan-image` builds the compose image (`reporter-web-api:latest`) and scans it, including OS packages. Both call [scripts/scan.sh](../scripts/scan.sh) ([scripts/scan.ps1](../scripts/scan.ps1) from `.\make.ps1`). The jar scan uses `trivy rootfs` because `trivy fs` skips jar files. Set `TRIVY_FAIL_ON` to change which severities fail the scan, for example `make scan TRIVY_FAIL_ON=MEDIUM,HIGH,CRITICAL`.

Each scan writes to `target/scan/jar/` or `target/scan/image/`: `trivy.json`, `trivy.csv` ([scan/csv.tpl](../scan/csv.tpl)), `report.html` (Trivy's stock template, vendored as [scan/html.tpl](../scan/html.tpl)), and `index.html`, a viewer from [scan/index.html](../scan/index.html) with dark mode and a filter. The viewer reads `trivy.js`, which wraps the JSON, so open `index.html` straight from disk. No web server is needed. To work on the page, open [scan/index.html](../scan/index.html) from disk. It loads the sample report in [scan/trivy.js](../scan/trivy.js), which holds made-up findings at every severity.

The UI theme lives in `themes/`, one JSON file per theme with `light`, `dark`, and `shared` tokens. `make theme THEME=ember` regenerates `src/main/resources/static/css/theme.css` and the favicon from `themes/ember.json`, and `make themes` lists the choices. Redrawing `favicon.ico` needs Pillow (`brew install pillow` on macOS); without it only `favicon.svg` is updated. To make your own, copy a theme file, change its colors, and run `make theme THEME=<name>`. The script rejects a theme with missing or unknown tokens. [themes/README.md](../themes/README.md) shows a preview card for every theme, and `make previews` redraws them after you change one.

Logs are written under `logs/` and rolled once a day. That directory is gitignored.

Local URLs after startup:

- <http://localhost:8080/>
- <http://localhost:8080/reports>
- <http://localhost:8080/swagger-ui/index.html>
- <http://localhost:8080/v3/api-docs>
- <http://localhost:8080/actuator/health>

See also [JDK with jenv](jdk.md), [architecture](architecture.md), [search query](search-query.md), and [OpenAPI](openapi.md).

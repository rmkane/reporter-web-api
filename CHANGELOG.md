# Changelog

All notable changes to this project are documented here. The format follows [Keep a Changelog](https://keepachangelog.com/).

## 0.1.0-SNAPSHOT (unreleased)

### Added

- Initial report API with an in-memory catalog loaded from `src/main/resources/data/reports.json`.
- Paged list and search, partial create and update, and delete.
- Thymeleaf home, reports, and report pages, with a browser query parser.
- OpenAPI bean, daily rolling logs, Makefile, Docker, and GitHub Actions.
- Health metrics and caching.
- Trivy scans of the jar and the Docker image (`make scan`, `make scan-jar`, `make scan-image`), with JSON, CSV, and HTML reports in `target/scan/` and a viewer with dark mode and a filter. `TRIVY_FAIL_ON` sets which severities fail the scan.
- `.java-version` pins JDK 21 for jenv.

### Changed

- Spring Boot 4.1.2.
- The compose image is tagged `reporter-web-api:latest`.
- Make targets are grouped into General, Build, Run, Quality, and Docker.

### Security

- Pin Jackson 3 to 3.1.7 and Jackson 2 to 2.21.7, patching CVE-2026-89407, CVE-2026-89425, CVE-2026-19032, CVE-2026-68497, CVE-2026-83557, CVE-2026-91776, and CVE-2026-91777.
- Pin Tomcat to 11.0.27, patching CVE-2026-65182, CVE-2026-65637, CVE-2026-65905, and CVE-2026-68525.

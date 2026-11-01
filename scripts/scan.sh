#!/usr/bin/env bash
# Scan a jar with Trivy and write JSON, CSV, and HTML reports to target/scan.
# Exits 0 when clean, 3 when Trivy finds vulnerabilities at TRIVY_SEVERITY, and
# with Trivy's own code on any other error.
set -euo pipefail

root="$(cd "$(dirname "$0")/.." && pwd)"
cd "$root"

jar="${1:?usage: scripts/scan.sh <jar>}"
severity="${TRIVY_SEVERITY:-HIGH,CRITICAL}"
out="target/scan"

if ! command -v trivy >/dev/null 2>&1; then
    echo "trivy not found, see https://trivy.dev" >&2
    exit 1
fi

mkdir -p "$out"

status=0
trivy rootfs -q --scanners vuln --severity "$severity" --exit-code 3 -f json -o "$out/trivy.json" "$jar" || status=$?
if [ "$status" -ne 0 ] && [ "$status" -ne 3 ]; then
    exit "$status"
fi

trivy convert -q -f template -t @scan/csv.tpl -o "$out/trivy.csv" "$out/trivy.json"
trivy convert -q -f template -t @scan/html.tpl -o "$out/report.html" "$out/trivy.json"
cp scan/index.html "$out/index.html"
{ printf 'window.TRIVY_REPORT = '; cat "$out/trivy.json"; printf ';\n'; } > "$out/trivy.js"

echo "Wrote $out (trivy.json, trivy.csv, report.html, index.html)"
exit "$status"

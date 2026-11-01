#!/usr/bin/env bash
# Scan a jar or a Docker image with Trivy and write JSON, CSV, and HTML reports
# of every severity to target/scan/<kind>. Exits 0 when clean, 3 when Trivy finds
# vulnerabilities at TRIVY_FAIL_ON, and with Trivy's own code on any other error.
set -euo pipefail

root="$(cd "$(dirname "$0")/../.." && pwd)"
cd "$root"

usage="usage: scripts/scan/scan.sh jar <path> | image <ref>"
kind="${1:?$usage}"
subject="${2:?$usage}"
fail_on="${TRIVY_FAIL_ON:-HIGH,CRITICAL}"
out="target/scan/$kind"

case "$kind" in
    jar) mode=rootfs ;;
    image) mode=image ;;
    *) echo "$usage" >&2; exit 1 ;;
esac

if ! command -v trivy >/dev/null 2>&1; then
    echo "trivy not found, see https://trivy.dev" >&2
    exit 1
fi

mkdir -p "$out"

trivy "$mode" -q --scanners vuln -f json -o "$out/trivy.json" "$subject"
trivy convert -q -f template -t @scan/csv.tpl -o "$out/trivy.csv" "$out/trivy.json"
trivy convert -q -f template -t @scan/html.tpl -o "$out/report.html" "$out/trivy.json"
cp scan/index.html "$out/index.html"
{
    printf 'window.TRIVY_FAIL_ON = "%s";\n' "$fail_on"
    printf 'window.TRIVY_REPORT = '
    cat "$out/trivy.json"
    printf ';\n'
} > "$out/trivy.js"

echo "Wrote $out (trivy.json, trivy.csv, report.html, index.html)"

# Print the findings at TRIVY_FAIL_ON, if any, and fail on them.
status=0
trivy convert -q --severity "$fail_on" --exit-code 3 -f table "$out/trivy.json" || status=$?
exit "$status"

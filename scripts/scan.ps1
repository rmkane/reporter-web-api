#requires -Version 5.1
<#
.SYNOPSIS
  Scan a jar with Trivy and write JSON, CSV, and HTML reports to target/scan.
.DESCRIPTION
  Exits 0 when clean, 3 when Trivy finds vulnerabilities at TRIVY_SEVERITY, and
  with Trivy's own code on any other error.
.EXAMPLE
  .\scripts\scan.ps1 target/reporter-web-api-0.1.0-SNAPSHOT.jar
#>
param(
    [Parameter(Mandatory = $true, Position = 0)]
    [string]$Jar
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$Root = Split-Path -Parent $PSScriptRoot
Set-Location $Root

$Severity = if ($env:TRIVY_SEVERITY) { $env:TRIVY_SEVERITY } else { 'HIGH,CRITICAL' }
$Out = Join-Path $Root 'target/scan'
$Report = Join-Path $Out 'trivy.json'

if (-not (Get-Command trivy -ErrorAction SilentlyContinue)) {
    Write-Host 'trivy not found, see https://trivy.dev' -ForegroundColor Yellow
    exit 1
}

New-Item -ItemType Directory -Force -Path $Out | Out-Null

& trivy rootfs -q --scanners vuln --severity $Severity --exit-code 3 -f json -o $Report $Jar
$status = $LASTEXITCODE
if ($status -ne 0 -and $status -ne 3) {
    exit $status
}

foreach ($convert in @(@('@scan/csv.tpl', 'trivy.csv'), @('@scan/html.tpl', 'report.html'))) {
    & trivy convert -q -f template -t $convert[0] -o (Join-Path $Out $convert[1]) $Report
    if ($LASTEXITCODE -ne 0) {
        exit $LASTEXITCODE
    }
}
Copy-Item -Path (Join-Path $Root 'scan/index.html') -Destination $Out -Force
$json = Get-Content -Path $Report -Raw
Set-Content -Path (Join-Path $Out 'trivy.js') -Value "window.TRIVY_REPORT = $json;" -Encoding UTF8

Write-Host "Wrote $Out (trivy.json, trivy.csv, report.html, index.html)" -ForegroundColor Green
exit $status

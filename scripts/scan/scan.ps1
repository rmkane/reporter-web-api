#requires -Version 5.1
<#
.SYNOPSIS
  Scan a jar or a Docker image with Trivy and write JSON, CSV, and HTML reports
  of every severity to target/scan/<kind>.
.DESCRIPTION
  Exits 0 when clean, 3 when Trivy finds vulnerabilities at TRIVY_FAIL_ON, and
  with Trivy's own code on any other error.
.EXAMPLE
  .\scripts\scan\scan.ps1 jar target/reporter-web-api-0.1.0-SNAPSHOT.jar
  .\scripts\scan\scan.ps1 image reporter-web-api:latest
#>
param(
    [Parameter(Mandatory = $true, Position = 0)]
    [ValidateSet('jar', 'image')]
    [string]$Kind,
    [Parameter(Mandatory = $true, Position = 1)]
    [string]$Subject
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$Root = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
Set-Location $Root

$Mode = if ($Kind -eq 'jar') { 'rootfs' } else { 'image' }
$FailOn = if ($env:TRIVY_FAIL_ON) { $env:TRIVY_FAIL_ON } else { 'HIGH,CRITICAL' }
$Out = Join-Path $Root "target/scan/$Kind"
$Report = Join-Path $Out 'trivy.json'

if (-not (Get-Command trivy -ErrorAction SilentlyContinue)) {
    Write-Host 'trivy not found, see https://trivy.dev' -ForegroundColor Yellow
    exit 1
}

New-Item -ItemType Directory -Force -Path $Out | Out-Null

& trivy $Mode -q --scanners vuln -f json -o $Report $Subject
if ($LASTEXITCODE -ne 0) {
    exit $LASTEXITCODE
}

foreach ($convert in @(@('@scan/csv.tpl', 'trivy.csv'), @('@scan/html.tpl', 'report.html'))) {
    & trivy convert -q -f template -t $convert[0] -o (Join-Path $Out $convert[1]) $Report
    if ($LASTEXITCODE -ne 0) {
        exit $LASTEXITCODE
    }
}
Copy-Item -Path (Join-Path $Root 'scan/index.html') -Destination $Out -Force
$json = Get-Content -Path $Report -Raw
Set-Content -Path (Join-Path $Out 'trivy.js') -Value "window.TRIVY_FAIL_ON = `"$FailOn`";`nwindow.TRIVY_REPORT = $json;" -Encoding UTF8

Write-Host "Wrote $Out (trivy.json, trivy.csv, report.html, index.html)" -ForegroundColor Green

# Print the findings at TRIVY_FAIL_ON, if any, and fail on them.
& trivy convert -q --severity $FailOn --exit-code 3 -f table $Report
exit $LASTEXITCODE

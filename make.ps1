#requires -Version 5.1
<#
.SYNOPSIS
  PowerShell stand-in for the Maven targets in the Makefile.
.EXAMPLE
  .\make.ps1 test
  .\make.ps1 integration
#>
param(
    [Parameter(Position = 0)]
    [string]$Target = 'help'
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Continue'
Set-Location $PSScriptRoot

$Root = $PSScriptRoot
$Jar = Join-Path $Root 'target/reporter-web-api-0.1.0-SNAPSHOT.jar'
$Image = 'reporter-web-api:latest'
$PidFile = Join-Path $Root '.reporter-web-api.pid'
$DebugOpts = '-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:8787'

function Invoke-Maven {
    param([Parameter(Mandatory = $true)][string[]]$MavenArgs)
    & mvn -B @MavenArgs
    if ($LASTEXITCODE -ne 0) {
        exit $LASTEXITCODE
    }
}

function Invoke-Python {
    param([Parameter(Mandatory = $true)][string[]]$PythonArgs)
    $python = if (Get-Command python3 -ErrorAction SilentlyContinue) { 'python3' } else { 'python' }
    & $python @PythonArgs
    if ($LASTEXITCODE -ne 0) {
        exit $LASTEXITCODE
    }
}

function Stop-Quiet {
    if (-not (Test-Path $PidFile)) {
        return
    }
    $appPid = (Get-Content -Path $PidFile -Raw).Trim()
    if ($appPid) {
        Stop-Process -Id $appPid -Force -ErrorAction SilentlyContinue
    }
    Remove-Item -Path $PidFile -Force -ErrorAction SilentlyContinue
}

function Start-Jar {
    param([string[]]$JavaArgs)
    New-Item -ItemType Directory -Force -Path (Join-Path $Root 'logs') | Out-Null
    $proc = Start-Process -FilePath 'java' `
        -ArgumentList $JavaArgs `
        -WorkingDirectory $Root `
        -WindowStyle Hidden `
        -PassThru `
        -RedirectStandardOutput (Join-Path $Root 'logs/console.out') `
        -RedirectStandardError (Join-Path $Root 'logs/console.err')
    Set-Content -Path $PidFile -Value $proc.Id -NoNewline
    return $proc
}

function Show-Help {
    Write-Host ''
    Write-Host 'Usage' -ForegroundColor White
    Write-Host '  .\make.ps1 ' -NoNewline
    Write-Host '<target>' -ForegroundColor Cyan
    Write-Host ''
    $groups = [ordered]@{
        'General' = @(
            @{ Name = 'help'; Help = 'Show available commands' }
        )
        'Build'   = @(
            @{ Name = 'install'; Help = 'Resolve Maven dependencies' },
            @{ Name = 'build'; Help = 'Compile, test, and package the jar' },
            @{ Name = 'package'; Help = 'Package the jar without tests' },
            @{ Name = 'clean'; Help = 'Remove build output' },
            @{ Name = 'theme'; Help = 'Generate theme.css and the favicon from themes/$env:THEME.json (default ember)' },
            @{ Name = 'themes'; Help = 'List the available UI themes' },
            @{ Name = 'previews'; Help = 'Draw a preview card for every theme in themes/previews' }
        )
        'Run'     = @(
            @{ Name = 'run'; Help = 'Start the packaged jar in the background' },
            @{ Name = 'stop'; Help = 'Stop the packaged jar' },
            @{ Name = 'dev'; Help = 'Run from source with DevTools restart and LiveReload' },
            @{ Name = 'debug'; Help = 'Run from source with the dev profile and JDWP on port 8787' }
        )
        'Quality' = @(
            @{ Name = 'test'; Help = 'Run the unit tests' },
            @{ Name = 'integration'; Help = 'Run the integration tests' },
            @{ Name = 'lint'; Help = 'Check Java formatting and class annotation order' },
            @{ Name = 'format'; Help = 'Format Java and fix class annotation order' },
            @{ Name = 'scan'; Help = 'Scan the jar and the Docker image with Trivy' },
            @{ Name = 'scan-jar'; Help = 'Scan the jar and write reports to target/scan/jar' },
            @{ Name = 'scan-image'; Help = 'Scan the Docker image and write reports to target/scan/image' }
        )
    }
    foreach ($group in $groups.Keys) {
        Write-Host $group -ForegroundColor White
        $groups[$group] | ForEach-Object {
            Write-Host ('  {0,-12} {1}' -f $_.Name, $_.Help) -ForegroundColor Green
        }
        Write-Host ''
    }
}

switch ($Target) {
    'help' { Show-Help }
    'install' { Invoke-Maven @('-DskipTests', 'dependency:go-offline') }
    'build' { Invoke-Maven @('package') }
    'package' { Invoke-Maven @('-DskipTests', 'package') }
    'clean' {
        Invoke-Maven @('clean')
        Stop-Quiet
    }
    'theme' {
        $theme = if ($env:THEME) { $env:THEME } else { 'ember' }
        Invoke-Python @('scripts/theme/generate_css.py', $theme)
        Invoke-Python @('scripts/theme/generate_icons.py', '--theme', $theme)
    }
    'themes' { Invoke-Python @('scripts/theme/generate_css.py', '--list') }
    'previews' { Invoke-Python @('scripts/theme/generate_previews.py') }
    'run' {
        Invoke-Maven @('-DskipTests', 'package')
        Stop-Quiet
        $proc = Start-Jar @('-jar', $Jar)
        Write-Host "Started $Jar (pid $($proc.Id))" -ForegroundColor Green
    }
    'stop' {
        if (Test-Path $PidFile) {
            $appPid = (Get-Content -Path $PidFile -Raw).Trim()
            Stop-Quiet
            Write-Host "Stopped pid $appPid" -ForegroundColor Yellow
        }
        else {
            Write-Host 'Not running' -ForegroundColor Yellow
        }
    }
    'debug' {
        Invoke-Maven @(
            'spring-boot:run',
            '-Dspring-boot.run.profiles=dev',
            "-Dspring-boot.run.jvmArguments=$DebugOpts"
        )
    }
    'dev' { Invoke-Maven @('spring-boot:run', '-Dspring-boot.run.profiles=dev') }
    'test' { Invoke-Maven @('test') }
    'integration' { Invoke-Maven @('test', '-Pintegration') }
    'lint' {
        Invoke-Maven @('spotless:check')
        Invoke-Python @('scripts/lint/annotation_order.py', '--check')
    }
    'format' {
        Invoke-Maven @('spotless:apply')
        Invoke-Python @('scripts/lint/annotation_order.py', '--fix')
    }
    'scan-jar' {
        Invoke-Maven @('-DskipTests', 'package')
        & (Join-Path $Root 'scripts/scan/scan.ps1') jar $Jar
        exit $LASTEXITCODE
    }
    'scan-image' {
        & docker compose build
        if ($LASTEXITCODE -ne 0) {
            exit $LASTEXITCODE
        }
        & (Join-Path $Root 'scripts/scan/scan.ps1') image $Image
        exit $LASTEXITCODE
    }
    'scan' {
        Invoke-Maven @('-DskipTests', 'package')
        & (Join-Path $Root 'scripts/scan/scan.ps1') jar $Jar
        $status = $LASTEXITCODE
        & docker compose build
        if ($LASTEXITCODE -ne 0) {
            exit $LASTEXITCODE
        }
        & (Join-Path $Root 'scripts/scan/scan.ps1') image $Image
        if ($LASTEXITCODE -ne 0) {
            $status = $LASTEXITCODE
        }
        exit $status
    }
    default {
        Write-Host "Unknown target: $Target" -ForegroundColor Yellow
        Show-Help
        exit 1
    }
}

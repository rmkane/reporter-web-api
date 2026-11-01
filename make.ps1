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
$PidFile = Join-Path $Root '.reporter-web-api.pid'
$DebugOpts = '-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:8787'

function Invoke-Maven {
    param([Parameter(Mandatory = $true)][string[]]$MavenArgs)
    & mvn -B @MavenArgs
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
            @{ Name = 'clean'; Help = 'Remove build output' }
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
            @{ Name = 'lint'; Help = 'Check Java formatting' },
            @{ Name = 'format'; Help = 'Format Java with Google Java Format' },
            @{ Name = 'scan'; Help = 'Scan the jar with Trivy and write JSON, CSV, and HTML reports' }
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
    'lint' { Invoke-Maven @('spotless:check') }
    'format' { Invoke-Maven @('spotless:apply') }
    'scan' {
        Invoke-Maven @('-DskipTests', 'package')
        & (Join-Path $Root 'scripts/scan.ps1') $Jar
        exit $LASTEXITCODE
    }
    default {
        Write-Host "Unknown target: $Target" -ForegroundColor Yellow
        Show-Help
        exit 1
    }
}

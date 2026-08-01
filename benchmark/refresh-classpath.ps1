<#
.SYNOPSIS
    Recompile and refresh build/bench/classpath.txt for the benchmark launcher.

.DESCRIPTION
    Run after any source change. Uses --offline; the first ever invocation on a
    machine must be online once so that runtime-only dependencies (DevAuth,
    log4j 2.8.1, jline, night-config) land in the Gradle cache — `gradlew build`
    alone only resolves the compile classpath and leaves them missing.
#>
[CmdletBinding()]
param([switch] $Online)

$ErrorActionPreference = 'Stop'
$edge = Split-Path -Parent $PSScriptRoot

$args = @('-p', $edge, '-I', (Join-Path $PSScriptRoot 'dump-classpath.gradle'))
if (-not $Online) { $args += '--offline' }
$args += @('classes', 'dumpBenchClasspath')

& (Join-Path $edge 'gradlew.bat') @args
if ($LASTEXITCODE -ne 0) { throw "Gradle failed with exit code $LASTEXITCODE" }

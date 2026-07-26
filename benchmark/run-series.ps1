<#
.SYNOPSIS
    Run an interleaved A/B benchmark series and collect the results.

.DESCRIPTION
    Alternates the variants (A, B, A, B, ...) rather than running all of A then all
    of B. On a laptop, thermal drift over a series is large enough that a blocked
    layout would charge the whole drift to whichever variant ran second.

    Each run is a fresh JVM. Per Georges et al. (OOPSLA'07), repeated measurements
    inside one VM invocation are not a substitute for repeated invocations: JIT
    state, heap layout and OS page cache all differ on a cold start.

    The first run of a series is discarded by default (cold page cache).

.PARAMETER Variants
    Ordered hashtable of variant name -> override hashtable. Use @{} for stock
    configuration. Example:
        -Variants ([ordered]@{ off = @{ 'Performance.EntitiesOptimize' = $false }
                               on  = @{ 'Performance.EntitiesOptimize' = $true  } })

.PARAMETER Runs
    Measured runs per variant, after the discarded warm-up run.
#>
[CmdletBinding()]
param(
    [Parameter(Mandatory)] [string] $Scenario,
    [Parameter(Mandatory)] [System.Collections.IDictionary] $Variants,
    [int]    $Runs = 3,
    [string] $Tag,
    [int]    $TimeoutSec = 420,
    [switch] $NoDiscardFirst
)

$ErrorActionPreference = 'Stop'
$edge = Split-Path -Parent $PSScriptRoot
if (-not $Tag) { $Tag = $Scenario }
$outDir = Join-Path $edge "benchmark\results\$Tag"
New-Item -ItemType Directory -Force -Path $outDir | Out-Null

$runClient = Join-Path $PSScriptRoot 'run-client.ps1'
$names = @($Variants.Keys)
$log = [System.Collections.Generic.List[object]]::new()
$consecutiveFailures = 0

# Discarded warm-up run on the first variant only: it exists to warm the OS page
# cache and the GPU clock state, not to be compared against anything.
$totalPasses = if ($NoDiscardFirst) { $Runs } else { $Runs + 1 }

for ($pass = 0; $pass -lt $totalPasses; $pass++) {
    $discard = (-not $NoDiscardFirst) -and ($pass -eq 0)
    foreach ($name in $names) {
        $result = & $runClient -Scenario $Scenario -Variant $name `
                               -Overrides $Variants[$name] -TimeoutSec $TimeoutSec
        $src = $result.ResultFile

        if ($result.Outcome -ne 'OK') {
            $log.Add([pscustomobject]@{ Pass = $pass; Variant = $name; Status = $result.Outcome; File = $null })
            Write-Warning "run failed: variant=$name pass=$pass outcome=$($result.Outcome)"
            # Fail fast. A series is 25+ minutes; grinding through it while every run
            # fails wastes the whole slot and produces nothing to compare.
            if (++$consecutiveFailures -ge 2) {
                throw "aborting series: $consecutiveFailures consecutive failed runs (last outcome $($result.Outcome))"
            }
            continue
        }
        $consecutiveFailures = 0
        if ($discard) {
            $log.Add([pscustomobject]@{ Pass = $pass; Variant = $name; Status = 'DISCARDED'; File = $null })
            continue
        }

        $dest = Join-Path $outDir ("{0}-{1}.json" -f $name, $pass)
        Copy-Item $src $dest -Force

        # Keep one set of screenshots per variant. Comparing the candidate's rendered
        # output against the baseline's, from the same series, is a stronger check than
        # comparing against a reference captured on some earlier build.
        if ($result.ShotsDir) {
            $shotDest = Join-Path $outDir "shots\$name"
            New-Item -ItemType Directory -Force -Path $shotDest | Out-Null
            Copy-Item (Join-Path $result.ShotsDir '*.png') $shotDest -Force
        }
        $summary = (Get-Content $dest -Raw | ConvertFrom-Json).summary
        $log.Add([pscustomobject]@{
            Pass = $pass; Variant = $name; Status = 'OK'; File = $dest
            Frames = $summary.frameCount; AvgFps = [math]::Round($summary.avgFps, 1)
            P50ms = [math]::Round($summary.p50FrameMs, 3); P99ms = [math]::Round($summary.p99FrameMs, 3)
        })
        Write-Host ("pass {0} {1,-10} avg={2,8:N1}fps p50={3,7:N3}ms p99={4,7:N3}ms" -f `
            $pass, $name, $summary.avgFps, $summary.p50FrameMs, $summary.p99FrameMs)
    }
}

$log | Export-Csv -Path (Join-Path $outDir 'series.csv') -NoTypeInformation
Write-Host "`nResults in $outDir"
$log

<#
.SYNOPSIS
    Pin GPU clocks and the Windows power plan for the duration of a benchmark session.

.DESCRIPTION
    Minecraft 1.8.9 is a light OpenGL workload, which makes it unusually prone to
    sitting in a low GPU power state and to being moved between states mid-run.
    NVIDIA's own benchmarking guidance notes GPC clocks oscillate for the first few
    seconds of a workload before settling. Pinning the clock removes DVFS as a
    variable and lets the warmup phase be much shorter.

    This is the opposite of the "simulate a slower device" idea: the goal is a
    constant operating point, not a lower one.

    Both changes are reversible; -Restore puts back what was recorded by -Apply.

.PARAMETER GraphicsClockMhz
    Clock to pin. Should be a value the card sustains indefinitely under load, not
    the maximum boost bin.
#>
[CmdletBinding(DefaultParameterSetName = 'Apply')]
param(
    [Parameter(ParameterSetName = 'Apply')]   [int]    $GraphicsClockMhz = 1800,
    [Parameter(ParameterSetName = 'Restore')] [switch] $Restore
)

$ErrorActionPreference = 'Stop'
$stateFile = Join-Path $PSScriptRoot 'results\clock-lock-state.json'

function Get-ActiveSchemeGuid {
    $line = powercfg /getactivescheme
    if ($line -match '([0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12})') { return $Matches[1] }
    throw "could not parse active power scheme from: $line"
}

if ($Restore) {
    if (-not (Test-Path $stateFile)) { throw "no saved state at $stateFile" }
    $state = Get-Content $stateFile -Raw | ConvertFrom-Json
    try { nvidia-smi --reset-gpu-clocks | Out-Null; Write-Host 'GPU clocks reset' }
    catch { Write-Warning "could not reset GPU clocks: $_" }
    powercfg /setactive $state.powerSchemeGuid
    Write-Host "power scheme restored to $($state.powerSchemeGuid)"
    Remove-Item $stateFile
    return
}

New-Item -ItemType Directory -Force -Path (Split-Path -Parent $stateFile) | Out-Null
$previousScheme = Get-ActiveSchemeGuid

$lockOutput = & nvidia-smi --lock-gpu-clocks="$GraphicsClockMhz,$GraphicsClockMhz" 2>&1
$lockOk = $LASTEXITCODE -eq 0
if (-not $lockOk) {
    # Clock locking needs elevation on Windows. Not fatal: the fallback is a longer
    # warmup, but the run must record that DVFS was left enabled.
    Write-Warning "GPU clock lock failed (elevation required?): $lockOutput"
}

# High performance: keeps the CPU off its low-frequency states between frames.
$highPerformance = '8c5e7fda-e8bf-4a96-9a85-a6e23a8c635c'
powercfg /setactive $highPerformance

[pscustomobject]@{
    powerSchemeGuid  = $previousScheme
    gpuClockLocked   = $lockOk
    graphicsClockMhz = $GraphicsClockMhz
} | ConvertTo-Json | Set-Content -Path $stateFile -Encoding UTF8

Write-Host "power scheme -> high performance (was $previousScheme)"
Write-Host "gpu clock lock: $(if ($lockOk) { "$GraphicsClockMhz MHz" } else { 'FAILED - DVFS still active' })"

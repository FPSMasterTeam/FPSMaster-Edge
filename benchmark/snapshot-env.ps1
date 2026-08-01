<#
.SYNOPSIS
    Record the host fingerprint that every benchmark result must be tagged with.

.DESCRIPTION
    GL_RENDERER / GL_VERSION are captured in-process by the harness; this covers
    what the JVM cannot see. Results measured under different fingerprints must
    never be compared.
#>
[CmdletBinding()]
param([string] $OutFile)

$ErrorActionPreference = 'Stop'
$edge = Split-Path -Parent $PSScriptRoot
if (-not $OutFile) { $OutFile = Join-Path $edge 'benchmark\results\host-env.json' }
New-Item -ItemType Directory -Force -Path (Split-Path -Parent $OutFile) | Out-Null

$cpu = Get-CimInstance Win32_Processor | Select-Object -First 1
$gpu = Get-CimInstance Win32_VideoController | Select-Object Name, DriverVersion

$nvClocks = $null
try { $nvClocks = (nvidia-smi --query-gpu=clocks.current.graphics,clocks.current.memory,temperature.gpu --format=csv,noheader) -join '; ' } catch { }

[pscustomobject]@{
    timestampUtc   = (Get-Date).ToUniversalTime().ToString('o')
    gitSha         = (& git -C $edge rev-parse --short HEAD)
    gitBranch      = (& git -C $edge rev-parse --abbrev-ref HEAD)
    gitDirty       = [bool](& git -C $edge status --porcelain)
    cpu            = $cpu.Name
    cpuCores       = $cpu.NumberOfCores
    cpuThreads     = $cpu.NumberOfLogicalProcessors
    ramGB          = [math]::Round((Get-CimInstance Win32_ComputerSystem).TotalPhysicalMemory / 1GB, 1)
    gpus           = @($gpu | ForEach-Object { "$($_.Name) [$($_.DriverVersion)]" })
    nvidiaClocks   = $nvClocks
    os             = (Get-CimInstance Win32_OperatingSystem).Caption
    osBuild        = [Environment]::OSVersion.Version.ToString()
    powerPlan      = (powercfg /getactivescheme)
} | ConvertTo-Json -Depth 4 | Set-Content -Path $OutFile -Encoding UTF8

Write-Host "Wrote $OutFile"

<#
.SYNOPSIS
    Price every Performance sub-feature individually on one scenario.

.DESCRIPTION
    Variants are: the module off (the user-visible baseline), the module on with every
    sub-feature off (the reference the per-feature deltas are taken against), one variant
    per sub-feature, and the shipping defaults.

    Two settings are gated on the module switch alone rather than on a sub-toggle, so they
    are neutralised in every module-on variant:

      FPSLimit       caps the frame rate to 30 while the window is not focused. An
                     unattended series that loses focus would then compare a 30 fps
                     variant against an unlimited one and read it as a catastrophic
                     regression. Pinned to 260, which is Unlimited in 1.8.9.
      ParticlesLimit caps particle emitters. Pinned to its maximum so the cap cannot fire.

    FastLoad and DownscalePackIcons are omitted: both act only during startup and resource
    pack loading, which is over before the measurement window opens.
#>
[CmdletBinding()]
param(
    [Parameter(Mandatory)] [string] $Scenario,
    [int]    $Runs = 4,
    [string] $Tag,
    [int]    $TimeoutSec = 420
)

$ErrorActionPreference = 'Stop'
if (-not $Tag) { $Tag = "featmatrix-$Scenario" }

$allOff = @{
    'Performance'                     = $true
    'Performance.IgnoreStands'        = $false
    'Performance.FastLoad'            = $false
    'Performance.StaticParticleColor' = $false
    'Performance.LimitChunks'         = $false
    'Performance.BatchModelRendering' = $false
    'Performance.LowAnimationTick'    = $false
    'Performance.DownscalePackIcons'  = $false
    'Performance.ParticleCulling'     = $false
    'Performance.CacheSkyColor'       = $false
    'Performance.EntityCulling'       = $false
    'Performance.CustomHudFont'       = $false
    'Performance.FastRender'          = $false
    'Performance.FPSLimit'            = 260
    'Performance.ParticlesLimit'      = 2000
}

function With([string] $setting) {
    $copy = $allOff.Clone()
    $copy[$setting] = $true
    return $copy
}

$variants = [ordered]@{
    off       = @{ 'Performance' = $false }
    base      = $allOff.Clone()
    stands    = With 'Performance.IgnoreStands'
    sparticle = With 'Performance.StaticParticleColor'
    limitchk  = With 'Performance.LimitChunks'
    batch     = With 'Performance.BatchModelRendering'
    lowanim   = With 'Performance.LowAnimationTick'
    pcull     = With 'Performance.ParticleCulling'
    skycache  = With 'Performance.CacheSkyColor'
    ecull     = With 'Performance.EntityCulling'
    hudfont   = With 'Performance.CustomHudFont'
    fastrend  = With 'Performance.FastRender'
    alldef    = @{ 'Performance' = $true; 'Performance.FPSLimit' = 260 }
}

& (Join-Path $PSScriptRoot 'run-series.ps1') -Scenario $Scenario -Variants $variants `
    -Runs $Runs -Tag $Tag -TimeoutSec $TimeoutSec

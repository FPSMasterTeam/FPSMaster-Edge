<#
.SYNOPSIS
    Price the parts of the per-entity pass by deleting them one at a time.

.DESCRIPTION
    The entity pass is around 70% of the frame on a recorded Hypixel match, which makes it
    the obvious target and says nothing about which part of it to attack. These probes delete
    one part each and measure what the frame gives back. None of them is shippable: they are
    there to decide whether building a real version is worth it.

    Two controls run in the same series. They are the same configuration with no probe, so
    the difference between them is the null band for this scenario, measured under the same
    conditions as the effects rather than borrowed from another scenario.

    Two probes that were built are not in this series, because counters priced their target
    at zero first: armour glint renders 0.01 models per frame on these recordings and no sign
    is drawn at all. Running them would have measured nothing at a cost of twenty minutes.
#>
[CmdletBinding()]
param(
    [Parameter(Mandatory)] [string] $Scenario,
    [int]    $Runs = 3,
    [string] $Tag,
    [int]    $TimeoutSec = 420
)

$ErrorActionPreference = 'Stop'
if (-not $Tag) { $Tag = "probes-$Scenario" }

# FPSLimit is pinned for the same reason the feature matrix pins it: it caps the frame rate
# to 30 while the window is unfocused, and an unattended series is unfocused throughout.
$defaults = @{ 'Performance' = $true; 'Performance.FPSLimit' = 260 }

$variants = [ordered]@{
    ctrl       = $defaults.Clone()
    ctrl2      = $defaults.Clone()
    noarmor    = $defaults.Clone()
    nohelditem = $defaults.Clone()
    nonames    = $defaults.Clone()
}

$experiments = @{
    noarmor    = @('noArmor')
    nohelditem = @('noHeldItem')
    nonames    = @('noNameplates')
}

& (Join-Path $PSScriptRoot 'run-series.ps1') -Scenario $Scenario -Variants $variants `
    -VariantExperiments $experiments -Runs $Runs -Tag $Tag -TimeoutSec $TimeoutSec

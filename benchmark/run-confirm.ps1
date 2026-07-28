<#
.SYNOPSIS
    Confirm the two results the feature survey left standing, on one scenario.

.DESCRIPTION
    The survey priced thirteen configurations at once, which is the right way to find what is
    worth looking at and the wrong way to decide anything: heavy intermittent contamination on
    this machine left several variants with one or two usable runs. This is the short version -
    four variants, so a pass costs four minutes rather than twelve, and every variant gets the
    same number of clean runs.

      off      no optimisations at all, the user-visible baseline
      alldef   the shipping defaults
      hudfont  the defaults plus CustomHudFont, which was the one clear win in the survey
      fastrend the defaults plus FastRender, which OptiFine ships on and this project has
               twice failed to measure a benefit from

    alldef doubles as the null reference for the other two: they differ from it by one setting
    each, so anything smaller than the alldef-to-off spread is not a result.
#>
[CmdletBinding()]
param(
    [Parameter(Mandatory)] [string] $Scenario,
    [int]    $Runs = 3,
    [string] $Tag,
    [int]    $TimeoutSec = 420
)

$ErrorActionPreference = 'Stop'
if (-not $Tag) { $Tag = "confirm-$Scenario" }

$defaults = @{ 'Performance' = $true; 'Performance.FPSLimit' = 260 }

$variants = [ordered]@{
    off      = @{ 'Performance' = $false }
    alldef   = $defaults.Clone()
    hudfont  = $defaults.Clone() + @{ 'Performance.CustomHudFont' = $true }
    fastrend = $defaults.Clone() + @{ 'Performance.FastRender' = $true }
}

& (Join-Path $PSScriptRoot 'run-series.ps1') -Scenario $Scenario -Variants $variants `
    -Runs $Runs -Tag $Tag -TimeoutSec $TimeoutSec

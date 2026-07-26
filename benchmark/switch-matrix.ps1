<#
.SYNOPSIS
    Prove that a Performance sub-feature actually reaches its code path, and that the
    module's master switch actually disables it.

.DESCRIPTION
    Frame times cannot answer this: a setting that is wired to nothing looks exactly
    like a setting whose effect is below the noise band. The counters can, so each
    variant here is checked against an expected counter, not against a timing.

    Runs the short flat-quick scenario, so a three-variant matrix costs about a minute.
#>
[CmdletBinding()]
param(
    [Parameter(Mandatory)] [string] $Setting,
    [Parameter(Mandatory)] [string] $Counter,
    [string] $Scenario = 'flat-quick',
    [int]    $TimeoutSec = 200
)

$ErrorActionPreference = 'Stop'
$runClient = Join-Path $PSScriptRoot 'run-client.ps1'

$cases = [ordered]@{
    'module-off' = @{ overrides = @{ 'Performance' = $false; "Performance.$Setting" = $true }; expect = 'zero' }
    'sub-off'    = @{ overrides = @{ 'Performance' = $true;  "Performance.$Setting" = $false }; expect = 'zero' }
    'both-on'    = @{ overrides = @{ 'Performance' = $true;  "Performance.$Setting" = $true  }; expect = 'nonzero' }
}

$failures = 0
foreach ($name in $cases.Keys) {
    $case = $cases[$name]
    $result = & $runClient -Scenario $Scenario -Variant $name -Overrides $case.overrides -TimeoutSec $TimeoutSec
    if ($result.Outcome -ne 'OK') {
        Write-Host ("{0,-12} RUN FAILED ({1})" -f $name, $result.Outcome) -ForegroundColor Red
        $failures++
        continue
    }

    $report = Get-Content $result.ResultFile -Raw | ConvertFrom-Json
    # countersTotal, not the measurement-window delta: some paths (display-list
    # compilation, resource-pack load) fire only during startup, so the windowed
    # counter reads zero for a feature that is demonstrably working.
    $value = $report.countersTotal.$Counter
    if ($null -eq $value) { throw "no counter named '$Counter' in the report" }

    $ok = if ($case.expect -eq 'zero') { $value -eq 0 } else { $value -gt 0 }
    if (-not $ok) { $failures++ }
    $status = if ($ok) { 'PASS' } else { 'FAIL' }
    $colour = if ($ok) { 'Green' } else { 'Red' }
    Write-Host ("{0,-12} {1} = {2,-12} expected {3,-8} {4}" -f $name, $Counter, $value, $case.expect, $status) -ForegroundColor $colour
}

if ($failures) { throw "$failures switch-matrix case(s) failed for $Setting" }
Write-Host "`nswitch matrix passed for Performance.$Setting" -ForegroundColor Green

$base = @{ 'Performance' = $true; 'BetterChat' = $true; 'BetterChat.BetterFont' = $false }
function Pair($name, $key) {
  $off = $base.Clone(); $off[$key] = $false
  $on  = $base.Clone(); $on[$key]  = $true
  & "$PSScriptRoot\benchmark\run-series.ps1" -Scenario replay-lobby -Tag $name -Runs 3 -TimeoutSec 260 `
      -Variants ([ordered]@{ off = $off; on = $on }) | Out-Null
}
Pair 'nv-customfont' 'Performance.CustomHudFont'
Pair 'nv-fastrender' 'Performance.FastRender'
& "$PSScriptRoot\benchmark\run-series.ps1" -Scenario replay-lobby -Tag nv-cull-lobby -Runs 3 -TimeoutSec 260 `
    -Variants ([ordered]@{ off = @{ 'Performance' = $true; 'Performance.EntityCulling' = $false }
                           on  = @{ 'Performance' = $true; 'Performance.EntityCulling' = $true } }) | Out-Null

$base = @{ 'Performance' = $true }
function Pair($name, $key) {
  $off = $base.Clone(); $off[$key] = $false
  $on  = $base.Clone(); $on[$key]  = $true
  & "$PSScriptRoot\benchmark\run-series.ps1" -Scenario replay-lobby -Tag $name -Runs 3 -TimeoutSec 260 `
      -Variants ([ordered]@{ off = $off; on = $on }) | Out-Null
}
Pair 'nv2-chunkcache' 'Performance.CacheEntityChunkLookup'
Pair 'nv2-cull'       'Performance.EntityCulling'

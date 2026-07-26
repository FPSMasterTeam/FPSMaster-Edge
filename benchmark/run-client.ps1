<#
.SYNOPSIS
    Launch one unattended Edge dev-client benchmark run.

.DESCRIPTION
    Bypasses Gradle entirely: `gradlew runClient` is broken under Gradle 8.6 +
    Loom 0.10.0.5 (RunGameTask.main fails task-property validation), and Gradle
    adds ~12s of startup per run anyway. This invokes JDK 8 on
    net.minecraft.launchwrapper.Launch directly, using the classpath dumped by
    benchmark/dump-classpath.gradle and the arguments from
    .gradle/loom-cache/launch.cfg.

    Uses a dedicated game directory (run-bench/) so the developer's own run/
    config, saves and profile are never touched.

.PARAMETER Scenario
    Scenario id written to run-bench/bench-request.json for the in-game harness
    to pick up. Omit for a plain smoke launch.

.PARAMETER TimeoutSec
    Hard kill deadline. Guards against a hung client stalling an unattended loop.

.PARAMETER KeepAlive
    Do not kill the client when it reaches the main menu; used for manual poking.
#>
[CmdletBinding()]
param(
    [string]    $Scenario,
    [string]    $Variant = 'baseline',
    [hashtable] $Overrides,
    [int]       $TimeoutSec = 420,
    [switch]    $KeepAlive
)

$ErrorActionPreference = 'Stop'

$edge     = Split-Path -Parent $PSScriptRoot
$gameDir  = Join-Path $edge 'run-bench'
$cpFile   = Join-Path $edge 'build\bench\classpath.txt'
$java     = Join-Path $env:USERPROFILE '.gradle\jdks\temurin-8-amd64-windows\jdk8u472-b08\bin\java.exe'
$loom     = Join-Path $env:USERPROFILE '.gradle\caches\essential-loom'
$natives  = Join-Path $loom '1.8.9\natives'
$assets   = Join-Path $loom 'assets'
$mcpDir   = 'de.oceanlabs.mcp.mcp_stable.1_8_9.22-1.8.9-forge-1.8.9-11.15.1.2318-1.8.9'
$srg      = Join-Path $loom "1.8.9\$mcpDir\mappings-srg-named.srg"

foreach ($required in @($java, $cpFile, $natives, $assets, $srg)) {
    if (-not (Test-Path $required)) {
        throw "Missing prerequisite: $required (run benchmark/refresh-classpath.ps1 first?)"
    }
}

# Fresh game dir every run: the client rewrites options.txt and its own config on
# shutdown, so reusing one would let settings drift across an A/B series.
# The previous run's JVM may still be releasing its redirected log handles, so retry
# rather than aborting a whole series on a transient sharing violation.
if (Test-Path $gameDir) {
    for ($attempt = 1; ; $attempt++) {
        try { Remove-Item $gameDir -Recurse -Force; break }
        catch {
            if ($attempt -ge 20) { throw }
            Start-Sleep -Milliseconds 500
        }
    }
}
New-Item -ItemType Directory -Path $gameDir | Out-Null
Copy-Item (Join-Path $PSScriptRoot 'options.benchmark.txt') (Join-Path $gameDir 'options.txt')

if ($Scenario) {
    $scenarioSrc = Join-Path $PSScriptRoot 'scenarios'
    if (-not (Test-Path (Join-Path $scenarioSrc "$Scenario.json"))) {
        throw "Unknown scenario '$Scenario' (looked in $scenarioSrc)"
    }
    Copy-Item $scenarioSrc (Join-Path $gameDir 'scenarios') -Recurse
    $request = @{ scenario = $Scenario; variant = $Variant }
    if ($Overrides) { $request.overrides = $Overrides }
    $request | ConvertTo-Json -Depth 5 |
        Set-Content -Path (Join-Path $gameDir 'bench-request.json') -Encoding UTF8
}

$cp = (Get-Content $cpFile -Raw).Trim()

$jvmArgs = @(
    # Fixed heap + pre-touch so GC behaviour does not drift between runs.
    '-Xms2G', '-Xmx2G', '-XX:+AlwaysPreTouch', '-XX:+DisableExplicitGC',
    # Keep the Windows timer resolution constant for the whole process instead of
    # letting it flip per Thread.sleep call (see JDK-5091934).
    '-XX:+ForceTimeHighResolution',
    '-XX:+PrintGCApplicationStoppedTime',
    '-Dfabric.development=true',
    "-Dfabric.remapClasspathFile=$edge\.gradle\loom-cache\remapClasspath.txt",
    "-Dlog4j.configurationFile=$edge\.gradle\loom-cache\log4j.xml,$edge\log4j2.xml",
    '-Dlog4j2.formatMsgNoLookups=true',
    "-Dnet.minecraftforge.gradle.GradleStart.srg.srg-mcp=$srg",
    '-Dmixin.env.remapRefMap=true',
    '-Dfabric.log.disableAnsi=false',
    "-Djava.library.path=$natives",
    "-Dorg.lwjgl.librarypath=$natives",
    # Never grab the cursor: an unattended loop must not hijack the desktop.
    '-Dfml.noGrab=true'
    # Deliberately omitted vs. Loom's launch.cfg: mixin.debug and asmhelper.verbose.
    # Both add classload and logging overhead that would pollute frame timings.
)

$gameArgs = @(
    '-cp', $cp,
    'net.minecraft.launchwrapper.Launch',
    '--assetIndex', '1.8.9-1.8',
    '--assetsDir', $assets,
    '--tweakClass', 'net.minecraftforge.fml.common.launcher.FMLTweaker',
    '--tweakClass', 'org.spongepowered.asm.launch.MixinTweaker',
    '--accessToken', 'undefined',
    '--mixin', 'mixins.fpsmaster.json'
)

$stdout = Join-Path $gameDir 'launch-stdout.log'
$stderr = Join-Path $gameDir 'launch-stderr.log'

$sw = [Diagnostics.Stopwatch]::StartNew()
$proc = Start-Process -FilePath $java -ArgumentList ($jvmArgs + $gameArgs) `
    -WorkingDirectory $gameDir -PassThru `
    -RedirectStandardOutput $stdout -RedirectStandardError $stderr

$resultDir = Join-Path $gameDir 'bench-results'
$deadline  = (Get-Date).AddSeconds($TimeoutSec)
$outcome   = 'TIMEOUT'

while ((Get-Date) -lt $deadline) {
    if ($proc.HasExited) {
        $outcome = 'EXITED'
        break
    }
    if (-not $Scenario -and -not $KeepAlive) {
        # Smoke mode: stop as soon as the client is up.
        $log = Join-Path $gameDir 'logs\latest.log'
        if ((Test-Path $log) -and
            ((Get-Content $log -Raw -ErrorAction SilentlyContinue) -match 'Sound engine started')) {
            $outcome = 'REACHED_MENU'
            break
        }
    }
    Start-Sleep -Milliseconds 500
}
$sw.Stop()

if (-not $proc.HasExited) {
    Stop-Process -Id $proc.Id -Force -ErrorAction SilentlyContinue
}
# Always wait: the redirected stdout/stderr handles stay open until the process is
# fully reaped, and the next run in a series starts by deleting this directory.
$proc.WaitForExit(15000) | Out-Null

# Success is decided by the artefact, not the exit code: Windows PowerShell 5.1 does
# not cache ExitCode on a Start-Process -PassThru object the way pwsh 7 does, and an
# unattended series must not silently discard eight good runs over that difference.
$resultFile = Join-Path $resultDir 'result.json'
$failedFile = Join-Path $resultDir 'FAILED'
$succeeded  = (Test-Path $resultFile) -and -not (Test-Path $failedFile)

$shotsDir = Join-Path $resultDir 'screenshots'

[pscustomobject]@{
    Outcome    = if ($succeeded) { 'OK' } elseif (Test-Path $failedFile) { 'HARNESS_FAILED' } else { $outcome }
    ElapsedSec = [math]::Round($sw.Elapsed.TotalSeconds, 1)
    GameDir    = $gameDir
    ResultFile = if ($succeeded) { $resultFile } else { $null }
    ShotsDir   = if (Test-Path $shotsDir) { $shotsDir } else { $null }
}

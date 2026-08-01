<#
.SYNOPSIS
    Launches the client to play with, using the same command the benchmark uses.

.DESCRIPTION
    `gradlew runClient` does not work in this workspace: Loom's RunGameTask trips a Gradle 8
    property-validation check and the task fails before launching. The benchmark harness builds
    its own java command for the same reason, and it works, so this reuses it.

    What is deliberately different from run-client.ps1:

      - the game directory is `run/`, not `run-bench/`, because that one is wiped every launch
      - no fixed heap, no AlwaysPreTouch, no timer pinning: those exist to keep two benchmark
        runs comparable and only cost startup time when a person is driving
      - fml.noGrab is not set, so the mouse works
      - no username is forced, so this does not sit on the benchmark's identity

.PARAMETER Experiments
    Probe flags, same names as the benchmark's: terrainProbe, collisionProbe, hudBreakdown.

.PARAMETER PerfArgs
    Adds the JVM flags read off a running Badlion Client that are worth trying here. Off by
    default: they are unproven on this client, and a launcher script is the wrong place to
    change behaviour silently. See the notes on $perfArgs below for what each one is for.
#>
[CmdletBinding()]
param(
    [string[]] $Experiments,
    [string]   $Username = 'Dev',
    [string]   $Memory = '2G',
    [switch]   $PerfArgs
)

$ErrorActionPreference = 'Stop'
$edge     = Split-Path -Parent $PSScriptRoot
$gameDir  = Join-Path $edge 'run'
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
New-Item -ItemType Directory -Force -Path $gameDir | Out-Null

$cp = (Get-Content $cpFile -Raw).Trim()

$jvmArgs = @(
    "-Xmx$Memory",
    '-Dfabric.development=true',
    "-Dfabric.remapClasspathFile=$edge\.gradle\loom-cache\remapClasspath.txt",
    "-Dlog4j.configurationFile=$edge\.gradle\loom-cache\log4j.xml,$edge\log4j2.xml",
    '-Dlog4j2.formatMsgNoLookups=true',
    "-Dnet.minecraftforge.gradle.GradleStart.srg.srg-mcp=$srg",
    '-Dmixin.env.remapRefMap=true',
    '-Dfabric.log.disableAnsi=false',
    "-Djava.library.path=$natives",
    "-Dorg.lwjgl.librarypath=$natives"
)
foreach ($e in $Experiments) { $jvmArgs += "-Dedge.exp.$e=true" }

if ($PerfArgs) {
    $jvmArgs += @(
        # Stops the JVM writing its perf counters into a memory-mapped file. Badlion sets it
        # to hide from jps/jcmd, but the reason to want it here is that those mmap writes are
        # a known safepoint-pause source. Costs nothing and changes nothing on screen.
        '-XX:+PerfDisableSharedMem',
        # System.gc() from any library is a full stop-the-world collection nobody asked for.
        # The benchmark harness already sets this; the dev launcher did not.
        '-XX:+DisableExplicitGC',
        # Not a heap dump setting. The path is never used -- it exists so that "minecraft.exe"
        # appears in the process command line, which is what Intel and AMD drivers match on to
        # apply their Minecraft-specific profile. Vanilla's own launcher does this, and the
        # string is copied verbatim because the drivers match on it.
        '-XX:HeapDumpPath=MojangTricksIntelDriversForPerformance_javaw.exe_minecraft.exe.heapdump'
    )
    # Deliberately not included: -DLog4jContextSelector=...AsyncLoggerContextSelector, which
    # needs LMAX Disruptor on the classpath and this project does not depend on it. Without
    # the jar log4j2 logs an error and silently falls back to synchronous logging, so setting
    # it here would look like the optimisation was on when it was not.
    Write-Host "perf args: $($jvmArgs[-3..-1] -join ' ')"
}

$gameArgs = @(
    '-cp', $cp,
    'net.minecraft.launchwrapper.Launch',
    '--assetIndex', '1.8.9-1.8',
    '--assetsDir', $assets,
    '--tweakClass', 'net.minecraftforge.fml.common.launcher.FMLTweaker',
    '--tweakClass', 'org.spongepowered.asm.launch.MixinTweaker',
    '--accessToken', 'undefined',
    '--username', $Username,
    '--mixin', 'mixins.fpsmaster.json'
)

Write-Host "launching in $gameDir"
# Run *in* the game directory. Minecraft resolves logs, options.txt, saves and crash reports
# against the working directory, so launching from the project root writes all of them into the
# repository -- which is what the first version of this script did.
Push-Location $gameDir
try { & $java ($jvmArgs + $gameArgs) } finally { Pop-Location }

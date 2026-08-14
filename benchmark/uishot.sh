#!/bin/zsh
# macOS twin of run-client.ps1's -UiShot mode: launch the dev client, screenshot
# one client screen, quit. Bypasses Gradle for the same reason the ps1 does
# (runClient is broken under Gradle 8.6 + Loom 0.10.0.5) and because Gradle adds
# ~12s of startup per run.
#
# Usage:
#   sh benchmark/uishot.sh <name> [screen] [widthxheight] [delaySec]
#     name     output file stem -> run-shots/bench-results/<name>.png
#     screen   edge.uishot.screen value ('' = main menu)
#     size     window size, default 1280x800 (prototype shots are 1280x800)
#     delay    seconds after first screen before capture, default 6
#
# Prereqs: build/bench/classpath.txt (gradlew -I benchmark/dump-classpath.gradle
# dumpBenchClasspath) and compiled classes (gradlew classes).
set -e
EDGE="$(cd "$(dirname "$0")/.." && pwd)"
NAME="${1:-ui}"
SCREEN="${2:-}"
SIZE="${3:-1280x800}"
DELAY="${4:-6}"
WIDTH="${SIZE%x*}"
HEIGHT="${SIZE#*x}"

JAVA8="/Users/apple/.fpsmaster/runtime/jdk-8-x64/zulu8.94.0.17-ca-jre8.0.492-macosx_x64/Contents/Home/bin/java"
LOOM="$HOME/.gradle/caches/essential-loom"
NATIVES="$LOOM/1.8.9/natives"
ASSETS="$LOOM/assets"
SRG="$LOOM/1.8.9/de.oceanlabs.mcp.mcp_stable.1_8_9.22-1.8.9-forge-1.8.9-11.15.1.2318-1.8.9/mappings-srg-named.srg"
CP_FILE="$EDGE/build/bench/classpath.txt"
GAMEDIR="$EDGE/run-shots"

for required in "$JAVA8" "$CP_FILE" "$NATIVES" "$ASSETS" "$SRG"; do
    if [ ! -e "$required" ]; then
        echo "missing prerequisite: $required" >&2
        exit 1
    fi
done

# Fresh game dir every run so config drift can't leak between shots. A seed dir
# lets a test pre-position client config (e.g. mark OOBE done, pick a background).
rm -rf "$GAMEDIR"
mkdir -p "$GAMEDIR"
cp "$EDGE/benchmark/options.benchmark.txt" "$GAMEDIR/options.txt"
printf 'overrideWidth:%s\noverrideHeight:%s\n' "$WIDTH" "$HEIGHT" >> "$GAMEDIR/options.txt"
if [ -d "$EDGE/benchmark/shot-seed" ]; then
    cp -R "$EDGE/benchmark/shot-seed/." "$GAMEDIR/"
fi

CP="$(cat "$CP_FILE")"

EXTRA_PROPS=()
if [ -n "$SCREEN" ]; then
    EXTRA_PROPS+=("-Dedge.uishot.screen=$SCREEN")
fi

# LWJGL2's NSAutoreleasePool startup crash is probabilistic on this Mac; retry.
# A watchdog hard-kills a hung client so an unattended loop can't stall forever.
TIMEOUT_SEC="${UISHOT_TIMEOUT:-240}"
attempt=1
while true; do
    set +e
    "$JAVA8" \
        -Xms1G -Xmx2G \
        -Dfabric.development=true \
        "-Dfabric.remapClasspathFile=$EDGE/.gradle/loom-cache/remapClasspath.txt" \
        "-Dlog4j.configurationFile=$EDGE/.gradle/loom-cache/log4j.xml,$EDGE/log4j2.xml" \
        -Dlog4j2.formatMsgNoLookups=true \
        "-Dnet.minecraftforge.gradle.GradleStart.srg.srg-mcp=$SRG" \
        -Dmixin.env.remapRefMap=true \
        "-Djava.library.path=$NATIVES" \
        "-Dorg.lwjgl.librarypath=$NATIVES" \
        -Dfml.noGrab=true \
        "-Dfpsmaster.hidpi=${UISHOT_HIDPI:-true}" \
        "-Dedge.uishot=$DELAY" \
        "-Dedge.uishot.name=$NAME" \
        "${EXTRA_PROPS[@]}" \
        -cp "$CP" \
        net.minecraft.launchwrapper.Launch \
        --gameDir "$GAMEDIR" \
        --assetIndex 1.8.9-1.8 \
        --assetsDir "$ASSETS" \
        --tweakClass net.minecraftforge.fml.common.launcher.FMLTweaker \
        --tweakClass org.spongepowered.asm.launch.MixinTweaker \
        --accessToken undefined \
        --mixin mixins.fpsmaster.json \
        --username Steve \
        > "$GAMEDIR/launch.log" 2>&1 &
    client=$!
    ( sleep "$TIMEOUT_SEC"; kill "$client" 2>/dev/null ) &
    watchdog=$!
    wait "$client"
    code=$?
    kill "$watchdog" 2>/dev/null
    wait "$watchdog" 2>/dev/null
    set -e
    if ls "$GAMEDIR"/bench-results/screenshots/*.png >/dev/null 2>&1; then
        ls "$GAMEDIR"/bench-results/screenshots/*.png
        exit 0
    fi
    if [ $attempt -ge 3 ]; then
        echo "no screenshot after $attempt attempts (exit $code); tail of launch.log:" >&2
        tail -30 "$GAMEDIR/launch.log" >&2
        exit 1
    fi
    attempt=$((attempt + 1))
done

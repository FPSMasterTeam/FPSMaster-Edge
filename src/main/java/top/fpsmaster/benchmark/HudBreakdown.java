package top.fpsmaster.benchmark;

import top.fpsmaster.modules.logger.ClientLogger;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Times each piece of the HUD in place, by name.
 *
 * <p>The HUD section covers the whole overlay - vanilla's, Forge's and ours - so its 704us says
 * nothing about which part to look at. Timing in place rather than by removal, for the reason the
 * sky taught: a removal probe reports nothing when the thing it removes was not happening, and says
 * nothing useful when the cost simply moves.
 *
 * <pre>
 *   -Dedge.exp.hudBreakdown=true
 * </pre>
 */
public final class HudBreakdown {

    private static final Map<String, long[]> BUCKETS = new LinkedHashMap<String, long[]>();
    private static long frames;

    private HudBreakdown() {
    }

    public static boolean enabled() {
        return Experiments.active(Experiments.HUD_BREAKDOWN);
    }

    public static void record(String name, long nanos) {
        long[] bucket = BUCKETS.get(name);
        if (bucket == null) {
            bucket = new long[2];
            BUCKETS.put(name, bucket);
        }
        bucket[0] += nanos;
        bucket[1]++;
    }

    /** Called once per overlay; reports periodically so a run produces a few readings. */
    public static void endFrame() {
        if (++frames % 600L != 0L) {
            return;
        }
        StringBuilder line = new StringBuilder("hud breakdown over " + frames + " frames:");
        long total = 0L;
        for (Map.Entry<String, long[]> entry : BUCKETS.entrySet()) {
            long nanos = entry.getValue()[0];
            total += nanos;
            if (nanos / 1000.0d / frames < 1.0d) {
                continue;  // below a microsecond a frame is noise, and there are twenty of these
            }
            line.append(String.format(" | %s %.1fus", entry.getKey(), nanos / 1000.0d / frames));
        }
        line.append(String.format(" || counted %.1fus/frame", total / 1000.0d / frames));
        ClientLogger.info("hudbreak", line.toString());
    }
}

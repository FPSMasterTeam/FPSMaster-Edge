package top.fpsmaster.benchmark;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import top.fpsmaster.cosmetic.CosmeticManager;
import top.fpsmaster.modules.logger.ClientLogger;
import top.fpsmaster.modules.music.MusicTextures;
import top.fpsmaster.replay.ReplayRecorder;
import top.fpsmaster.utils.render.gui.Backgrounds;

/**
 * Flips features on and off during the measurement window, to hunt resource leaks.
 *
 * <p>A settled scene never exercises the paths where leaks actually happen. Every leak this client
 * has had came from something being created and destroyed repeatedly — a {@code ShaderGroup}
 * reloaded per frame once leaked framebuffers — and a scenario that stands still cannot reproduce
 * that. Toggling the feature on a timer does.
 *
 * <p>Frame times from a stress run are meaningless by construction, since the workload changes
 * mid-window. Only the resource counters should be read from it.
 */
public final class BenchStress {

    /** Fixed names so a stress run reuses one replay file and one texture key instead of growing a set. */
    private static final String PROBE_NAME = "leak-stress";

    private static final String[] NO_TARGETS = new String[0];

    private final String[] targets;
    private final String[] cycle;
    private final long intervalMillis;
    private long lastToggleMillis;
    private boolean state;
    private int cycles;

    private BenchStress(String[] targets, String[] cycle, long intervalMillis) {
        this.targets = targets;
        this.cycle = cycle;
        this.intervalMillis = intervalMillis;
    }

    public static BenchStress parse(JsonObject scenario) {
        if (scenario == null || !scenario.has("stress")) {
            return null;
        }
        JsonObject stress = scenario.getAsJsonObject("stress");
        String[] targets = strings(stress, "toggle");
        String[] cycle = strings(stress, "cycle");
        long interval = stress.has("intervalMillis") ? stress.get("intervalMillis").getAsLong() : 2000L;
        return targets.length == 0 && cycle.length == 0
                ? null : new BenchStress(targets, cycle, interval);
    }

    private static String[] strings(JsonObject stress, String member) {
        if (!stress.has(member)) {
            return NO_TARGETS;
        }
        JsonArray array = stress.getAsJsonArray(member);
        String[] values = new String[array.size()];
        int i = 0;
        for (JsonElement element : array) {
            values[i++] = element.getAsString();
        }
        return values;
    }

    public void update(long nowMillis) {
        if (nowMillis - lastToggleMillis < intervalMillis) {
            return;
        }
        lastToggleMillis = nowMillis;
        state = !state;
        cycles++;
        for (String target : targets) {
            BenchOverrides.set(target, state);
        }
        for (String subsystem : cycle) {
            cycle(subsystem, state);
        }
    }

    /**
     * Opens and closes a subsystem that owns GL textures or worker threads but is not a module, so
     * {@link BenchOverrides} cannot reach it. What this produces is one create/destroy pair per
     * interval for the resource counters to balance.
     */
    private static void cycle(String subsystem, boolean open) {
        if ("cosmetics".equals(subsystem)) {
            if (open) {
                CosmeticManager.getInstance().reloadCustom();
            } else {
                CosmeticManager.getInstance().releaseTextures();
            }
        } else if ("music".equals(subsystem)) {
            // A locally rendered QR, so the cycle needs no network and still uploads a real texture.
            if (open) {
                MusicTextures.qr(PROBE_NAME);
            } else {
                MusicTextures.invalidate(PROBE_NAME);
            }
        } else if ("background".equals(subsystem)) {
            if (open) {
                Backgrounds.initGui();
            } else {
                Backgrounds.clearCaches();
            }
        } else if ("replay".equals(subsystem)) {
            if (open) {
                ReplayRecorder.instance().start(PROBE_NAME);
            } else {
                ReplayRecorder.instance().stop();
            }
        } else {
            // Same reasoning as an unresolved override: a silently ignored target produces a run
            // that looks clean because it never exercised anything.
            throw new IllegalArgumentException("no cycle target named '" + subsystem + "'");
        }
    }

    public int cycles() {
        return cycles;
    }

    public void logSummary() {
        ClientLogger.info("benchmark", "stress toggled " + targets.length + " target(s) and cycled "
                + cycle.length + " subsystem(s) over " + cycles + " cycles");
    }
}

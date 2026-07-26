package top.fpsmaster.benchmark;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import top.fpsmaster.modules.logger.ClientLogger;

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

    private final String[] targets;
    private final long intervalMillis;
    private long lastToggleMillis;
    private boolean state;
    private int cycles;

    private BenchStress(String[] targets, long intervalMillis) {
        this.targets = targets;
        this.intervalMillis = intervalMillis;
    }

    public static BenchStress parse(JsonObject scenario) {
        if (scenario == null || !scenario.has("stress")) {
            return null;
        }
        JsonObject stress = scenario.getAsJsonObject("stress");
        JsonArray toggle = stress.getAsJsonArray("toggle");
        String[] targets = new String[toggle.size()];
        int i = 0;
        for (JsonElement element : toggle) {
            targets[i++] = element.getAsString();
        }
        long interval = stress.has("intervalMillis") ? stress.get("intervalMillis").getAsLong() : 2000L;
        return targets.length == 0 ? null : new BenchStress(targets, interval);
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
    }

    public int cycles() {
        return cycles;
    }

    public void logSummary() {
        ClientLogger.info("benchmark", "stress toggled " + targets.length + " target(s) over "
                + cycles + " cycles");
    }
}

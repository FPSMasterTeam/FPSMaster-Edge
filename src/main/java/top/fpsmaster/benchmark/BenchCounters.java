package top.fpsmaster.benchmark;

import com.google.gson.JsonObject;

/**
 * Plain counters for work actually performed during a benchmark run.
 *
 * <p>These exist to answer the question frame times cannot: did the optimisation under test change
 * behaviour at all, and in the intended direction? A culling change that shows a frame-time win but
 * leaves {@code entitiesCulled} at zero did something other than what it claims.
 *
 * <p>Counters are never reset mid-run. Some code paths fire once during startup and never again —
 * display-list compilation is the obvious case — so a counter that was cleared when measurement
 * began would read zero for a feature that is demonstrably working. Instead the report carries both
 * the run total and the delta over the measurement window, and each question is answered by the
 * appropriate one.
 *
 * <p>Every increment site is guarded by {@link BenchmarkMode#ACTIVE}, which HotSpot folds away
 * outside benchmark runs.
 */
public final class BenchCounters {

    public static long entitiesAttempted;
    public static long entitiesCulled;
    public static long entitiesRendered;

    public static long particlesTicked;
    public static long particlesRendered;
    public static long particlesCulled;

    /**
     * Sub-feature hit counters. These exist so the switch matrix can be checked: toggling a
     * sub-feature, or the Performance module as a whole, must move the corresponding counter to or
     * from zero. Without them "the setting is wired up" is an assertion, not an observation.
     */
    public static long staticParticleColorHits;
    public static long batchedModelDraws;
    public static long lowAnimationTickHits;
    public static long skyColorCacheHits;
    public static long packIconsDownscaled;

    public static long chunkRebuildsRequested;
    public static long chunkRebuildsCompleted;
    public static long chunkThrottleSleeps;

    public static long fontCacheHits;
    public static long fontCacheMisses;
    public static long fontCacheEvictions;

    public static long cullProbesIssued;
    public static long cullProbesHarvested;
    public static long cullProbesOccluded;

    /**
     * GL objects live in driver memory, so a steady leak there leaves the Java heap flat. During a
     * measurement window the scene is settled and the camera repeats a fixed loop, which makes a net
     * rise in live objects a leak rather than churn.
     */
    public static long displayListsAllocated;
    public static long displayListsReleased;
    public static long texturesAllocated;
    public static long texturesReleased;

    public static long animatedSpritesTotal;
    public static long animatedSpritesUpdated;

    private static final String[] NAMES = {
            "entitiesAttempted", "entitiesCulled", "entitiesRendered",
            "particlesTicked", "particlesRendered", "particlesCulled",
            "staticParticleColorHits", "batchedModelDraws", "lowAnimationTickHits",
            "skyColorCacheHits",
            "packIconsDownscaled",
            "chunkRebuildsRequested", "chunkRebuildsCompleted", "chunkThrottleSleeps",
            "fontCacheHits", "fontCacheMisses", "fontCacheEvictions",
            "cullProbesIssued", "cullProbesHarvested", "cullProbesOccluded",
            "displayListsAllocated", "displayListsReleased",
            "texturesAllocated", "texturesReleased",
            "animatedSpritesTotal", "animatedSpritesUpdated",
    };

    private BenchCounters() {
    }

    /** Current values, in the same order as {@link #NAMES}. Called twice per run, not on a hot path. */
    public static long[] values() {
        return new long[]{
                entitiesAttempted, entitiesCulled, entitiesRendered,
                particlesTicked, particlesRendered, particlesCulled,
                staticParticleColorHits, batchedModelDraws, lowAnimationTickHits,
                skyColorCacheHits,
                packIconsDownscaled,
                chunkRebuildsRequested, chunkRebuildsCompleted, chunkThrottleSleeps,
                fontCacheHits, fontCacheMisses, fontCacheEvictions,
                cullProbesIssued, cullProbesHarvested, cullProbesOccluded,
                displayListsAllocated, displayListsReleased,
                texturesAllocated, texturesReleased,
                animatedSpritesTotal, animatedSpritesUpdated,
        };
    }

    public static JsonObject toJson(long[] counters) {
        JsonObject json = new JsonObject();
        for (int i = 0; i < NAMES.length; i++) {
            json.addProperty(NAMES[i], counters[i]);
        }
        return json;
    }

    public static long[] difference(long[] after, long[] before) {
        long[] delta = new long[after.length];
        for (int i = 0; i < after.length; i++) {
            delta[i] = after[i] - before[i];
        }
        return delta;
    }
}

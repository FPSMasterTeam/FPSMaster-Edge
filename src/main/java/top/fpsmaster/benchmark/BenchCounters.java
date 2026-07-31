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

    public static long chunkRebuilds;
    public static long terrainDrawCalls;
    public static long chunkThrottleSleeps;
    public static long chunkBudgetMovingTicks;
    public static long chunkBudgetStillTicks;
    public static long collisionQueriesSkipped;
    public static long entityListLookups;
    public static long entityListNonEmpty;

    public static long cullCandidates;
    public static long cullDormantFrames;
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

    /**
     * Volume behind the per-entity layer stack, which is the largest single section on a recorded
     * PvP workload. A ceiling probe says what a pass is worth; these say how much work was in it,
     * which is what decides whether the answer generalises beyond the recording it was measured on.
     *
     * <p>{@code armorGlintModelRenders} counts whole model renders, not enchanted pieces: vanilla
     * draws the model twice more per enchanted piece, each with its own texture matrix.
     */
    public static long armorLayerRenders;
    public static long armorPiecesRendered;
    public static long armorGlintModelRenders;
    public static long heldItemLayerRenders;

    public static long armorTextureCacheHits;

    public static long signsRendered;
    public static long signTextCulled;
    public static long blockEntitiesAttempted;
    public static long blockEntitiesCulled;

    private static final String[] NAMES = {
            "entitiesAttempted", "entitiesCulled", "entitiesRendered",
            "particlesTicked", "particlesRendered", "particlesCulled",
            "staticParticleColorHits", "batchedModelDraws", "lowAnimationTickHits",
            "skyColorCacheHits",
            "packIconsDownscaled",
            "chunkRebuilds", "terrainDrawCalls", "chunkThrottleSleeps", "entityListLookups", "entityListNonEmpty",
            "chunkBudgetMovingTicks", "chunkBudgetStillTicks", "collisionQueriesSkipped",
            "cullCandidates", "cullDormantFrames", "cullProbesIssued", "cullProbesHarvested", "cullProbesOccluded",
            "displayListsAllocated", "displayListsReleased",
            "texturesAllocated", "texturesReleased",
            "animatedSpritesTotal", "animatedSpritesUpdated",
            "armorLayerRenders", "armorPiecesRendered", "armorGlintModelRenders", "heldItemLayerRenders",
            "armorTextureCacheHits",
            "signsRendered", "signTextCulled",
            "blockEntitiesAttempted", "blockEntitiesCulled",
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
                chunkRebuilds, terrainDrawCalls, chunkThrottleSleeps,
                entityListLookups, entityListNonEmpty,
                chunkBudgetMovingTicks, chunkBudgetStillTicks, collisionQueriesSkipped,
                cullCandidates, cullDormantFrames, cullProbesIssued, cullProbesHarvested, cullProbesOccluded,
                displayListsAllocated, displayListsReleased,
                texturesAllocated, texturesReleased,
                animatedSpritesTotal, animatedSpritesUpdated,
                armorLayerRenders, armorPiecesRendered, armorGlintModelRenders, heldItemLayerRenders,
                armorTextureCacheHits,
                signsRendered, signTextCulled,
                blockEntitiesAttempted, blockEntitiesCulled,
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

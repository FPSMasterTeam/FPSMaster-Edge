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
    public static long textureUploads;
    public static long textureUploadsDirect;
    public static long textureUploadPixels;
    public static long visibleListReused;
    public static long modelCallLists;
    public static long modelComposedTransforms;

    /** Shadowed strings drawn as one recording and one draw call instead of two. */
    public static long mergedShadowDraws;

    /** Strings drawn through the client font renderer, both passes counted separately. */
    public static long clientFontDraws;

    /** Entries into vanilla's {@code drawString}, and how many of those asked for a shadow. */
    public static long drawStringCalls;
    public static long drawStringShadowed;

    /**
     * Nanoseconds inside texture upload, split by which path read the pixels.
     *
     * <p>Counts rather than section timings because uploads happen while loading, outside any
     * measured window. Without these the whole of {@code FastTextureUpload} — shipped on, three
     * quarters of uploads taking a direct path — has no number attached to it at all.
     */
    public static long textureUploadNanos;
    public static long textureUploadDirectNanos;

    /**
     * The rest of the load-time picture: mipmap generation, atlas stitching, PNG decode.
     *
     * <p>Roadmap §4.6 named these three and none was ever priced, because loading is not made of
     * frames and nothing in this harness looked outside a measured window. The upload half of the
     * same section turned out to be 160ms without its optimisation, so the others are worth a
     * number before anything is decided about them.
     */
    public static long mipmapNanos;
    public static long mipmapCalls;
    public static long atlasStitchNanos;
    public static long imageDecodeNanos;
    public static long imageDecodeCalls;

    /** Item models replayed from a display list, and how many lists were recorded. */
    public static long itemModelListHits;
    public static long itemModelListsRecorded;

    /** Obfuscated strings drawn, and how many of those were served from the scramble cache. */
    public static long obfuscatedStrings;
    public static long obfuscatedCacheHits;
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

    /**
     * Framebuffers, GL programs, embedded browsers and client worker threads — the four resource
     * classes the soak gate reads alongside textures and display lists.
     *
     * <p>Edge embeds no browser; the pair is here so an Edge report and Nova's runtime probe carry
     * the same field set and can be diffed directly. It stays at zero on this client.
     */
    public static long framebuffersAllocated;
    public static long framebuffersReleased;
    public static long shaderProgramsAllocated;
    public static long shaderProgramsReleased;
    public static long browsersOpened;
    public static long browsersClosed;
    public static long workerThreadsStarted;
    public static long workerThreadsStopped;

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
            "textureUploads", "textureUploadsDirect", "textureUploadPixels",
            "textureUploadNanos", "textureUploadDirectNanos",
            "mipmapNanos", "mipmapCalls", "atlasStitchNanos", "imageDecodeNanos", "imageDecodeCalls", "visibleListReused",
            "modelCallLists", "modelComposedTransforms", "mergedShadowDraws", "clientFontDraws", "drawStringCalls", "drawStringShadowed", "obfuscatedStrings", "obfuscatedCacheHits", "itemModelListHits", "itemModelListsRecorded",
            "cullCandidates", "cullDormantFrames", "cullProbesIssued", "cullProbesHarvested", "cullProbesOccluded",
            "displayListsAllocated", "displayListsReleased",
            "texturesAllocated", "texturesReleased",
            "framebuffersAllocated", "framebuffersReleased",
            "shaderProgramsAllocated", "shaderProgramsReleased",
            "browsersOpened", "browsersClosed",
            "workerThreadsStarted", "workerThreadsStopped",
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
                textureUploads, textureUploadsDirect, textureUploadPixels,
                textureUploadNanos, textureUploadDirectNanos,
                mipmapNanos, mipmapCalls, atlasStitchNanos, imageDecodeNanos, imageDecodeCalls, visibleListReused,
                modelCallLists, modelComposedTransforms, mergedShadowDraws, clientFontDraws, drawStringCalls, drawStringShadowed, obfuscatedStrings, obfuscatedCacheHits, itemModelListHits, itemModelListsRecorded,
                cullCandidates, cullDormantFrames, cullProbesIssued, cullProbesHarvested, cullProbesOccluded,
                displayListsAllocated, displayListsReleased,
                texturesAllocated, texturesReleased,
                framebuffersAllocated, framebuffersReleased,
                shaderProgramsAllocated, shaderProgramsReleased,
                browsersOpened, browsersClosed,
                workerThreadsStarted, workerThreadsStopped,
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

    /**
     * Wraps a worker body so the thread is counted from the moment it actually runs until it exits.
     *
     * <p>Counting at construction would report threads that were never started, and a thread that
     * outlives the run is exactly what the soak gate is looking for.
     */
    public static Runnable trackWorker(final Runnable body) {
        if (!BenchmarkMode.ACTIVE) {
            return body;
        }
        return new Runnable() {
            @Override
            public void run() {
                workerThreadsStarted++;
                try {
                    body.run();
                } finally {
                    workerThreadsStopped++;
                }
            }
        };
    }
}

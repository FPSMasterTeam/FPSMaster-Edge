package top.fpsmaster.benchmark;

import com.google.gson.JsonObject;

/**
 * Plain counters for work actually performed during a benchmark run.
 *
 * <p>These exist to answer the question frame times cannot: did the optimisation under test change
 * behaviour at all, and in the intended direction? A culling change that shows a frame-time win but
 * leaves {@code entitiesCulled} at zero did something other than what it claims.
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

    public static long chunkRebuildsRequested;
    public static long chunkRebuildsCompleted;
    public static long chunkThrottleSleeps;

    public static long fontCacheHits;
    public static long fontCacheMisses;
    public static long fontCacheEvictions;

    public static long animatedSpritesTotal;
    public static long animatedSpritesUpdated;

    private BenchCounters() {
    }

    public static void reset() {
        entitiesAttempted = 0L;
        entitiesCulled = 0L;
        entitiesRendered = 0L;
        particlesTicked = 0L;
        particlesRendered = 0L;
        particlesCulled = 0L;
        chunkRebuildsRequested = 0L;
        chunkRebuildsCompleted = 0L;
        chunkThrottleSleeps = 0L;
        fontCacheHits = 0L;
        fontCacheMisses = 0L;
        fontCacheEvictions = 0L;
        animatedSpritesTotal = 0L;
        animatedSpritesUpdated = 0L;
    }

    public static JsonObject snapshot() {
        JsonObject json = new JsonObject();
        json.addProperty("entitiesAttempted", entitiesAttempted);
        json.addProperty("entitiesCulled", entitiesCulled);
        json.addProperty("entitiesRendered", entitiesRendered);
        json.addProperty("particlesTicked", particlesTicked);
        json.addProperty("particlesRendered", particlesRendered);
        json.addProperty("particlesCulled", particlesCulled);
        json.addProperty("chunkRebuildsRequested", chunkRebuildsRequested);
        json.addProperty("chunkRebuildsCompleted", chunkRebuildsCompleted);
        json.addProperty("chunkThrottleSleeps", chunkThrottleSleeps);
        json.addProperty("fontCacheHits", fontCacheHits);
        json.addProperty("fontCacheMisses", fontCacheMisses);
        json.addProperty("fontCacheEvictions", fontCacheEvictions);
        json.addProperty("animatedSpritesTotal", animatedSpritesTotal);
        json.addProperty("animatedSpritesUpdated", animatedSpritesUpdated);
        return json;
    }
}

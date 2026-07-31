package top.fpsmaster.benchmark;

import top.fpsmaster.modules.logger.ClientLogger;

/**
 * Prices the entity AABB query before anything is built for it.
 *
 * <p>The obvious statement of this optimisation — "vanilla scans entities linearly, add spatial
 * partitioning" — is not available here, because vanilla already partitions: {@code World} walks
 * only the chunks the box touches, and {@code Chunk} walks only the 16-block sections it touches.
 * Whatever is left has to be one of three other things, and they want different fixes: scanning
 * entities the box was never going to contain, allocating a list per query, or asking the same
 * question twice in a tick.
 *
 * <p>So this counts rather than guesses. Queries per tick; chunks and sections walked; entities
 * examined against entities returned, which is the scan waste; and how many queries in a tick had
 * a box identical to an earlier one, which is the duplicate rate. The timing is kept separate from
 * the counts because the counts survive a noisy machine and the timing does not.
 *
 * <pre>
 *   -Dedge.exp.collisionProbe=true
 * </pre>
 */
public final class CollisionProbe {

    /** Enough distinct boxes in a tick to spot repeats without the set itself becoming the cost. */
    private static final int DUPLICATE_WINDOW = 256;

    private static long ticks;
    private static long queries;
    private static long typedQueries;
    private static long duplicates;
    private static long chunksWalked;
    private static long sectionsWalked;
    private static long examined;
    private static long returned;
    private static long nanos;

    private static final long[] SEEN = new long[DUPLICATE_WINDOW];
    private static int seenCount;

    private static long started;

    private CollisionProbe() {
    }

    public static boolean enabled() {
        return Experiments.active(Experiments.COLLISION_PROBE);
    }

    /** The typed walk, which goes through a different chunk method and was missed the first time. */
    public static void beginTypedQuery(double minX, double minY, double minZ,
                                       double maxX, double maxY, double maxZ) {
        typedQueries++;
        beginQuery(minX, minY, minZ, maxX, maxY, maxZ);
    }

    public static void beginQuery(double minX, double minY, double minZ,
                                  double maxX, double maxY, double maxZ) {
        queries++;
        started = System.nanoTime();

        // Hashed rather than kept: the question is how often the same box is asked for twice in a
        // tick, and a collision would overcount by one query in a run of millions.
        long key = Double.doubleToLongBits(minX) * 31L
                ^ Double.doubleToLongBits(minY) * 37L
                ^ Double.doubleToLongBits(minZ) * 41L
                ^ Double.doubleToLongBits(maxX) * 43L
                ^ Double.doubleToLongBits(maxY) * 47L
                ^ Double.doubleToLongBits(maxZ) * 53L;
        for (int i = 0; i < seenCount; i++) {
            if (SEEN[i] == key) {
                duplicates++;
                return;
            }
        }
        if (seenCount < SEEN.length) {
            SEEN[seenCount++] = key;
        }
    }

    public static void endQuery(int found) {
        nanos += System.nanoTime() - started;
        returned += found;
    }

    public static void chunkWalked(int sections, int entitiesInThoseSections) {
        chunksWalked++;
        sectionsWalked += sections;
        examined += entitiesInThoseSections;
    }

    public static void onClientTick() {
        seenCount = 0;
        if (++ticks % 200L != 0L) {
            return;
        }
        if (queries == 0L) {
            ClientLogger.info("collision", "no entity AABB queries in " + ticks + " ticks");
            reset();
            return;
        }
        ClientLogger.info("collision", String.format(
                "entity AABB queries over %d ticks: %.1f/tick (%.1f%% typed), %.1f%% repeats of a box already asked"
                        + " for this tick | per query: %.2f chunks, %.2f sections, %.1f entities"
                        + " examined, %.1f returned (%.1f%% kept) | %.2fus each, %.1fus/tick",
                ticks, queries / (double) ticks, 100.0d * typedQueries / queries,
                100.0d * duplicates / queries,
                chunksWalked / (double) queries, sectionsWalked / (double) queries,
                examined / (double) queries, returned / (double) queries,
                examined == 0L ? 0.0d : 100.0d * returned / examined,
                nanos / 1000.0d / queries, nanos / 1000.0d / ticks));
        reset();
    }

    private static void reset() {
        ticks = 0L;
        queries = 0L;
        typedQueries = 0L;
        duplicates = 0L;
        chunksWalked = 0L;
        sectionsWalked = 0L;
        examined = 0L;
        returned = 0L;
        nanos = 0L;
    }
}

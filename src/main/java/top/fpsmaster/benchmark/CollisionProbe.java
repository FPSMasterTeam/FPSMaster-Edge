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

    /** The block half: getCollidingBoundingBoxes, which walks block positions before it walks entities. */
    private static long moves;
    private static long movePositions;
    private static long moveBoxes;
    private static long moveNanos;
    private static long moveStarted;
    private static int moveDepth;
    private static long moveNestedNanos;
    private static long moveNestedQueries;

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
        long elapsed = System.nanoTime() - started;
        nanos += elapsed;
        returned += found;
        if (moveDepth > 0) {
            // Separated because the move bracket contains this one, and reporting the whole entity
            // total as "the part inside" was wrong by however many queries happen elsewhere.
            moveNestedNanos += elapsed;
            moveNestedQueries++;
        }
    }

    /**
     * Brackets one {@code getCollidingBoundingBoxes}, which is the other half of collision and the
     * half the entity counters above cannot see.
     *
     * <p>That method walks every block position the swept box touches, asking each for its collision
     * boxes, and only then runs the entity query. So the entity query's time is inside this one, and
     * the block share is the difference. Re-entrancy is counted rather than assumed away: a move
     * resolves each axis separately and can ask more than once.
     */
    public static void beginMove(double minX, double minY, double minZ,
                                 double maxX, double maxY, double maxZ) {
        if (moveDepth++ > 0) {
            return;
        }
        moves++;
        moveStarted = System.nanoTime();
        // The same bounds vanilla derives, so the count is the positions it will actually visit.
        long x = (long) Math.floor(maxX + 1.0d) - (long) Math.floor(minX);
        long y = (long) Math.floor(maxY + 1.0d) - ((long) Math.floor(minY) - 1L);
        long z = (long) Math.floor(maxZ + 1.0d) - (long) Math.floor(minZ);
        movePositions += Math.max(0L, x) * Math.max(0L, y) * Math.max(0L, z);
    }

    public static void endMove(int boxes) {
        if (--moveDepth > 0) {
            return;
        }
        moveDepth = 0;
        moveNanos += System.nanoTime() - moveStarted;
        moveBoxes += boxes;
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
        if (moves > 0L) {
            ClientLogger.info("collision", String.format(
                    "block collision over %d ticks: %.1f moves/tick | per move: %.1f block positions"
                            + " visited, %.2f boxes returned | %.2fus each, %.1fus/tick, of which"
                            + " %.1fus/tick is the %.1f entity queries nested inside it, leaving"
                            + " %.1fus/tick of block walking",
                    ticks, moves / (double) ticks, movePositions / (double) moves,
                    moveBoxes / (double) moves, moveNanos / 1000.0d / moves,
                    moveNanos / 1000.0d / ticks, moveNestedNanos / 1000.0d / ticks,
                    moveNestedQueries / (double) ticks,
                    (moveNanos - moveNestedNanos) / 1000.0d / ticks));
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
        moves = 0L;
        movePositions = 0L;
        moveBoxes = 0L;
        moveNanos = 0L;
        moveNestedNanos = 0L;
        moveNestedQueries = 0L;
    }
}

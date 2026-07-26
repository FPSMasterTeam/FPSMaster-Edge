package top.fpsmaster.utils.math;

/**
 * Trigonometry lookup tables sized for the cache rather than for precision.
 *
 * <p>Vanilla's {@code MathHelper.SIN_TABLE} holds 65536 floats — 256 KB. Entity rendering indexes it
 * essentially at random, so on a 32 KB L1 and a few-hundred-KB L2 slice almost every lookup is a
 * miss. A smaller table trades angular resolution for locality.
 *
 * <p>Precision context: vanilla is <em>already</em> approximate. Its table quantises to 2π/65536
 * (~9.6e-5 rad), so anything downstream — movement vectors included — already tolerates table-level
 * error. A 4096-entry table quantises to ~1.5e-3 rad, which is 16x coarser but still four orders of
 * magnitude below the position tolerances a server applies.
 *
 * <p>The caveat that does matter: {@code MathHelper.sin} is also used by world generation on the
 * integrated server, so changing it changes generated terrain for a given seed. That is a
 * singleplayer-only concern and the reason this is opt-in.
 */
public final class FastMath {

    /** Table sizes are powers of two so the index mask is a single AND. */
    public static final int SIZE_4K = 4096;
    public static final int SIZE_8K = 8192;
    public static final int SIZE_16K = 16384;

    private static final float[] TABLE_4K = buildSinTable(SIZE_4K);
    private static final float[] TABLE_8K = buildSinTable(SIZE_8K);
    private static final float[] TABLE_16K = buildSinTable(SIZE_16K);

    private static final float RADIANS_TO_INDEX_4K = SIZE_4K / ((float) Math.PI * 2.0f);
    private static final float RADIANS_TO_INDEX_8K = SIZE_8K / ((float) Math.PI * 2.0f);
    private static final float RADIANS_TO_INDEX_16K = SIZE_16K / ((float) Math.PI * 2.0f);

    private static final int MASK_4K = SIZE_4K - 1;
    private static final int MASK_8K = SIZE_8K - 1;
    private static final int MASK_16K = SIZE_16K - 1;

    /** Quarter turn, in table entries, for deriving cosine from the sine table. */
    private static final int QUARTER_4K = SIZE_4K / 4;
    private static final int QUARTER_8K = SIZE_8K / 4;
    private static final int QUARTER_16K = SIZE_16K / 4;

    private FastMath() {
    }

    private static float[] buildSinTable(int size) {
        float[] table = new float[size];
        for (int i = 0; i < size; i++) {
            table[i] = (float) Math.sin(i * Math.PI * 2.0d / size);
        }
        return table;
    }

    public static float sin4k(float radians) {
        return TABLE_4K[(int) (radians * RADIANS_TO_INDEX_4K) & MASK_4K];
    }

    public static float cos4k(float radians) {
        return TABLE_4K[((int) (radians * RADIANS_TO_INDEX_4K) + QUARTER_4K) & MASK_4K];
    }

    public static float sin8k(float radians) {
        return TABLE_8K[(int) (radians * RADIANS_TO_INDEX_8K) & MASK_8K];
    }

    public static float cos8k(float radians) {
        return TABLE_8K[((int) (radians * RADIANS_TO_INDEX_8K) + QUARTER_8K) & MASK_8K];
    }

    public static float sin16k(float radians) {
        return TABLE_16K[(int) (radians * RADIANS_TO_INDEX_16K) & MASK_16K];
    }

    public static float cos16k(float radians) {
        return TABLE_16K[((int) (radians * RADIANS_TO_INDEX_16K) + QUARTER_16K) & MASK_16K];
    }

    /** Worst-case absolute error of a table of the given size against {@link Math#sin}. */
    public static double worstCaseError(int size) {
        // Truncating the index means the error is bounded by one full step, not half of one.
        return 2.0d * Math.PI / size;
    }
}

package top.fpsmaster.benchmark;

import net.minecraft.util.MathHelper;
import top.fpsmaster.utils.math.FastMath;

/**
 * Standalone microbenchmark for the trigonometry implementations.
 *
 * <p>Runs without Minecraft: {@code MathHelper} is plain bytecode on the classpath and needs no
 * LaunchWrapper, mixins or GL context. That matters because JMH forks a clean JVM, which is exactly
 * why JMH cannot be used to benchmark anything mixin-dependent in this project — here there is
 * nothing to patch, so a plain harness is both sufficient and honest about what it measures.
 *
 * <p>Access is deliberately scattered rather than sequential. A linear sweep prefetches perfectly
 * and would show every table size as equally fast, hiding the only effect being tested: whether the
 * table fits in cache when entity rendering indexes it essentially at random.
 *
 * <p>Takes exactly one implementation per invocation, and the driver script runs each in its own
 * JVM. Measuring them all in one process gave a nonsensical ordering — the 16K table appeared 65%
 * faster than vanilla while the 8K and 4K tables appeared no faster at all — because a single
 * dispatch site profiled across every implementation pollutes the JIT's picture of the loop and the
 * measurement then depends on run order. This is the same reason repeated measurements inside one
 * VM invocation are not a substitute for repeated invocations.
 *
 * <pre>
 *   java -cp ... top.fpsmaster.benchmark.MathBenchmark vanilla|fast4k|fast8k|fast16k|jdk
 * </pre>
 */
public final class MathBenchmark {

    private static final int SAMPLES = 1 << 16;
    private static final int WARMUP_ROUNDS = 5;
    private static final int TIMED_ROUNDS = 10;

    private MathBenchmark() {
    }

    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("usage: MathBenchmark vanilla|fast4k|fast8k|fast16k|jdk");
            System.exit(2);
        }
        Impl impl = parse(args[0]);

        float[] angles = new float[SAMPLES];
        // A large odd stride walks the whole input range without ever repeating a cache line
        // pattern, which is closer to how rotations arrive during rendering than a linear sweep.
        double stride = Math.PI * 2.0d * 9781.0d / SAMPLES;
        for (int i = 0; i < SAMPLES; i++) {
            angles[i] = (float) ((i * stride) % (Math.PI * 2.0d));
        }

        for (int round = 0; round < WARMUP_ROUNDS; round++) {
            run(angles, impl);
        }
        double best = Double.MAX_VALUE;
        for (int round = 0; round < TIMED_ROUNDS; round++) {
            long start = System.nanoTime();
            float sink = run(angles, impl);
            long elapsed = System.nanoTime() - start;
            if (sink == Float.MIN_VALUE) {
                System.out.print("");  // keep the result live so the loop is not optimised away
            }
            best = Math.min(best, elapsed / (double) angles.length);
        }
        // Machine-readable: the driver aggregates one line per JVM invocation.
        System.out.printf("%s	%.4f	%.3e%n", args[0], best, measureError(impl));
    }

    private static Impl parse(String name) {
        if ("vanilla".equals(name)) return Impl.VANILLA;
        if ("fast4k".equals(name)) return Impl.FAST_4K;
        if ("fast8k".equals(name)) return Impl.FAST_8K;
        if ("fast16k".equals(name)) return Impl.FAST_16K;
        if ("jdk".equals(name)) return Impl.JDK;
        throw new IllegalArgumentException("unknown implementation: " + name);
    }

    private enum Impl {
        VANILLA, FAST_4K, FAST_8K, FAST_16K, JDK
    }

    private static float apply(Impl impl, float angle) {
        switch (impl) {
            case VANILLA:
                return MathHelper.sin(angle) + MathHelper.cos(angle);
            case FAST_4K:
                return FastMath.sin4k(angle) + FastMath.cos4k(angle);
            case FAST_8K:
                return FastMath.sin8k(angle) + FastMath.cos8k(angle);
            case FAST_16K:
                return FastMath.sin16k(angle) + FastMath.cos16k(angle);
            default:
                return (float) (Math.sin(angle) + Math.cos(angle));
        }
    }

    private static float run(float[] angles, Impl impl) {
        float sink = 0.0f;
        for (int i = 0; i < angles.length; i++) {
            sink += apply(impl, angles[i]);
        }
        return sink;
    }

    /**
     * Worst observed absolute deviation of sine alone from {@link Math#sin}.
     *
     * <p>Measured on sine by itself rather than on the sin+cos sum the timing loop uses: summing two
     * approximations lets their errors add, which would overstate the error of each by up to a
     * factor of two.
     */
    private static double measureError(Impl impl) {
        double worst = 0.0d;
        for (int i = 0; i < 1 << 20; i++) {
            float angle = (float) (i * Math.PI * 2.0d / (1 << 20));
            worst = Math.max(worst, Math.abs(sinOnly(impl, angle) - Math.sin(angle)));
        }
        return worst;
    }

    private static float sinOnly(Impl impl, float angle) {
        switch (impl) {
            case VANILLA:  return MathHelper.sin(angle);
            case FAST_4K:  return FastMath.sin4k(angle);
            case FAST_8K:  return FastMath.sin8k(angle);
            case FAST_16K: return FastMath.sin16k(angle);
            default:       return (float) Math.sin(angle);
        }
    }
}

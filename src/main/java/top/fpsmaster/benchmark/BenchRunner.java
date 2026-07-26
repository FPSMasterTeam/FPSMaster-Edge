package top.fpsmaster.benchmark;

import net.minecraft.client.Minecraft;
import top.fpsmaster.modules.logger.ClientLogger;

import java.io.File;

/**
 * Drives an unattended benchmark run as a frame-paced state machine.
 *
 * <p>Ticked once per frame from the frame boundary in {@code Minecraft.runGameLoop}, right after
 * {@code FrameTimer.addFrame} — i.e. after {@code Display.update()}, so a sample covers a whole
 * present-to-present interval.
 */
public final class BenchRunner {

    /**
     * Enough for 90 seconds at ~2900 FPS. Overflow stops recording rather than growing the buffer,
     * so a runaway frame rate cannot turn into an allocation spike mid-measurement.
     */
    private static final int SAMPLE_CAPACITY = 262_144;

    private static final BenchRunner INSTANCE = new BenchRunner();

    private enum State {
        INIT,
        WARMUP,
        DISCARD,
        MEASURE,
        FINISHED,
        FAILED
    }

    private final FrameSampler sampler = new FrameSampler(SAMPLE_CAPACITY);
    private State state = State.INIT;
    private BenchScenario scenario;
    private long phaseStartMillis;
    private long gcCountAtStart;
    private long gcMillisAtStart;

    private BenchRunner() {
    }

    public static BenchRunner instance() {
        return INSTANCE;
    }

    public void onFrameBoundary() {
        if (state == State.FINISHED || state == State.FAILED) {
            return;
        }
        try {
            Minecraft mc = Minecraft.getMinecraft();
            long now = System.currentTimeMillis();
            sampler.onFrame(System.nanoTime());

            switch (state) {
                case INIT:
                    beginRun(mc, now);
                    break;
                case WARMUP:
                    if (now - phaseStartMillis >= scenario.warmupMillis()) {
                        enterDiscard(now);
                    }
                    break;
                case DISCARD:
                    if (now - phaseStartMillis >= scenario.discardMillis()) {
                        enterMeasure(now);
                    }
                    break;
                case MEASURE:
                    if (now - phaseStartMillis >= scenario.measureMillis() || sampler.isFull()) {
                        finish(mc);
                    }
                    break;
                default:
                    break;
            }
        } catch (Throwable t) {
            fail(t);
        }
    }

    private void beginRun(Minecraft mc, long now) throws Exception {
        scenario = BenchScenario.load(mc.mcDataDir, BenchmarkMode.scenario());
        BenchCounters.reset();
        gcCountAtStart = BenchReport.gcCollectionCount();
        gcMillisAtStart = BenchReport.gcCollectionMillis();
        phaseStartMillis = now;
        state = State.WARMUP;
        ClientLogger.info("benchmark", "scenario '" + scenario.id() + "' variant '" + BenchmarkMode.variant()
                + "': warmup " + scenario.warmupMillis() + "ms");
    }

    private void enterDiscard(long now) {
        sampler.start();
        phaseStartMillis = now;
        state = State.DISCARD;
    }

    private void enterMeasure(long now) {
        // Keep recording, but throw away everything captured so far: the discard window exists to
        // get past the frames where getDebugFPS() is still 0.
        sampler.discardCollected();
        BenchCounters.reset();
        gcCountAtStart = BenchReport.gcCollectionCount();
        gcMillisAtStart = BenchReport.gcCollectionMillis();
        phaseStartMillis = now;
        state = State.MEASURE;
        ClientLogger.info("benchmark", "measuring for " + scenario.measureMillis() + "ms");
    }

    private void finish(Minecraft mc) {
        sampler.stop();
        state = State.FINISHED;
        try {
            BenchReport.write(mc.mcDataDir, sampler, gcCountAtStart, gcMillisAtStart);
            ClientLogger.info("benchmark", "wrote result with " + sampler.sampleCount() + " frames");
        } catch (Throwable t) {
            ClientLogger.error("benchmark", "failed to write result: " + t);
        }
        mc.shutdown();
    }

    private void fail(Throwable cause) {
        state = State.FAILED;
        ClientLogger.error("benchmark", "run aborted: " + cause);
        try {
            // Leave a marker so the launcher can tell a crashed run from a hung one.
            File marker = new File(Minecraft.getMinecraft().mcDataDir, "bench-results");
            if (marker.isDirectory() || marker.mkdirs()) {
                new File(marker, "FAILED").createNewFile();
            }
        } catch (Throwable ignored) {
            // Reporting the failure must not itself throw; the launcher timeout is the backstop.
        }
        Minecraft.getMinecraft().shutdown();
    }
}

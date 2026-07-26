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
        LOADING_WORLD,
        SETTLING,
        WARMUP,
        DISCARD,
        MEASURE,
        SHOTS,
        FINISHED,
        FAILED
    }

    private final FrameSampler sampler = new FrameSampler(SAMPLE_CAPACITY);
    private final DisplayWatch displayWatch = new DisplayWatch();
    private State state = State.INIT;
    private BenchScenario scenario;
    private BenchWorld.SettleTracker settleTracker;
    private boolean setupIssued;
    private long phaseStartMillis;
    private long pathStartMillis;
    private long[] countersAtMeasureStart;
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
            sampler.onFrame(System.nanoTime(), displayWatch.pollDisturbed());
            BenchProfiler.instance().endFrame();
            advance(mc, now);
        } catch (Throwable t) {
            fail(t);
        }
    }

    private void advance(Minecraft mc, long now) throws Exception {
        switch (state) {
            case INIT:
                beginRun(mc, now);
                break;
            case LOADING_WORLD:
                if (BenchWorld.isReady(mc)) {
                    settleTracker.reset(now);
                    phaseStartMillis = now;
                    pathStartMillis = now;
                    state = State.SETTLING;
                    ClientLogger.info("benchmark", "world ready, settling");
                } else if (now - phaseStartMillis >= scenario.settleTimeoutMillis()) {
                    throw new IllegalStateException("world did not load within "
                            + scenario.settleTimeoutMillis() + "ms");
                }
                break;
            case SETTLING:
                // Hold at the start of the path. Settling means "let the world finish building
                // around the start position"; a moving camera keeps pulling new chunks into view,
                // so the rebuild counter would never go quiet.
                holdCameraAtPathStart(mc);
                if (settleTracker.update(now)) {
                    if (!setupIssued) {
                        // Only now: spawning and block placement are rejected in chunks the server
                        // has not loaded yet, and a partial scenario still produces a plausible
                        // looking measurement.
                        setupIssued = true;
                        BenchSetup.run(mc, scenario.world());
                        settleTracker.reset(now);
                        phaseStartMillis = now;
                        break;
                    }
                    if (BenchSetup.hasFailed()) {
                        throw new IllegalStateException("scenario setup commands failed; see log");
                    }
                    ClientLogger.info("benchmark", "settled after " + (now - phaseStartMillis)
                            + "ms, warmup " + scenario.warmupMillis() + "ms");
                    phaseStartMillis = now;
                    pathStartMillis = now;
                    state = State.WARMUP;
                } else if (now - phaseStartMillis >= scenario.settleTimeoutMillis()) {
                    throw new IllegalStateException("terrain never settled within "
                            + scenario.settleTimeoutMillis() + "ms");
                }
                break;
            case WARMUP:
                driveCamera(mc, now);
                if (now - phaseStartMillis >= scenario.warmupMillis()) {
                    sampler.start();
                    BenchProfiler.instance().start();
                    phaseStartMillis = now;
                    state = State.DISCARD;
                }
                break;
            case DISCARD:
                driveCamera(mc, now);
                if (now - phaseStartMillis >= scenario.discardMillis()) {
                    enterMeasure(now);
                }
                break;
            case MEASURE:
                driveCamera(mc, now);
                if (scenario.stress() != null) {
                    scenario.stress().update(now);
                }
                if (now - phaseStartMillis >= scenario.measureMillis() || sampler.isFull()) {
                    sampler.stop();
                    BenchProfiler.instance().stop();
                    if (scenario.screenshots() == null) {
                        finish(mc);
                    } else {
                        state = State.SHOTS;
                    }
                }
                break;
            case SHOTS:
                // Capture happens after the timed window, so writing PNGs never lands in the samples.
                scenario.camera().apply(mc.thePlayer, scenario.screenshots().currentPathMillis());
                if (scenario.screenshots().advance(mc)) {
                    finish(mc);
                }
                break;
            default:
                break;
        }
    }

    private void beginRun(Minecraft mc, long now) throws Exception {
        scenario = BenchScenario.load(mc.mcDataDir, BenchmarkMode.scenario());
        BenchOverrides.apply(BenchmarkMode.overrides());
        // Needs a current GL context, so not in the constructor.
        BenchProfiler.instance().initGpuTimer();
        gcCountAtStart = BenchReport.gcCollectionCount();
        gcMillisAtStart = BenchReport.gcCollectionMillis();
        phaseStartMillis = now;
        pathStartMillis = now;

        ClientLogger.info("benchmark", "scenario '" + scenario.id() + "' variant '"
                + BenchmarkMode.variant() + "'");

        if (scenario.world() == null) {
            settleTracker = null;
            state = State.WARMUP;
            return;
        }
        settleTracker = new BenchWorld.SettleTracker(scenario.settleSeconds());
        BenchWorld.launch(mc, scenario.world());
        state = State.LOADING_WORLD;
    }

    private void driveCamera(Minecraft mc, long now) {
        if (scenario.camera() != null && mc.thePlayer != null) {
            scenario.camera().apply(mc.thePlayer, now - pathStartMillis);
        }
    }

    private void holdCameraAtPathStart(Minecraft mc) {
        if (scenario.camera() != null && mc.thePlayer != null) {
            scenario.camera().apply(mc.thePlayer, 0L);
        }
    }

    private void enterMeasure(long now) {
        // Keep recording, but throw away everything captured so far: the discard window exists to
        // get past the frames where getDebugFPS() is still 0.
        sampler.discardCollected();
        BenchProfiler.instance().discardCollected();
        displayWatch.reset();
        countersAtMeasureStart = BenchCounters.values();
        gcCountAtStart = BenchReport.gcCollectionCount();
        gcMillisAtStart = BenchReport.gcCollectionMillis();
        phaseStartMillis = now;
        state = State.MEASURE;
        ClientLogger.info("benchmark", "measuring for " + scenario.measureMillis() + "ms");
    }

    private void finish(Minecraft mc) {
        if (scenario.stress() != null) {
            scenario.stress().logSummary();
        }
        sampler.stop();
        state = State.FINISHED;
        try {
            BenchReport.write(mc.mcDataDir, sampler, displayWatch, countersAtMeasureStart,
                    gcCountAtStart, gcMillisAtStart);
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
            File dir = new File(Minecraft.getMinecraft().mcDataDir, "bench-results");
            if (dir.isDirectory() || dir.mkdirs()) {
                new File(dir, "FAILED").createNewFile();
            }
        } catch (Throwable ignored) {
            // Reporting the failure must not itself throw; the launcher timeout is the backstop.
        }
        Minecraft.getMinecraft().shutdown();
    }
}

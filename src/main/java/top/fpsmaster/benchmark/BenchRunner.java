package top.fpsmaster.benchmark;

import net.minecraft.client.Minecraft;
import top.fpsmaster.modules.logger.ClientLogger;
import top.fpsmaster.replay.ReplayPlayer;
import top.fpsmaster.utils.io.FileUtils;

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

    /**
     * Ceiling on waiting for a steady frame time.
     *
     * <p>A run that never settles is a result in itself and should be reported rather than waited
     * on forever — it means the scenario or the machine is doing something the harness cannot see.
     */
    private static final long DISCARD_CEILING_MILLIS = 30_000L;

    private static final BenchRunner INSTANCE = new BenchRunner();

    private enum State {
        INIT,
        LOADING_WORLD,
        LOADING_REPLAY,
        SETTLING,
        WARMUP,
        DISCARD,
        MEASURE,
        SHOTS,
        FINISHED,
        FAILED
    }

    private final FrameSampler sampler = new FrameSampler(SAMPLE_CAPACITY);
    /** How far past the anchor the window may open before the pin is reported as not holding. */
    private static final long ANCHOR_TOLERANCE_MILLIS = 50L;

    private final SteadyState steadyState = new SteadyState();
    private long lastFrameNanos;
    private final DisplayWatch displayWatch = new DisplayWatch();
    private State state = State.INIT;
    private BenchScenario scenario;
    private BenchWorld.SettleTracker settleTracker;
    private boolean setupIssued;
    private long phaseStartMillis;

    /** Recording position the measured window opened at, or -1 when this is not a replay. */
    private int replayAtMeasureStart = -1;
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
            long frameNanos = System.nanoTime();
            if (state == State.DISCARD && lastFrameNanos != 0L) {
                steadyState.record(frameNanos - lastFrameNanos);
            }
            lastFrameNanos = frameNanos;
            sampler.onFrame(frameNanos, displayWatch.pollDisturbed());
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
            case LOADING_REPLAY:
                // Settle means the same thing it does for a world: give it time to fill in. Here
                // that is the recording's own opening burst - the snapshot of chunks and entities -
                // plus enough of the stream to be past it.
                if (!ReplayPlayer.instance().isActive()) {
                    throw new IllegalStateException("replay stopped before the run began");
                }
                if (ReplayPlayer.instance().hasAvatar()
                        && ReplayPlayer.instance().elapsedMillis() >= scenario.settleSeconds() * 1000L) {
                    // Possessed, so the camera is the recorder's own movement track: identical on
                    // both sides of a comparison, with no path of our own to drift.
                    ReplayPlayer.instance().possess();
                    ClientLogger.info("benchmark", "replay settled, warmup "
                            + scenario.warmupMillis() + "ms");
                    phaseStartMillis = now;
                    state = State.WARMUP;
                } else if (now - phaseStartMillis >= scenario.settleTimeoutMillis()) {
                    throw new IllegalStateException("replay never produced an avatar within "
                            + scenario.settleTimeoutMillis() + "ms");
                }
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
                    steadyState.reset();
                    lastFrameNanos = 0L;
                    phaseStartMillis = now;
                    state = State.DISCARD;
                }
                break;
            case DISCARD:
                driveCamera(mc, now);
                // discardMillis is a floor now rather than the whole wait: measurement starts when
                // the run has stopped getting faster, because how long that takes is itself variable
                // -- the same scenario has settled in a tenth of one run and three tenths of another.
                boolean waitedLongEnough = now - phaseStartMillis >= scenario.discardMillis();
                // On a replay, the window also has to open at a fixed point in the recording. Steady
                // state is reached at a different wall-clock moment every run, and a replay keeps
                // playing while we wait, so without this the measured span lands somewhere different
                // each time and the scene differs with it.
                long anchor = edge$replayAnchor();
                boolean replayReady = anchor < 0L || ReplayPlayer.instance().elapsedMillis() >= anchor;
                if (waitedLongEnough && steadyState.isSteady() && replayReady) {
                    int at = scenario.replay() == null
                            ? -1 : ReplayPlayer.instance().elapsedMillis();
                    // The anchor is only worth having if it is what the window waits on. When steady
                    // state arrives after the recording has already passed it, the start is decided
                    // by the frame rate again and two runs drift apart -- measured at 249ms apart,
                    // enough to change the visible crowd by 22%. Raise the anchor past the worst
                    // steady time rather than leaving this warning to be ignored.
                    if (anchor >= 0L && at > anchor + ANCHOR_TOLERANCE_MILLIS) {
                        ClientLogger.warn("benchmark: replay had already reached " + at + "ms when"
                                + " the run steadied, past the " + anchor + "ms anchor. The window is"
                                + " not pinned and this run is not comparable frame for frame --"
                                + " raise replayMeasureFromMillis above " + at + ".");
                    }
                    ClientLogger.info("benchmark", "steady after " + (now - phaseStartMillis)
                            + "ms of discard, measuring " + scenario.measureMillis() + "ms"
                            + (anchor < 0L ? "" : " from replay t=" + at + "ms (anchor " + anchor + ")"));
                    enterMeasure(now);
                } else if (now - phaseStartMillis >= DISCARD_CEILING_MILLIS) {
                    ClientLogger.warn("benchmark: never reached a steady frame time in "
                            + DISCARD_CEILING_MILLIS + "ms of discard; measuring anyway, and this"
                            + " run's numbers should be treated as suspect");
                    enterMeasure(now);
                }
                break;
            case MEASURE:
                if (scenario.replay() != null && !ReplayPlayer.instance().isActive()) {
                    throw new IllegalStateException("the recording ended before the measurement did;"
                            + " shorten measureMillis or record longer");
                }
                driveCamera(mc, now);
                if (scenario.stress() != null) {
                    scenario.stress().update(now);
                }
                // Closed on replay position too, so both ends of the window are the same recording
                // moment in every run and two runs compare frame for frame.
                long measureAnchor = edge$replayAnchor();
                boolean windowDone = measureAnchor < 0L
                        ? now - phaseStartMillis >= scenario.measureMillis()
                        : ReplayPlayer.instance().elapsedMillis()
                                >= measureAnchor + scenario.measureMillis();
                if (windowDone || sampler.isFull()) {
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
                // Shots are posed explicitly, which is what makes them comparable between runs. A
                // replay has no camera block — the recording drives the view — so there is nothing
                // to pose, and the shot would land wherever the replay happened to stop, which
                // depends on how fast the run went. Measured: two runs of the same recording
                // differed by 65-87% of pixels. Failing here is the point; a screenshot gate that
                // compares two different moments passes and fails for reasons of its own.
                if (scenario.camera() == null) {
                    throw new IllegalStateException("scenario '" + scenario.id() + "' is a replay and"
                            + " cannot take screenshots: they are posed from a camera path, and a"
                            + " replay has none. Remove its screenshots block.");
                }
                scenario.camera().apply(mc.thePlayer, scenario.screenshots().currentPathMillis());
                if (scenario.screenshots().advance(mc)) {
                    finish(mc);
                }
                break;
            default:
                break;
        }
    }

    /**
     * The recording position the measured window is pinned to, or -1 when it is not pinned.
     *
     * <p>Only meaningful for replay scenarios. A scenario that sets it while the recording has
     * already run past that point by the time discard ends would otherwise wait forever, so the
     * discard ceiling still applies and the run is reported as suspect rather than hanging.
     */
    private long edge$replayAnchor() {
        if (scenario.replay() == null || scenario.replayMeasureFromMillis() < 0L) {
            return -1L;
        }
        return scenario.replayMeasureFromMillis();
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

        if (scenario.replay() != null) {
            settleTracker = null;
            ReplayPlayer.instance().start(new File(new File(FileUtils.dir, "replays"),
                    scenario.replay() + ".edgereplay"));
            if (!ReplayPlayer.instance().isActive()) {
                throw new IllegalStateException("could not open replay '" + scenario.replay() + "'");
            }
            state = State.LOADING_REPLAY;
            return;
        }
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
        replayAtMeasureStart = scenario.replay() == null
                ? -1 : ReplayPlayer.instance().elapsedMillis();
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
                    gcCountAtStart, gcMillisAtStart, replayAtMeasureStart,
                    scenario.replay() == null ? -1 : ReplayPlayer.instance().elapsedMillis());
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

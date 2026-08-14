package top.fpsmaster.benchmark;

import net.minecraft.client.Minecraft;
import top.fpsmaster.modules.logger.ClientLogger;
import top.fpsmaster.replay.ReplayPlayer;
import top.fpsmaster.replay.director.CameraKeyframe;
import top.fpsmaster.replay.director.CameraPose;
import top.fpsmaster.replay.director.CameraTrack;
import top.fpsmaster.replay.director.DirectorCamera;
import top.fpsmaster.replay.director.DirectorExporter;

/**
 * Unattended end-to-end check of the director pipeline.
 *
 * <pre>
 *   -Dedge.autoworld=&lt;save folder&gt;   load a singleplayer world straight from the menu
 *   -Dedge.director.smoke=&lt;fps&gt;      once a replay is playing: build a small orbit track
 *                                       around the recorder, export it, then quit
 * </pre>
 *
 * Combined with {@code edge.replay.record} / {@code edge.replay.play} this exercises the whole
 * chain — record, sidecar, track sampling, camera takeover, deterministic clock, framebuffer
 * capture and the ffmpeg pipe — with no hands on the keyboard.
 */
public final class DirectorSmoke {

    private static final int FPS = Integer.getInteger("edge.director.smoke", -1).intValue();
    private static final String AUTO_WORLD = System.getProperty("edge.autoworld");

    private static boolean worldLaunched;
    private static boolean trackBuilt;
    private static boolean exportStarted;
    private static boolean shuttingDown;

    private DirectorSmoke() {
    }

    public static void onClientTick() {
        Minecraft mc = Minecraft.getMinecraft();
        if (AUTO_WORLD != null && !worldLaunched && mc.theWorld == null && mc.currentScreen != null
                && !(mc.currentScreen instanceof net.minecraft.client.gui.GuiScreenWorking)) {
            worldLaunched = true;
            ClientLogger.info("director-smoke", "loading world '" + AUTO_WORLD + "'");
            mc.launchIntegratedServer(AUTO_WORLD, AUTO_WORLD, null);
            return;
        }
        if (FPS <= 0) {
            return;
        }
        ReplayPlayer player = ReplayPlayer.instance();
        if (!player.isActive()) {
            if (exportStarted) {
                // export teardown stopped the replay, or something died — either way we are done
                shutdownSoon(mc);
            }
            return;
        }
        if (!trackBuilt && player.elapsedMillis() > 1200) {
            trackBuilt = true;
            buildOrbitTrack(mc, player);
            return;
        }
        if (trackBuilt && !exportStarted) {
            exportStarted = true;
            // Custom resolution: exercises the framebuffer-resize path independent of the window.
            boolean ok = DirectorExporter.start(FPS, 1280, 720);
            ClientLogger.info("director-smoke", "export start -> " + (ok ? "ok" : DirectorExporter.errorMessage()));
            if (!ok) {
                shutdownSoon(mc);
            }
            return;
        }
        if (exportStarted && !DirectorExporter.isRunning()) {
            if (!shuttingDown) {
                ClientLogger.info("director-smoke", "export state=" + DirectorExporter.state()
                        + " file=" + DirectorExporter.outputFile()
                        + (DirectorExporter.errorMessage().isEmpty() ? "" : " error=" + DirectorExporter.errorMessage()));
            }
            shutdownSoon(mc);
        }
    }

    /** A quarter orbit around the camera's starting point (= the recorder's position). */
    private static void buildOrbitTrack(Minecraft mc, ReplayPlayer player) {
        CameraPose base = DirectorCamera.capturePose();
        if (base == null) {
            return;
        }
        CameraTrack track = DirectorCamera.track();
        track.keyframes.clear();
        int start = Math.max(200, player.elapsedMillis() - 500);
        double radius = 6.0;
        for (int i = 0; i < 4; i++) {
            double angle = Math.toRadians(90.0 * i);
            double cx = base.x + Math.sin(angle) * radius;
            double cz = base.z - Math.cos(angle) * radius;
            float yaw = (float) Math.toDegrees(angle);
            CameraKeyframe frame = new CameraKeyframe(start + i * 2000,
                    new CameraPose(cx, base.y + 2.0, cz, yaw, 25f, i % 2 == 0 ? 70f : 50f));
            frame.transition = CameraKeyframe.Transition.SMOOTH;
            frame.easing = CameraKeyframe.Easing.EASE_IN_OUT;
            track.keyframes.add(frame);
        }
        track.sort();

        // Cut list: keep [start, start+4000], first half in 0.5x slow motion.
        // Expected output: 2000/0.5 + 2000/1 = 6000ms regardless of the recording's length.
        int replayDuration = Math.max(player.durationMillis(), player.elapsedMillis());
        track.segments.clear();
        track.trimStart(start, replayDuration);
        track.trimEnd(start + 4000, replayDuration);
        track.splitAt(start + 2000, replayDuration);
        top.fpsmaster.replay.director.TimelineSegment slow = track.segmentAt(start + 1000, replayDuration);
        if (slow != null && !slow.excluded) {
            slow.speed = 0.5f;
        }
        DirectorCamera.markDirty();
        DirectorCamera.saveIfDirty();
        ClientLogger.info("director-smoke", "orbit track built: " + track.startMillis() + ".." + track.endMillis()
                + "ms, edit out=" + track.outputDurationMillis(replayDuration) + "ms segments=" + track.segments.size());
    }

    private static void shutdownSoon(Minecraft mc) {
        if (!shuttingDown) {
            shuttingDown = true;
            mc.shutdown();
        }
    }
}

package top.fpsmaster.replay.director;

import net.minecraft.client.Minecraft;
import net.minecraft.client.shader.Framebuffer;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import top.fpsmaster.FPSMaster;
import top.fpsmaster.modules.logger.ClientLogger;
import top.fpsmaster.replay.ReplayPlayer;
import top.fpsmaster.utils.io.FileUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Frame-driven mp4 export of a camera track: every rendered frame is read back from Minecraft's
 * framebuffer and piped raw into an external ffmpeg process.
 *
 * <p>Driven from the head of {@code runGameLoop}: at that point the framebuffer still holds the
 * frame the previous iteration rendered, so the exporter captures it, then advances the replay
 * clock deterministically ({@link ReplayPlayer#externalAdvanceTo}) and pins the camera on the next
 * sampled pose — the iteration that follows renders exactly frame N+1 of the timeline. Playback
 * time is therefore driven by frame count, not wall time; a slow machine exports slower, never
 * differently.
 */
public final class DirectorExporter {

    public enum State {
        IDLE, SEEKING, RUNNING, FINISHING, DONE, FAILED
    }

    private static State state = State.IDLE;
    private static String error = "";
    private static Process ffmpeg;
    private static OutputStream video;
    private static ByteBuffer frameBuffer;
    private static byte[] frameBytes;
    private static int width;
    private static int height;
    private static int fps;
    private static int replayDuration;
    private static long outputDuration;
    private static int frameIndex;
    private static int totalFrames;
    private static File outputFile;
    private static boolean hudWasHidden;
    /** Window framebuffer size to restore when a custom export resolution was used. */
    private static int restoreWidth;
    private static int restoreHeight;
    private static boolean resized;

    private DirectorExporter() {
    }

    public static boolean isRunning() {
        return state == State.SEEKING || state == State.RUNNING || state == State.FINISHING;
    }

    public static State state() {
        return state;
    }

    public static String errorMessage() {
        return error;
    }

    public static File outputFile() {
        return outputFile;
    }

    public static float progress() {
        return totalFrames <= 0 ? 0f : Math.min(1f, frameIndex / (float) totalFrames);
    }

    public static String progressText() {
        return frameIndex + " / " + totalFrames;
    }

    /** Clears a finished/failed banner so the workbench returns to its resting state. */
    public static void acknowledge() {
        if (state == State.DONE || state == State.FAILED) {
            state = State.IDLE;
        }
    }

    /** Convenience overload: window resolution. */
    public static boolean start(int framesPerSecond) {
        return start(framesPerSecond, 0, 0);
    }

    /**
     * Starts an export. {@code exportWidth/Height} of 0 mean the current window resolution;
     * anything else temporarily resizes Minecraft's framebuffer — the capture reads the FBO, not
     * the window, so the output is exactly that size regardless of the window.
     */
    public static boolean start(int framesPerSecond, int exportWidth, int exportHeight) {
        if (isRunning()) {
            return false;
        }
        ReplayPlayer player = ReplayPlayer.instance();
        CameraTrack track = DirectorCamera.track();
        if (!player.isActive()) {
            return fail("director.export.error.empty");
        }
        replayDuration = Math.max(player.durationMillis(), player.elapsedMillis());
        outputDuration = exportSpanMillis(track, replayDuration);
        if (outputDuration <= 0) {
            return fail("director.export.error.empty");
        }
        String binary = locateFfmpeg();
        if (binary == null) {
            return fail("director.export.error.noffmpeg");
        }

        Minecraft mc = Minecraft.getMinecraft();
        restoreWidth = mc.displayWidth;
        restoreHeight = mc.displayHeight;
        resized = exportWidth > 0 && exportHeight > 0
                && (exportWidth != mc.displayWidth || exportHeight != mc.displayHeight);
        width = (resized ? exportWidth : mc.displayWidth) & ~1;   // yuv420p wants even dimensions
        height = (resized ? exportHeight : mc.displayHeight) & ~1;
        if (resized) {
            mc.resize(width, height);
        }
        fps = framesPerSecond;
        frameIndex = 0;
        totalFrames = (int) (outputDuration * (long) fps / 1000L) + 1;

        File directory = new File(FileUtils.dir, "exports");
        if (!directory.isDirectory() && !directory.mkdirs()) {
            return fail("director.export.error.io");
        }
        String replayName = player.file() == null ? "replay" : player.file().getName().replace(".edgereplay", "");
        String stamp = new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.ROOT).format(new Date());
        outputFile = new File(directory, replayName + "-" + stamp + ".mp4");

        List<String> command = new ArrayList<String>();
        command.add(binary);
        command.add("-y");
        command.add("-f");
        command.add("rawvideo");
        command.add("-pix_fmt");
        command.add("rgba");
        command.add("-s");
        command.add(width + "x" + height);
        command.add("-r");
        command.add(String.valueOf(fps));
        command.add("-i");
        command.add("pipe:0");
        command.add("-vf");
        command.add("vflip");
        command.add("-c:v");
        command.add("libx264");
        command.add("-preset");
        command.add("medium");
        command.add("-crf");
        command.add("18");
        command.add("-pix_fmt");
        command.add("yuv420p");
        command.add(outputFile.getAbsolutePath());
        try {
            ProcessBuilder builder = new ProcessBuilder(command);
            builder.redirectErrorStream(false);
            ffmpeg = builder.start();
        } catch (IOException exception) {
            ClientLogger.error("director", "could not start ffmpeg: " + exception);
            return fail("director.export.error.noffmpeg");
        }
        video = ffmpeg.getOutputStream();
        drainStderr(ffmpeg);

        int pixels = width * height * 4;
        if (frameBuffer == null || frameBuffer.capacity() < pixels) {
            frameBuffer = BufferUtils.createByteBuffer(pixels);
            frameBytes = new byte[pixels];
        }

        hudWasHidden = mc.gameSettings.hideGUI;
        mc.gameSettings.hideGUI = true;
        if (player.isPaused()) {
            player.togglePause();
        }
        player.setExternalClock(true);
        error = "";
        // The edit may start before the current position; a backwards seek rebuilds the world.
        player.seek(outputToSource(0));
        state = State.SEEKING;
        ClientLogger.info("director", "export started: " + width + "x" + height + "@" + fps
                + " frames=" + totalFrames + " out=" + outputDuration + "ms -> " + outputFile.getName());
        return true;
    }

    /** Output length of the current edit: the cut list when present, else the keyframe span. */
    public static long exportSpanMillis(CameraTrack track, int duration) {
        if (!track.segments.isEmpty()) {
            return track.hasKeptContent(duration) ? track.outputDurationMillis(duration) : 0L;
        }
        return Math.max(0L, track.endMillis() - track.startMillis());
    }

    /** Maps a moment of the output movie to replay time under the current edit. */
    private static int outputToSource(long outputMillis) {
        CameraTrack track = DirectorCamera.track();
        if (!track.segments.isEmpty()) {
            return track.mapOutputToSource(outputMillis, replayDuration);
        }
        return track.startMillis() + (int) outputMillis;
    }

    public static void cancel() {
        if (!isRunning()) {
            return;
        }
        finishProcess(false);
        if (outputFile != null && outputFile.isFile() && !outputFile.delete()) {
            ClientLogger.warn("director: could not delete cancelled export " + outputFile);
        }
        state = State.IDLE;
    }

    /** Frame boundary hook — called from the head of every runGameLoop iteration. */
    public static void onFrame() {
        if (state == State.IDLE || state == State.DONE || state == State.FAILED) {
            return;
        }
        ReplayPlayer player = ReplayPlayer.instance();
        if (!player.isActive()) {
            finishProcess(false);
            fail("director.export.error.stopped");
            return;
        }
        if (state == State.SEEKING) {
            if (player.isSeeking()) {
                return;
            }
            // Seek landed at (or before) the edit start; step the clock onto the exact frame grid.
            player.setExternalClock(true);
            stepTo(player, 0);
            state = State.RUNNING;
            return; // this iteration renders frame 0; captured on the next
        }
        if (state == State.RUNNING) {
            if (!capture()) {
                return;
            }
            frameIndex++;
            if (frameIndex >= totalFrames) {
                state = State.FINISHING;
                return;
            }
            stepTo(player, frameIndex);
            return;
        }
        if (state == State.FINISHING) {
            boolean ok = finishProcess(true);
            state = ok ? State.DONE : State.FAILED;
            if (ok) {
                ClientLogger.info("director", "export finished: " + outputFile.getAbsolutePath());
            }
        }
    }

    /** Advances the replay to output frame {@code frame} and pins the camera on its pose. */
    private static void stepTo(ReplayPlayer player, int frame) {
        int source = outputToSource(frame * 1000L / fps);
        player.externalAdvanceTo(source);
        CameraTrack track = DirectorCamera.track();
        if (!track.isEmpty()) {
            DirectorCamera.applyExact(track.sample(source));
        }
    }

    /** Reads the finished frame back from the MC framebuffer and pipes it to ffmpeg. */
    private static boolean capture() {
        Minecraft mc = Minecraft.getMinecraft();
        Framebuffer framebuffer = mc.getFramebuffer();
        try {
            frameBuffer.clear();
            framebuffer.bindFramebufferTexture();
            GL11.glGetTexImage(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, frameBuffer);
            framebuffer.unbindFramebufferTexture();
            // The FBO may be larger than the (evened) export size; crop by copying row prefixes.
            int fboWidth = framebuffer.framebufferTextureWidth;
            int rowBytes = width * 4;
            for (int row = 0; row < height; row++) {
                frameBuffer.position(row * fboWidth * 4);
                frameBuffer.get(frameBytes, row * rowBytes, rowBytes);
            }
            video.write(frameBytes, 0, height * rowBytes);
            return true;
        } catch (IOException | RuntimeException exception) {
            ClientLogger.error("director", "export frame failed: " + exception);
            finishProcess(false);
            fail("director.export.error.io");
            return false;
        }
    }

    private static boolean finishProcess(boolean waitForExit) {
        ReplayPlayer player = ReplayPlayer.instance();
        player.setExternalClock(false);
        Minecraft mc = Minecraft.getMinecraft();
        mc.gameSettings.hideGUI = hudWasHidden;
        if (resized) {
            resized = false;
            mc.resize(restoreWidth, restoreHeight);
        }
        DirectorCamera.clearOverride();
        if (video != null) {
            try {
                video.close();
            } catch (IOException ignored) {
                // the process is going away either way
            }
            video = null;
        }
        if (ffmpeg == null) {
            return false;
        }
        Process process = ffmpeg;
        ffmpeg = null;
        if (!waitForExit) {
            process.destroy();
            return false;
        }
        try {
            int code = process.waitFor();
            if (code != 0) {
                ClientLogger.error("director", "ffmpeg exited with " + code);
                error = i18n("director.export.error.ffmpeg") + " (" + code + ")";
                return false;
            }
            return true;
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            process.destroy();
            return false;
        }
    }

    private static void drainStderr(final Process process) {
        Thread drain = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
                    String line;
                    String last = "";
                    while ((line = reader.readLine()) != null) {
                        last = line;
                    }
                    if (!last.isEmpty()) {
                        ClientLogger.info("director", "ffmpeg: " + last);
                    }
                } catch (IOException ignored) {
                    // stream closes when the process dies; nothing to do
                }
            }
        }, "Edge-FfmpegLog");
        drain.setDaemon(true);
        drain.start();
    }

    private static String locateFfmpeg() {
        List<String> candidates = new ArrayList<String>();
        String configured = System.getProperty("fpsmaster.ffmpeg");
        if (configured != null && !configured.isEmpty()) {
            candidates.add(configured);
        }
        candidates.add("ffmpeg");
        candidates.add("/opt/homebrew/bin/ffmpeg");
        candidates.add("/usr/local/bin/ffmpeg");
        candidates.add("/usr/bin/ffmpeg");
        for (String candidate : candidates) {
            try {
                // Read -version output to EOF before waiting: closing the pipe instead makes
                // ffmpeg die on EPIPE and look "missing" even when it is right there.
                ProcessBuilder builder = new ProcessBuilder(candidate, "-version");
                builder.redirectErrorStream(true);
                Process probe = builder.start();
                byte[] scratch = new byte[4096];
                while (probe.getInputStream().read(scratch) != -1) {
                    // just draining
                }
                if (probe.waitFor() == 0) {
                    return candidate;
                }
            } catch (IOException | InterruptedException | RuntimeException missing) {
                if (missing instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }
            }
        }
        return null;
    }

    private static boolean fail(String key) {
        state = State.FAILED;
        error = i18n(key);
        return false;
    }

    private static String i18n(String key) {
        return FPSMaster.i18n == null ? key : FPSMaster.i18n.get(key);
    }
}

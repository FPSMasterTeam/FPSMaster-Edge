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
    private static int startMillis;
    private static int endMillis;
    private static int frameIndex;
    private static int totalFrames;
    private static File outputFile;
    private static boolean hudWasHidden;

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

    public static boolean start(int framesPerSecond) {
        if (isRunning()) {
            return false;
        }
        ReplayPlayer player = ReplayPlayer.instance();
        CameraTrack track = DirectorCamera.track();
        if (!player.isActive() || track.isEmpty() || track.endMillis() <= track.startMillis()) {
            return fail("director.export.error.empty");
        }
        String binary = locateFfmpeg();
        if (binary == null) {
            return fail("director.export.error.noffmpeg");
        }

        Minecraft mc = Minecraft.getMinecraft();
        width = mc.displayWidth & ~1;   // yuv420p wants even dimensions
        height = mc.displayHeight & ~1;
        fps = framesPerSecond;
        startMillis = track.startMillis();
        endMillis = track.endMillis();
        frameIndex = 0;
        totalFrames = (int) ((endMillis - startMillis) * (long) fps / 1000L) + 1;

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
        // The track may start before the current position; a backwards seek rebuilds the world.
        player.seek(startMillis);
        state = State.SEEKING;
        ClientLogger.info("director", "export started: " + width + "x" + height + "@" + fps
                + " frames=" + totalFrames + " -> " + outputFile.getName());
        return true;
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
            // Seek landed at (or before) the track start; step the clock onto the exact frame grid.
            player.setExternalClock(true);
            player.externalAdvanceTo(frameMillis(0));
            DirectorCamera.applyExact(DirectorCamera.track().sample(frameMillis(0)));
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
            int millis = frameMillis(frameIndex);
            player.externalAdvanceTo(millis);
            DirectorCamera.applyExact(DirectorCamera.track().sample(millis));
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

    private static int frameMillis(int frame) {
        return startMillis + (int) (frame * 1000L / fps);
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
        Minecraft.getMinecraft().gameSettings.hideGUI = hudWasHidden;
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

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
    private static long exportStartNanos;

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
        // Any open screen (the workbench included) renders into the same framebuffer the capture
        // reads — it would be baked into every exported frame.
        if (mc.currentScreen != null) {
            mc.displayGuiScreen(null);
        }
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
            if (org.lwjgl.input.Keyboard.isKeyDown(org.lwjgl.input.Keyboard.KEY_ESCAPE)) {
                cancel();
                return;
            }
            if (player.isSeeking()) {
                return;
            }
            // Seek landed at (or before) the edit start; step the clock onto the exact frame grid.
            player.setExternalClock(true);
            stepTo(player, 0);
            exportStartNanos = System.nanoTime();
            state = State.RUNNING;
            return; // this iteration renders frame 0; captured on the next
        }
        if (state == State.RUNNING) {
            // ESC = cancel, Premiere-style. No screen is open during export, so poll directly.
            if (org.lwjgl.input.Keyboard.isKeyDown(org.lwjgl.input.Keyboard.KEY_ESCAPE)) {
                cancel();
                return;
            }
            // A screen that opened mid-export (pause menu, disconnect, ...) would be baked into
            // the frame the loop just rendered: close it and re-render this frame instead of
            // capturing the contaminated one.
            if (Minecraft.getMinecraft().currentScreen != null) {
                Minecraft.getMinecraft().displayGuiScreen(null);
                return;
            }
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

    // ------------------------------------------------------------------
    // Export presentation (window-side; never reaches the exported frames)
    // ------------------------------------------------------------------

    /**
     * Paints the Premiere-style export screen straight into the window's backbuffer, with a small
     * live preview of the frame the game just rendered offscreen. Returns false when no export is
     * running, so the caller falls through to the normal frame blit.
     */
    public static boolean presentExportScreen(net.minecraft.client.shader.Framebuffer framebuffer) {
        if (!isRunning()) {
            return false;
        }
        int windowWidth = restoreWidth;
        int windowHeight = restoreHeight;
        if (windowWidth <= 0 || windowHeight <= 0) {
            return false;
        }
        // Raw GL throughout: UiChrome/Rects/Images mix cached GlStateManager calls with raw
        // glEnable/glDisable, so the caches cannot be trusted mid-pass. Truth is re-synced below.
        GL11.glViewport(0, 0, windowWidth, windowHeight);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDepthMask(false);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glLoadIdentity();
        float logicalHeight = 400f;
        float logicalWidth = windowWidth * logicalHeight / windowHeight;
        GL11.glOrtho(0, logicalWidth, logicalHeight, 0, -1, 1);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glLoadIdentity();
        GL11.glClearColor(0.035f, 0.035f, 0.045f, 1f);
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);

        drawExportUi(framebuffer, logicalWidth, logicalHeight);
        maybeSavePresentShot(windowWidth, windowHeight);

        // Leave GL exactly where vanilla framebufferRender would have: depth test off, depth
        // writes on, alpha/blend off — and force each GlStateManager cache through a transition
        // so cache and real GL agree again no matter what the UI helpers left behind.
        net.minecraft.client.renderer.GlStateManager.enableDepth();
        net.minecraft.client.renderer.GlStateManager.disableDepth();
        net.minecraft.client.renderer.GlStateManager.depthMask(false);
        net.minecraft.client.renderer.GlStateManager.depthMask(true);
        net.minecraft.client.renderer.GlStateManager.enableAlpha();
        net.minecraft.client.renderer.GlStateManager.disableAlpha();
        net.minecraft.client.renderer.GlStateManager.enableBlend();
        net.minecraft.client.renderer.GlStateManager.disableBlend();
        net.minecraft.client.renderer.GlStateManager.enableTexture2D();
        GL11.glColor4f(1f, 1f, 1f, 1f);
        return true;
    }

    private static boolean presentShotSaved;

    /**
     * Debug capture of the presentation screen itself ({@code -Dedge.director.presentshot=<png>}).
     * Reads the window backbuffer — a normal screenshot would show the offscreen export frame.
     */
    private static void maybeSavePresentShot(int windowWidth, int windowHeight) {
        String path = System.getProperty("edge.director.presentshot");
        if (path == null || presentShotSaved || frameIndex < totalFrames / 2) {
            return;
        }
        presentShotSaved = true;
        try {
            java.nio.ByteBuffer pixels =
                    org.lwjgl.BufferUtils.createByteBuffer(windowWidth * windowHeight * 4);
            GL11.glReadBuffer(GL11.GL_BACK);
            GL11.glReadPixels(0, 0, windowWidth, windowHeight,
                    GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, pixels);
            java.awt.image.BufferedImage image = new java.awt.image.BufferedImage(
                    windowWidth, windowHeight, java.awt.image.BufferedImage.TYPE_INT_RGB);
            for (int y = 0; y < windowHeight; y++) {
                int srcRow = (windowHeight - 1 - y) * windowWidth * 4;
                for (int x = 0; x < windowWidth; x++) {
                    int i = srcRow + x * 4;
                    image.setRGB(x, y, ((pixels.get(i) & 0xFF) << 16)
                            | ((pixels.get(i + 1) & 0xFF) << 8) | (pixels.get(i + 2) & 0xFF));
                }
            }
            File out = new File(path);
            javax.imageio.ImageIO.write(image, "png", out);
            ClientLogger.info("director", "present shot -> " + out.getAbsolutePath());
        } catch (Exception e) {
            ClientLogger.error("director", "present shot failed: " + e);
        }
    }

    private static void drawExportUi(net.minecraft.client.shader.Framebuffer framebuffer,
                                     float logicalWidth, float logicalHeight) {
        float centerX = logicalWidth / 2f;

        // preview window, aspect of the export, capped in both directions
        float previewH = 216f;
        float previewW = previewH * width / (float) height;
        float maxW = logicalWidth - 80f;
        if (previewW > maxW) {
            previewW = maxW;
            previewH = previewW * height / (float) width;
        }
        float px = centerX - previewW / 2f;
        float py = 40f;
        top.fpsmaster.utils.render.draw.Rects.rounded(px - 1f, py - 1f, previewW + 2f, previewH + 2f, 4,
                top.fpsmaster.ui.click.ClickGuiTheme.strokeStrong().getRGB(), false);
        drawFramebufferQuad(framebuffer, px, py, previewW, previewH);

        String title = FPSMaster.i18n.get("director.export.running")
                + (outputFile == null ? "" : "  " + outputFile.getName());
        top.fpsmaster.ui.click.UiChrome.boldCentered(FPSMaster.fontManager.s16, title,
                centerX, py + previewH + 14f, top.fpsmaster.ui.click.ClickGuiTheme.textPrimary().getRGB());

        // progress bar
        float barW = Math.min(300f, logicalWidth * 0.6f);
        float barX = centerX - barW / 2f;
        float barY = py + previewH + 32f;
        top.fpsmaster.utils.render.draw.Rects.rounded(barX, barY, barW, 4f, 2,
                top.fpsmaster.ui.click.ClickGuiTheme.layerActive().getRGB(), false);
        float p = progress();
        if (p > 0f) {
            top.fpsmaster.utils.render.draw.Rects.rounded(barX, barY, Math.max(2f, barW * p), 4f, 2,
                    top.fpsmaster.ui.click.ClickGuiTheme.accent().getRGB(), false);
        }

        // stats: frames · percent · eta
        String eta = "—";
        if (frameIndex > 0 && exportStartNanos > 0) {
            long elapsedMs = (System.nanoTime() - exportStartNanos) / 1_000_000L;
            long remaining = elapsedMs * (totalFrames - frameIndex) / Math.max(1, frameIndex);
            eta = top.fpsmaster.ui.screens.replay.ReplayScreen.formatDuration(remaining);
        }
        String stats = String.format(FPSMaster.i18n.get("director.export.stats"),
                frameIndex, totalFrames, (int) (p * 100), eta);
        FPSMaster.fontManager.getFont(12).drawCenteredString(stats, centerX, barY + 12f,
                top.fpsmaster.ui.click.ClickGuiTheme.textSecondary().getRGB());

        FPSMaster.fontManager.getFont(11).drawCenteredString(
                FPSMaster.i18n.get("director.export.cancelhint"),
                centerX, logicalHeight - 24f,
                top.fpsmaster.ui.click.ClickGuiTheme.textDisabled().getRGB());
    }

    /** Blits the game FBO into a rect of the current ortho space, v-flipped to read upright. */
    private static void drawFramebufferQuad(net.minecraft.client.shader.Framebuffer framebuffer,
                                            float x, float y, float w, float h) {
        framebuffer.bindFramebufferTexture();
        // Raw state: the rounded-rect helper re-enabled depth test behind GlStateManager's back,
        // and the FBO's alpha channel holds garbage that alpha test or blend would act on.
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDepthMask(false);
        GL11.glDisable(GL11.GL_ALPHA_TEST);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glColor4f(1f, 1f, 1f, 1f);
        float u = framebuffer.framebufferWidth / (float) framebuffer.framebufferTextureWidth;
        float v = framebuffer.framebufferHeight / (float) framebuffer.framebufferTextureHeight;
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glTexCoord2f(0f, v);
        GL11.glVertex2f(x, y);
        GL11.glTexCoord2f(0f, 0f);
        GL11.glVertex2f(x, y + h);
        GL11.glTexCoord2f(u, 0f);
        GL11.glVertex2f(x + w, y + h);
        GL11.glTexCoord2f(u, v);
        GL11.glVertex2f(x + w, y);
        GL11.glEnd();
        framebuffer.unbindFramebufferTexture();
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
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

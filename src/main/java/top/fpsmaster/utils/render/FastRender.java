package top.fpsmaster.utils.render;

import top.fpsmaster.features.impl.optimizes.Performance;
import top.fpsmaster.utils.system.OptifineUtil;

/**
 * Whether the game is drawing straight to the back buffer instead of through a framebuffer.
 *
 * <p>Minecraft normally renders the world into a framebuffer and then draws that framebuffer to the
 * screen as one full-screen quad. Skipping it saves a full-screen write and a full-screen textured
 * draw every frame, and costs everything that reads the framebuffer back: the client's own blur,
 * its motion blur, the minimap and the shader helpers all check here and stand down.
 *
 * <p>The answer is latched for the duration of a frame. {@code Framebuffer} decides whether to bind
 * and whether to unbind by asking separately, so a setting changed between those two calls would
 * bind a framebuffer that never gets unbound — and every frame after that would draw into it and
 * never show it.
 */
public final class FastRender {

    private static boolean active;

    private FastRender() {
    }

    /** Call once at the top of the frame, before anything binds or unbinds. */
    public static void beginFrame() {
        active = OptifineUtil.isFastRender()
                || (Performance.using && Performance.fastRender.getValue());
    }

    public static boolean isActive() {
        return active;
    }
}

package top.fpsmaster.utils.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.shader.Framebuffer;
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
 *
 * <p>Turning it off has to rebuild the framebuffer. Vanilla's {@code createBindFramebuffer} checks
 * the same question this class answers, and when the answer is no it updates the width and height
 * fields and returns without touching a single GL object. So for as long as this is active, every
 * resize — and the create at startup — leaves the real framebuffer at whatever size it last had, or
 * never makes one at all. Switching back then renders the world into a stale texture and stretches
 * it across the window, which is the smear this used to produce, and which resizing the window
 * appeared to fix because that ran the create again with the answer now being yes.
 */
public final class FastRender {

    private static boolean active;

    private FastRender() {
    }

    /** Call once at the top of the frame, before anything binds or unbinds. */
    public static void beginFrame() {
        boolean wasActive = active;
        active = OptifineUtil.isFastRender()
                || (Performance.using && Performance.fastRender.getValue());
        if (wasActive && !active) {
            rebuildFramebuffer();
        }
    }

    /**
     * Recreates the framebuffer at the window's current size.
     *
     * <p>Here rather than at the toggle, because the toggle happens inside the GUI's own render and
     * this deletes and recreates GL objects the frame in progress may be using. The top of the game
     * loop is where vanilla does the same thing on a resize.
     */
    private static void rebuildFramebuffer() {
        Minecraft mc = Minecraft.getMinecraft();
        Framebuffer framebuffer = mc == null ? null : mc.getFramebuffer();
        if (framebuffer == null || mc.displayWidth <= 0 || mc.displayHeight <= 0) {
            return;
        }
        framebuffer.createBindFramebuffer(mc.displayWidth, mc.displayHeight);
    }

    public static boolean isActive() {
        return active;
    }
}

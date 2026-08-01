package top.fpsmaster.utils.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.shader.Framebuffer;
import org.lwjgl.opengl.EXTFramebufferObject;
import org.lwjgl.opengl.GL11;

public class StencilUtil {

    public static void start() {
        Minecraft mc = Minecraft.getMinecraft();
        mc.getFramebuffer().bindFramebuffer(false);
        if (mc.getFramebuffer().depthBuffer > -1) {
            setupFBO(mc.getFramebuffer());
            mc.getFramebuffer().depthBuffer = -1;
        }
        GL11.glClear(GL11.GL_STENCIL_BUFFER_BIT);
        GL11.glStencilFunc(GL11.GL_ALWAYS, 1, 0xFF);
        GL11.glStencilOp(GL11.GL_REPLACE, GL11.GL_REPLACE, GL11.GL_REPLACE);
        GL11.glColorMask(false, false, false, false);
    }

    public static void draw(Runnable start, Runnable end) {
        GL11.glEnable(GL11.GL_STENCIL_TEST);
        start();
        start.run();
        GL11.glStencilFunc(GL11.GL_EQUAL, 1, 0xFF);
        GL11.glStencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_KEEP);
        GL11.glColorMask(true, true, true, true);
        end.run();
        GL11.glDisable(GL11.GL_STENCIL_TEST);
    }

    private static void setupFBO(Framebuffer fbo) {
        EXTFramebufferObject.glDeleteRenderbuffersEXT(fbo.depthBuffer);
        int stencilDepthBufferID = EXTFramebufferObject.glGenRenderbuffersEXT();
        EXTFramebufferObject.glBindRenderbufferEXT(36161, stencilDepthBufferID);
        EXTFramebufferObject.glRenderbufferStorageEXT(
            36161,
            34041,
            Minecraft.getMinecraft().displayWidth,
            Minecraft.getMinecraft().displayHeight
        );
        EXTFramebufferObject.glFramebufferRenderbufferEXT(36160, 36128, 36161, stencilDepthBufferID);
        EXTFramebufferObject.glFramebufferRenderbufferEXT(36160, 36096, 36161, stencilDepthBufferID);
    }

    /*
     * Given to me by igs
     */
    public static void checkSetupFBO(Framebuffer framebuffer) {
        if (framebuffer != null) {
            if (framebuffer.depthBuffer > -1) {
                setupFBO(framebuffer);
                framebuffer.depthBuffer = -1;
            }
        }
    }

    /**
     * @implNote Initializes the Stencil Buffer to write to
     */
    public static void initStencilToWrite() {
        // init
        Minecraft mc = Minecraft.getMinecraft();
        mc.getFramebuffer().bindFramebuffer(false);
        checkSetupFBO(mc.getFramebuffer());
        GL11.glClear(GL11.GL_STENCIL_BUFFER_BIT);
        GL11.glEnable(GL11.GL_STENCIL_TEST);
        GL11.glStencilFunc(GL11.GL_ALWAYS, 1, 1);
        GL11.glStencilOp(GL11.GL_REPLACE, GL11.GL_REPLACE, GL11.GL_REPLACE);
        GL11.glColorMask(false, false, false, false);
    }

    public static void readStencilBuffer(int ref) {
        GL11.glColorMask(true, true, true, true);
        GL11.glStencilFunc(GL11.GL_EQUAL, ref, 1);
        GL11.glStencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_KEEP);
    }

    /**
     * Ends a stencil pass and restores everything {@link #initStencilToWrite()} changed.
     *
     * <p>The colour mask used to be reopened only by {@link #readStencilBuffer(int)}, which is a
     * "switch to reading" call, not a teardown call. Any caller that threw — or returned early —
     * between writing and reading left the mask fully closed for the rest of the process. The symptom
     * is remote from the cause: the next {@code glClear(GL_COLOR_BUFFER_BIT)} anywhere (KawaseBlur's
     * framebuffer clear, for instance) asks the driver to clear a colour buffer it is forbidden to
     * write, which on Apple's Metal-backed GL aborts with "missing clear mask bits".
     *
     * <p>Reopening it here as well is idempotent and makes teardown total.
     */
    public static void uninitStencilBuffer() {
        GL11.glDisable(GL11.GL_STENCIL_TEST);
        GL11.glColorMask(true, true, true, true);
    }
}



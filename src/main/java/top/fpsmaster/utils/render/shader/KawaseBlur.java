package top.fpsmaster.utils.render.shader;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.shader.Framebuffer;

import java.util.ArrayList;
import java.util.List;

public class KawaseBlur {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private static final ShaderUtil kawaseDown = new ShaderUtil("blurDown");
    private static final ShaderUtil kawaseUp = new ShaderUtil("blurUp");
    private static final Framebuffer framebuffer = new Framebuffer(1, 1, false);
    private static final List<Framebuffer> framebufferList = new ArrayList<>();
    private static int currentIterations = 0;

    /** Upper bound on the half-size FBO chain, so a pathological setting cannot exhaust GPU memory. */
    private static final int MAX_ITERATIONS = 8;

    public static void setupUniforms(float offset) {
        kawaseDown.setUniformf("offset", offset, offset);
        kawaseUp.setUniformf("offset", offset, offset);
    }

    private static void initFramebuffers(float iterations) {
        for (Framebuffer fb : framebufferList) {
            fb.deleteFramebuffer();
        }
        framebufferList.clear();
        framebufferList.add(framebuffer);
        int i = 1;
        while (i <= iterations) {
            Framebuffer fb = new Framebuffer(mc.displayWidth, mc.displayHeight, false);
            framebufferList.add(fb);
            i++;
        }
    }

    public static void renderBlur(int iterations, int offset) {
        if (iterations <= 0) {
            return;
        }
        iterations = Math.min(iterations, MAX_ITERATIONS);
        // Rebuild on a size change too, so a resized window cannot leave the half-size chain stale.
        if (currentIterations != iterations
                || (framebufferList.size() > 1
                    && (framebufferList.get(1).framebufferWidth != mc.displayWidth
                        || framebufferList.get(1).framebufferHeight != mc.displayHeight))) {
            initFramebuffers(iterations);
            currentIterations = iterations;
        }
        renderFBO(framebufferList.get(1), mc.getFramebuffer().framebufferTexture, kawaseDown, (float) offset);

        // Downsample
        for (int i = 1; i < iterations; i++) {
            renderFBO(framebufferList.get(i + 1), framebufferList.get(i).framebufferTexture, kawaseDown, (float) offset);
        }

        // Upsample
        for (int i = iterations; i >= 2; i--) {
            renderFBO(framebufferList.get(i - 1), framebufferList.get(i).framebufferTexture, kawaseUp, (float) offset);
        }

        mc.getFramebuffer().bindFramebuffer(true);
        GlStateManager.bindTexture(framebufferList.get(1).framebufferTexture);
        kawaseUp.init();
        kawaseUp.setUniformf("offset", (float) offset, (float) offset);
        kawaseUp.setUniformf("halfpixel", 0.5f / mc.displayWidth, 0.5f / mc.displayHeight);
        kawaseUp.setUniformi("inTexture", 0);
        ShaderUtil.drawQuads();
        kawaseUp.unload();
    }

    private static void renderFBO(Framebuffer framebuffer, int framebufferTexture, ShaderUtil shader, float offset) {
        framebuffer.framebufferClear();
        framebuffer.bindFramebuffer(true);
        shader.init();
        GlStateManager.bindTexture(framebufferTexture);
        shader.setUniformf("offset", offset, offset);
        shader.setUniformi("inTexture", 0);
        shader.setUniformf("halfpixel", 0.5f / mc.displayWidth, 0.5f / mc.displayHeight);
        ShaderUtil.drawQuads();
        shader.unload();
        framebuffer.unbindFramebuffer();
    }
}



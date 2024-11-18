package top.fpsmaster.features.impl.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.shader.Framebuffer;
import org.lwjgl.opengl.GL11;
import top.fpsmaster.event.Subscribe;
import top.fpsmaster.event.events.EventMotionBlur;
import top.fpsmaster.features.manager.Category;
import top.fpsmaster.features.manager.Module;
import top.fpsmaster.features.settings.impl.NumberSetting;
import top.fpsmaster.interfaces.ProviderManager;
import top.fpsmaster.utils.OptifineUtil;
import top.fpsmaster.wrapper.renderEngine.bufferbuilder.WrapperBufferBuilder;

public class MotionBlur extends Module {
    private Framebuffer blurBufferMain;
    private Framebuffer blurBufferInto;
    private NumberSetting multiplier = new NumberSetting("Multiplier", 2, 0, 10, 0.5);

    public MotionBlur() {
        super("MotionBlur", Category.RENDER);
        addSettings(multiplier);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        if (OptifineUtil.Companion.isFastRender()) {
            OptifineUtil.Companion.setFastRender(false);
        }
    }

    private Framebuffer checkFramebufferSizes(Framebuffer framebuffer, int width, int height) {
        if (framebuffer == null || framebuffer.framebufferWidth != width || framebuffer.framebufferHeight != height) {
            if (framebuffer == null) {
                framebuffer = new Framebuffer(width, height, true);
            } else {
                framebuffer.createBindFramebuffer(width, height);
            }
            framebuffer.setFramebufferFilter(9728); // GL_NEAREST
        }
        return framebuffer;
    }

    private void drawTexturedRectNoBlend(float x, float y, float width, float height,
                                         float uMin, float uMax, float vMin, float vMax, int filter) {
        GlStateManager.enableTexture2D();
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, filter);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, filter);
        Tessellator tessellator = Tessellator.getInstance();
        WrapperBufferBuilder worldrenderer = new WrapperBufferBuilder(tessellator);
        worldrenderer.begin(7, DefaultVertexFormats.POSITION_TEX);
        worldrenderer.pos(x, y + height, 0.0).tex(uMin, vMax).endVertex();
        worldrenderer.pos(x + width, y + height, 0.0).tex(uMax, vMax).endVertex();
        worldrenderer.pos(x + width, y, 0.0).tex(uMax, vMin).endVertex();
        worldrenderer.pos(x, y, 0.0).tex(uMin, vMin).endVertex();
        tessellator.draw();
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, 9728);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, 9728);
    }

    @Subscribe
    public void renderOverlay(EventMotionBlur event) {
        if (ProviderManager.mcProvider.getPlayer() == null || ProviderManager.mcProvider.getPlayer().ticksExisted < 20)
            return;

        if (Minecraft.getMinecraft().currentScreen == null) {
            if (OpenGlHelper.isFramebufferEnabled()) {
                ScaledResolution sr = new ScaledResolution(Minecraft.getMinecraft());
                int width = Minecraft.getMinecraft().getFramebuffer().framebufferWidth;
                int height = Minecraft.getMinecraft().getFramebuffer().framebufferHeight;

                GlStateManager.matrixMode(GL11.GL_PROJECTION);
                GlStateManager.loadIdentity();
                GlStateManager.ortho(0.0, width / sr.getScaleFactor(), height / sr.getScaleFactor(), 0.0, 2000.0, 4000.0);
                GlStateManager.matrixMode(GL11.GL_MODELVIEW);
                GlStateManager.loadIdentity();
                GlStateManager.translate(0f, 0f, -2000f);

                blurBufferMain = checkFramebufferSizes(blurBufferMain, width, height);
                blurBufferInto = checkFramebufferSizes(blurBufferInto, width, height);

                blurBufferInto.framebufferClear();
                blurBufferInto.bindFramebuffer(true);

                OpenGlHelper.glBlendFunc(770, 771, 0, 1); // GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA
                GlStateManager.disableLighting();
                GlStateManager.disableFog();
                GlStateManager.disableBlend();

                Minecraft.getMinecraft().getFramebuffer().bindFramebufferTexture();
                GlStateManager.color(1f, 1f, 1f, 1f);
                drawTexturedRectNoBlend(0f, 0f, width / sr.getScaleFactor(), height / sr.getScaleFactor(),
                        0f, 1f, 0f, 1f, 9728);

                GlStateManager.enableBlend();
                blurBufferMain.bindFramebufferTexture();
                GlStateManager.color(1f, 1f, 1f, multiplier.getValue().floatValue() / 10 - 0.1f);
                drawTexturedRectNoBlend(0f, 0f, width / sr.getScaleFactor(), height / sr.getScaleFactor(),
                        0f, 1f, 1f, 0f, 9728);

                Minecraft.getMinecraft().getFramebuffer().bindFramebuffer(true);
                blurBufferInto.bindFramebufferTexture();
                GlStateManager.color(1f, 1f, 1f, 1f);
                GlStateManager.enableBlend();
                OpenGlHelper.glBlendFunc(770, 771, 1, 771);

                drawTexturedRectNoBlend(0f, 0f, width / sr.getScaleFactor(), height / sr.getScaleFactor(),
                        0f, 1f, 0f, 1f, 9728);

                Framebuffer tempBuff = blurBufferMain;
                blurBufferMain = blurBufferInto;
                blurBufferInto = tempBuff;
            }
        }
    }
}

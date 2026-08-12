package top.fpsmaster.features.impl.render;

import top.fpsmaster.utils.render.FastRender;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.client.shader.Shader;
import net.minecraft.client.shader.ShaderGroup;
import net.minecraft.client.shader.ShaderUniform;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;
import top.fpsmaster.FPSMaster;
import top.fpsmaster.event.Subscribe;
import top.fpsmaster.event.events.EventMotionBlur;
import top.fpsmaster.features.manager.Category;
import top.fpsmaster.features.manager.Module;
import top.fpsmaster.features.settings.impl.ModeSetting;
import top.fpsmaster.features.settings.impl.NumberSetting;
import top.fpsmaster.modules.logger.ClientLogger;
import top.fpsmaster.utils.system.OptifineUtil;
import top.fpsmaster.utils.core.Utility;

import java.lang.reflect.Field;
import java.util.List;

import static top.fpsmaster.utils.core.Utility.mc;

public class MotionBlur extends Module {
    // GLSL 120 only: 1.8.9 runs in a legacy/compatibility GL context, where a
    // #version 150 fragment shader is not guaranteed to compile (it never does on macOS)
    private static final String SHADER_PATH = "shaders/post/motionblur.json";
    private static final String SHADER_GROUP_NAME = "minecraft:" + SHADER_PATH;

    private static Framebuffer blurBufferMain;
    private static Framebuffer blurBufferInto;
    private static Field listShadersField;
    private static boolean listShadersUnavailable;

    private final ModeSetting mode = new ModeSetting("Mode", 1, "Old", "New");
    private final NumberSetting multiplier = new NumberSetting("Strength", 2, 0, 10, 0.5);

    private boolean shaderLoadFailed;

    public MotionBlur() {
        super("MotionBlur", Category.RENDER);
        addSettings(mode, multiplier);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        shaderLoadFailed = false;
        if (FastRender.isActive()) {
            OptifineUtil.setFastRender(false);
            Utility.sendClientNotify(FPSMaster.i18n.get("motionblur.fast_render"));
        }
    }

    private static Framebuffer checkFramebufferSizes(Framebuffer framebuffer, int width, int height) {
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

    private static void drawTexturedRectNoBlend(float x, float y, float width, float height,
                                                float uMin, float uMax, float vMin, float vMax, int filter) {
        GlStateManager.enableTexture2D();
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, filter);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, filter);
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldrenderer = tessellator.getWorldRenderer();
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
        if (mc.theWorld == null)
            return;

        if (mode.isMode("Old")) {
            if (Minecraft.getMinecraft().currentScreen == null) {
                if (isUsingShader())
                    Minecraft.getMinecraft().entityRenderer.stopUseShader();
                blur(multiplier.getValue().floatValue());
            }
        } else if (mode.isMode("New")) {
            if (mc.currentScreen != null || shaderLoadFailed)
                return;
            if (!isUsingShader()) {
                // vanilla loadShader replaces theShaderGroup without deleting the old
                // one, so any previous group must be freed here or its framebuffers leak
                mc.entityRenderer.stopUseShader();
                mc.entityRenderer.loadShader(new ResourceLocation(SHADER_PATH));
                // loadShader leaves GL framebuffer 0 bound mid-frame
                mc.getFramebuffer().bindFramebuffer(true);
                if (!isUsingShader()) {
                    // never retry: reloading every frame leaks a full ShaderGroup per frame
                    shaderLoadFailed = true;
                    ClientLogger.error("MotionBlur", "post shader failed to load, effect disabled");
                    return;
                }
            }
            float strength = 0.7f + multiplier.getValue().floatValue() / 100.0f * 3.0f - 0.01f;
            ShaderGroup shaderGroup = mc.entityRenderer.getShaderGroup();
            if (shaderGroup == null)
                return;
            List<Shader> listShaders = getListShaders(shaderGroup);
            if (listShaders == null)
                return;
            listShaders.forEach(it -> {
                ShaderUniform phosphor = it.getShaderManager().getShaderUniform("Phosphor");
                if (phosphor != null) {
                    phosphor.set(strength, 0, 0);
                }
            });
        }
    }

    @SuppressWarnings("unchecked")
    private static List<Shader> getListShaders(ShaderGroup shaderGroup) {
        if (listShadersUnavailable)
            return null;
        if (listShadersField == null) {
            try {
                listShadersField = ShaderGroup.class.getDeclaredField("listShaders");
            } catch (NoSuchFieldException e) {
                try {
                    // searge name in the remapped production jar
                    listShadersField = ShaderGroup.class.getDeclaredField("field_148031_d");
                } catch (NoSuchFieldException e2) {
                    listShadersUnavailable = true;
                    ClientLogger.error("MotionBlur", "ShaderGroup.listShaders field not found, strength setting will have no effect");
                    return null;
                }
            }
            listShadersField.setAccessible(true);
        }
        try {
            return (List<Shader>) listShadersField.get(shaderGroup);
        } catch (IllegalAccessException e) {
            listShadersUnavailable = true;
            ClientLogger.error("MotionBlur", "failed to read ShaderGroup.listShaders: " + e.getMessage());
            return null;
        }
    }

    private boolean isUsingShader() {
        EntityRenderer entityRenderer = mc.entityRenderer;
        return entityRenderer.isShaderActive() && entityRenderer.getShaderGroup() != null && entityRenderer.getShaderGroup().getShaderGroupName().equalsIgnoreCase(SHADER_GROUP_NAME);
    }

    @Override
    public void onDisable() {
        super.onDisable();
        Minecraft.getMinecraft().entityRenderer.stopUseShader();
        // Old-mode ping-pong FBOs are static and survive toggles; without an explicit delete the
        // driver's framebuffer memory stays allocated until process exit.
        deleteBlurBuffers();
    }

    private static void deleteBlurBuffers() {
        if (blurBufferMain != null) {
            blurBufferMain.deleteFramebuffer();
            blurBufferMain = null;
        }
        if (blurBufferInto != null) {
            blurBufferInto.deleteFramebuffer();
            blurBufferInto = null;
        }
    }

    public static void blur(float multiplier) {
        if (OpenGlHelper.isFramebufferEnabled()) {
            GlStateManager.matrixMode(GL11.GL_PROJECTION);
            GlStateManager.pushMatrix();
            GlStateManager.matrixMode(GL11.GL_MODELVIEW);
            GlStateManager.pushMatrix();
            try {
                ScaledResolution sr = new ScaledResolution(Minecraft.getMinecraft());
                int width = Minecraft.getMinecraft().getFramebuffer().framebufferWidth;
                int height = Minecraft.getMinecraft().getFramebuffer().framebufferHeight;
                // float division: with integer division the quads come up short by the
                // remainder pixels, leaving unprocessed rows/columns at the bottom/right edge
                float scaledWidth = (float) width / sr.getScaleFactor();
                float scaledHeight = (float) height / sr.getScaleFactor();

                GlStateManager.matrixMode(GL11.GL_PROJECTION);
                GlStateManager.loadIdentity();
                GlStateManager.ortho(0.0, scaledWidth, scaledHeight, 0.0, 2000.0, 4000.0);
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
                drawTexturedRectNoBlend(0f, 0f, scaledWidth, scaledHeight,
                        0f, 1f, 0f, 1f, 9728);

                GlStateManager.enableBlend();
                blurBufferMain.bindFramebufferTexture();
                GlStateManager.color(1f, 1f, 1f, multiplier / 10 - 0.1f);
                drawTexturedRectNoBlend(0f, 0f, scaledWidth, scaledHeight,
                        0f, 1f, 1f, 0f, 9728);

                Minecraft.getMinecraft().getFramebuffer().bindFramebuffer(true);
                blurBufferInto.bindFramebufferTexture();
                GlStateManager.color(1f, 1f, 1f, 1f);
                GlStateManager.enableBlend();
                OpenGlHelper.glBlendFunc(770, 771, 1, 771);

                drawTexturedRectNoBlend(0f, 0f, scaledWidth, scaledHeight,
                        0f, 1f, 0f, 1f, 9728);

                Framebuffer tempBuff = blurBufferMain;
                blurBufferMain = blurBufferInto;
                blurBufferInto = tempBuff;
            } finally {
                GlStateManager.matrixMode(GL11.GL_PROJECTION);
                GlStateManager.popMatrix();
                GlStateManager.matrixMode(GL11.GL_MODELVIEW);
                GlStateManager.popMatrix();

                // OpenGlHelper.glBlendFunc and bindFramebufferTexture write raw GL, bypassing
                // GlStateManager's cache. Push the equivalent state back *through* GlStateManager so
                // cache and driver agree. Using glPopAttrib here instead would revert the driver
                // behind the cache, silently turning the next enableBlend()/color() into a no-op.
                GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
                GlStateManager.enableBlend();
                GlStateManager.color(1f, 1f, 1f, 1f);
                GlStateManager.bindTexture(0);
            }
        }
    }
}




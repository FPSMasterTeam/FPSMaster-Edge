package top.fpsmaster.utils.render.shader;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.client.shader.ShaderGroup;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.EXTFramebufferObject;
import org.lwjgl.opengl.GL11;
import top.fpsmaster.features.impl.optimizes.Performance;
import top.fpsmaster.forge.api.IShaderGroup;
import top.fpsmaster.interfaces.ProviderManager;
import top.fpsmaster.utils.OptifineUtil;
import top.fpsmaster.utils.Utility;
import top.fpsmaster.utils.math.MathTimer;
import top.fpsmaster.utils.render.Render2DUtils;

import static top.fpsmaster.utils.Utility.mc;

public class BlurBuffer {
	private static ShaderGroup blurShader;
	private static Framebuffer buffer;
	private static float lastScale;
	private static int lastScaleWidth;
	private static int lastScaleHeight;
	private static final ResourceLocation shader = new ResourceLocation("shaders/post/blur.json");

	private static final MathTimer updateTimer = new MathTimer();

	public static void initFboAndShader() {
		if(OptifineUtil.isFastRender()){
			return;
		}
		try {
			buffer = new Framebuffer(mc.displayWidth, mc.displayHeight, false);
			buffer.setFramebufferColor(0.0F, 0.0F, 0.0F, 0.0F);

			blurShader = new ShaderGroup(mc.getTextureManager(), mc.getResourceManager(), buffer, shader);
			blurShader.createBindFramebuffers(mc.displayWidth, mc.displayHeight);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	private static void setShaderConfigs() {
		((IShaderGroup) blurShader).getListShaders().get(0).getShaderManager().getShaderUniform("Radius").set((float) 25);
		((IShaderGroup) blurShader).getListShaders().get(1).getShaderManager().getShaderUniform("Radius").set((float) 25);
		((IShaderGroup) blurShader).getListShaders().get(0).getShaderManager().getShaderUniform("BlurDir").set((float) 0.0, (float) 1.0);
		((IShaderGroup) blurShader).getListShaders().get(1).getShaderManager().getShaderUniform("BlurDir").set((float) 1.0, (float) 0.0);
	}

	public static void blurArea(float x, float y, float width, float height, boolean setupOverlay) {
		if (!blurEnabled())
			return;
		if(OptifineUtil.isFastRender()){
			return;
		}
		ScaledResolution scale = new ScaledResolution(mc);
		float factor = scale.getScaleFactor();
		int factor2 = scale.getScaledWidth();
		int factor3 = scale.getScaledHeight();

		if (lastScale != factor || lastScaleWidth != factor2 || lastScaleHeight != factor3 || buffer == null || blurShader == null) {
			initFboAndShader();
		}
		lastScale = factor;
		lastScaleWidth = factor2;
		lastScaleHeight = factor3;

		// 渲染
		GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT);
		GL11.glEnable(GL11.GL_STENCIL_TEST);
		GL11.glColorMask(false, false, false, false);
		GL11.glDepthMask(false);
		GL11.glStencilFunc(GL11.GL_NEVER, 1, 0xFF);
		GL11.glStencilOp(GL11.GL_REPLACE, GL11.GL_KEEP, GL11.GL_KEEP);
		GL11.glStencilMask(0xFF);
		GL11.glClear(GL11.GL_STENCIL_BUFFER_BIT);
		Render2DUtils.drawRect(x, y, width, height, 0xff000000);
		GL11.glColorMask(true, true, true, true);
		GL11.glDepthMask(true);
		GL11.glStencilMask(0x00);
		GL11.glStencilFunc(GL11.GL_EQUAL, 0, 0xFF);
		/* (nothing to draw) */
		GL11.glStencilFunc(GL11.GL_EQUAL, 1, 0xFF);
		buffer.framebufferRenderExt(Minecraft.getMinecraft().displayWidth, Minecraft.getMinecraft().displayHeight, true);
		GL11.glDisable(GL11.GL_STENCIL_TEST);

		if (setupOverlay) {
			mc.entityRenderer.setupOverlayRendering();
		}

		GlStateManager.enableDepth();
	}

	public static void blurRoundArea(float x, float y, float width, float height, float roundRadius, boolean setupOverlay) {
		ScaledResolution scale = new ScaledResolution(mc);
		float factor = scale.getScaleFactor();
		int factor2 = scale.getScaledWidth();
		int factor3 = scale.getScaledHeight();

		if (lastScale != factor || lastScaleWidth != factor2 || lastScaleHeight != factor3 || buffer == null || blurShader == null) {
			initFboAndShader();
		}

		lastScale = factor;
		lastScaleWidth = factor2;
		lastScaleHeight = factor3;

		// 渲染
		GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT);
		GL11.glEnable(GL11.GL_STENCIL_TEST);
		GL11.glColorMask(false, false, false, false);
		GL11.glDepthMask(false);
		GL11.glStencilFunc(GL11.GL_NEVER, 1, 0xFF);
		GL11.glStencilOp(GL11.GL_REPLACE, GL11.GL_KEEP, GL11.GL_KEEP);

		GL11.glStencilMask(0xFF);
		GL11.glClear(GL11.GL_STENCIL_BUFFER_BIT);
//		RenderUtil.drawRoundedRect(x, y, width, height, roundRadius, 0xff000000, 1, 0xff000000);

		Render2DUtils.drawRect(x, y, width, height, 0xff000000);
		GL11.glColorMask(true, true, true, true);
		GL11.glDepthMask(true);
		GL11.glStencilMask(0x00);
		GL11.glStencilFunc(GL11.GL_EQUAL, 0, 0xFF);
		/* (nothing to draw) */
		GL11.glStencilFunc(GL11.GL_EQUAL, 1, 0xFF);
		buffer.framebufferRenderExt(Minecraft.getMinecraft().displayWidth, Minecraft.getMinecraft().displayHeight, true);
		GL11.glDisable(GL11.GL_STENCIL_TEST);

		if (setupOverlay) {
			mc.entityRenderer.setupOverlayRendering();
		}

		GlStateManager.enableDepth();
	}

	public static void updateBlurBuffer(boolean setupOverlay) {
		// 以60帧每秒的速度更新 FrameBuffer
		if(OptifineUtil.isFastRender()){
			return;
		}
		if (updateTimer.delay((long) (1000 / 60f)) && blurShader != null) {
			mc.getFramebuffer().unbindFramebuffer();

			setShaderConfigs();
			buffer.bindFramebuffer(true);

			mc.getFramebuffer().framebufferRenderExt(mc.displayWidth, mc.displayHeight, true);

			if(blurEnabled()) {
				GlStateManager.matrixMode(5890);
				GlStateManager.pushMatrix();
				GlStateManager.loadIdentity();
				((IShaderGroup) blurShader).load(ProviderManager.timerProvider.getRenderPartialTicks());
				GlStateManager.popMatrix();
			}

			buffer.unbindFramebuffer();
			mc.getFramebuffer().bindFramebuffer(true);

			if(mc.getFramebuffer().depthBuffer > -1) {
				setupFBO(mc.getFramebuffer());
				mc.getFramebuffer().depthBuffer = -1;
			}

			if (setupOverlay) {
				mc.entityRenderer.setupOverlayRendering();
			}
		}
	}

	public static void setupFBO(Framebuffer fbo) {
		EXTFramebufferObject.glDeleteRenderbuffersEXT(fbo.depthBuffer);
		int stencil_depth_buffer_ID = EXTFramebufferObject.glGenRenderbuffersEXT();
		EXTFramebufferObject.glBindRenderbufferEXT(36161, stencil_depth_buffer_ID);
		EXTFramebufferObject.glRenderbufferStorageEXT(36161, 34041, Minecraft.getMinecraft().displayWidth, Minecraft.getMinecraft().displayHeight);
		EXTFramebufferObject.glFramebufferRenderbufferEXT(36160, 36128, 36161, stencil_depth_buffer_ID);
		EXTFramebufferObject.glFramebufferRenderbufferEXT(36160, 36096, 36161, stencil_depth_buffer_ID);
	}

	public static boolean blurEnabled() {
		if (!Performance.Companion.getBlur().getValue())
			return false;
		if (ProviderManager.mcProvider.getPlayer() == null)
			return false;

		if (OpenGlHelper.shadersSupported) {
            return ProviderManager.mcProvider.getWorld() == null || Minecraft.getMinecraft().getRenderViewEntity() instanceof EntityPlayer;
        }
		return false;
	}
}

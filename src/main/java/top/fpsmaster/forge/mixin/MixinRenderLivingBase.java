package top.fpsmaster.forge.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.entity.RendererLivingEntity;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.entity.EntityLivingBase;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import top.fpsmaster.features.impl.render.HitColor;
import top.fpsmaster.features.impl.utility.LevelTag;

import java.nio.FloatBuffer;

@Mixin(RendererLivingEntity.class)
public abstract class MixinRenderLivingBase {
    @Inject(method = "canRenderName(Lnet/minecraft/entity/EntityLivingBase;)Z", at = @At("HEAD"), cancellable = true)
    public void tag(EntityLivingBase entity, CallbackInfoReturnable<Boolean> cir) {
        if (LevelTag.using && LevelTag.showSelf.getValue()) {
            EntityPlayerSP entityplayersp = Minecraft.getMinecraft().thePlayer;
            boolean flag = !entity.isInvisibleToPlayer(entityplayersp);

            cir.setReturnValue(flag);
        }
    }

    @Shadow
    protected abstract int getColorMultiplier(EntityLivingBase entitylivingbaseIn, float lightBrightness, float partialTickTime);

    @Shadow
    protected FloatBuffer brightnessBuffer;

    @Shadow
    @Final
    private static DynamicTexture textureBrightness;

    @Inject(method = "setBrightness", at = @At("HEAD"), cancellable = true)
    protected void setBrightness(EntityLivingBase entitylivingbaseIn, float partialTicks, boolean combineTextures, CallbackInfoReturnable<Boolean> cir) {
        if (HitColor.using) {
            float f = entitylivingbaseIn.getBrightness(partialTicks);
            int i = this.getColorMultiplier(entitylivingbaseIn, f, partialTicks);
            boolean flag = (i >> 24 & 255) > 0;
            boolean flag1 = entitylivingbaseIn.hurtTime > 0 || entitylivingbaseIn.deathTime > 0;
            if (!flag && !flag1) {
                cir.setReturnValue(false);
            } else if (!flag && !combineTextures) {
                cir.setReturnValue(false);
            } else {
                GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);
                GlStateManager.enableTexture2D();
                GL11.glTexEnvi(8960, 8704, OpenGlHelper.GL_COMBINE);
                GL11.glTexEnvi(8960, OpenGlHelper.GL_COMBINE_RGB, 8448);
                GL11.glTexEnvi(8960, OpenGlHelper.GL_SOURCE0_RGB, OpenGlHelper.defaultTexUnit);
                GL11.glTexEnvi(8960, OpenGlHelper.GL_SOURCE1_RGB, OpenGlHelper.GL_PRIMARY_COLOR);
                GL11.glTexEnvi(8960, OpenGlHelper.GL_OPERAND0_RGB, 768);
                GL11.glTexEnvi(8960, OpenGlHelper.GL_OPERAND1_RGB, 768);
                GL11.glTexEnvi(8960, OpenGlHelper.GL_COMBINE_ALPHA, 7681);
                GL11.glTexEnvi(8960, OpenGlHelper.GL_SOURCE0_ALPHA, OpenGlHelper.defaultTexUnit);
                GL11.glTexEnvi(8960, OpenGlHelper.GL_OPERAND0_ALPHA, 770);
                GlStateManager.setActiveTexture(OpenGlHelper.lightmapTexUnit);
                GlStateManager.enableTexture2D();
                GL11.glTexEnvi(8960, 8704, OpenGlHelper.GL_COMBINE);
                GL11.glTexEnvi(8960, OpenGlHelper.GL_COMBINE_RGB, OpenGlHelper.GL_INTERPOLATE);
                GL11.glTexEnvi(8960, OpenGlHelper.GL_SOURCE0_RGB, OpenGlHelper.GL_CONSTANT);
                GL11.glTexEnvi(8960, OpenGlHelper.GL_SOURCE1_RGB, OpenGlHelper.GL_PREVIOUS);
                GL11.glTexEnvi(8960, OpenGlHelper.GL_SOURCE2_RGB, OpenGlHelper.GL_CONSTANT);
                GL11.glTexEnvi(8960, OpenGlHelper.GL_OPERAND0_RGB, 768);
                GL11.glTexEnvi(8960, OpenGlHelper.GL_OPERAND1_RGB, 768);
                GL11.glTexEnvi(8960, OpenGlHelper.GL_OPERAND2_RGB, 770);
                GL11.glTexEnvi(8960, OpenGlHelper.GL_COMBINE_ALPHA, 7681);
                GL11.glTexEnvi(8960, OpenGlHelper.GL_SOURCE0_ALPHA, OpenGlHelper.GL_PREVIOUS);
                GL11.glTexEnvi(8960, OpenGlHelper.GL_OPERAND0_ALPHA, 770);
                this.brightnessBuffer.position(0);
                if (flag1) {
                    if (HitColor.using) {
                        this.brightnessBuffer.put(HitColor.getRed());
                        this.brightnessBuffer.put(HitColor.getGreen());
                        this.brightnessBuffer.put(HitColor.getBlue());
                        this.brightnessBuffer.put(HitColor.getAlpha());
                    } else {
                        this.brightnessBuffer.put(1.0F);
                        this.brightnessBuffer.put(0.0F);
                        this.brightnessBuffer.put(0.0F);
                        this.brightnessBuffer.put(0.3F);
                    }
                } else {
                    float f1 = (float) (i >> 24 & 255) / 255.0F;
                    float f2 = (float) (i >> 16 & 255) / 255.0F;
                    float f3 = (float) (i >> 8 & 255) / 255.0F;
                    float f4 = (float) (i & 255) / 255.0F;
                    this.brightnessBuffer.put(f2);
                    this.brightnessBuffer.put(f3);
                    this.brightnessBuffer.put(f4);
                    this.brightnessBuffer.put(1.0F - f1);
                }

                this.brightnessBuffer.flip();
                GL11.glTexEnv(8960, 8705, this.brightnessBuffer);
                GlStateManager.setActiveTexture(OpenGlHelper.GL_TEXTURE2);
                GlStateManager.enableTexture2D();
                GlStateManager.bindTexture(textureBrightness.getGlTextureId());
                GL11.glTexEnvi(8960, 8704, OpenGlHelper.GL_COMBINE);
                GL11.glTexEnvi(8960, OpenGlHelper.GL_COMBINE_RGB, 8448);
                GL11.glTexEnvi(8960, OpenGlHelper.GL_SOURCE0_RGB, OpenGlHelper.GL_PREVIOUS);
                GL11.glTexEnvi(8960, OpenGlHelper.GL_SOURCE1_RGB, OpenGlHelper.lightmapTexUnit);
                GL11.glTexEnvi(8960, OpenGlHelper.GL_OPERAND0_RGB, 768);
                GL11.glTexEnvi(8960, OpenGlHelper.GL_OPERAND1_RGB, 768);
                GL11.glTexEnvi(8960, OpenGlHelper.GL_COMBINE_ALPHA, 7681);
                GL11.glTexEnvi(8960, OpenGlHelper.GL_SOURCE0_ALPHA, OpenGlHelper.GL_PREVIOUS);
                GL11.glTexEnvi(8960, OpenGlHelper.GL_OPERAND0_ALPHA, 770);
                GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);
                cir.setReturnValue(true);
            }
        }
    }
}

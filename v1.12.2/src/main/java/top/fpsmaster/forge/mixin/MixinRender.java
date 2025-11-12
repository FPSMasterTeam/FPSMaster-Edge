package top.fpsmaster.forge.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.fpsmaster.FPSMaster;
import top.fpsmaster.features.impl.utility.LevelTag;
import top.fpsmaster.api.ProviderManager;
import top.fpsmaster.modules.client.PlayerManager;
import top.fpsmaster.features.impl.optimizes.CheckEntity;
import top.fpsmaster.features.impl.optimizes.Performance;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL11.GL_DEPTH_TEST;
import static top.fpsmaster.utils.Utility.mc;
import static top.fpsmaster.utils.render.Render2DUtils.drawModalRectWithCustomSizedTexture;

@Mixin(Render.class)
public abstract class MixinRender {
    protected MixinRender() {
    }

    @Shadow
    protected abstract boolean bindEntityTexture(Entity entity);
    @Shadow
    protected boolean renderOutlines;
    @Shadow
    protected abstract int getTeamColor(Entity entityIn);
    @Final
    @Shadow
    protected RenderManager renderManager;
    @Shadow
    public void doRender(Entity entity, double x, double y, double z, float entityYaw, float partialTicks){}

    @Shadow
    public abstract void bindTexture(ResourceLocation location);

    @Inject(method = "doRender(Lnet/minecraft/entity/Entity;DDDFF)V", at = @At("HEAD"),cancellable = true)
    public void preRender(Entity entity, double x, double y, double z, float entityYaw, float partialTicks, CallbackInfo ci){
        if (Performance.using && entity != Minecraft.getMinecraft().player) {
            if (Performance.entitiesOptimize.getValue()) {
                if (!Performance.isVisible(new CheckEntity(entity))) {
                    ci.cancel();
                }
            }
        }
    }

    @Inject(method = "renderLivingLabel", at = @At("HEAD"), cancellable = true)
    protected void renderLivingLabel(Entity entityIn, String str, double x, double y, double z, int maxDistance, CallbackInfo ci) {
        if (LevelTag.using && LevelTag.health.getValue()) {
            double d = entityIn.getDistanceSq(this.renderManager.renderViewEntity);
            if (d < 100) {
                float f = 1.6F;
                float g = 0.016666668F * f;
                GlStateManager.pushMatrix();
                GlStateManager.translate((float) x + 0.0F, (float) y + entityIn.height + 0.5F, (float) z);
                GL11.glNormal3f(0.0F, 1.0F, 0.0F);
                GlStateManager.rotate(-this.renderManager.playerViewY, 0.0F, 1.0F, 0.0F);
                GlStateManager.rotate(this.renderManager.playerViewX, 1.0F, 0.0F, 0.0F);
                GlStateManager.scale(-g, -g, g);
                GlStateManager.disableLighting();
                GlStateManager.depthMask(false);
                GlStateManager.disableDepth();
                GlStateManager.enableBlend();
                GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
                ProviderManager.guiIngameProvider.drawHealth(entityIn);
                GlStateManager.disableTexture2D();
                GlStateManager.enableTexture2D();
                GlStateManager.enableLighting();
                GlStateManager.disableBlend();
                GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
                GlStateManager.popMatrix();
            }
        }
    }


    @Inject(method = "renderName",at = @At("HEAD"), cancellable = true)
    public void ignore(Entity entity, double x, double y, double z, CallbackInfo ci){
        if (Performance.using && Performance.ignoreStands.getValue() && entity instanceof EntityArmorStand) {
            ci.cancel();
        }
    }

}

package top.fpsmaster.forge.mixin;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.fpsmaster.features.impl.optimizes.Performance;
import top.fpsmaster.features.impl.utility.LevelTag;

import static top.fpsmaster.utils.core.Utility.mc;

@Mixin(Render.class)
public abstract class MixinRender {
    protected MixinRender() {
    }

    @Shadow
    protected abstract boolean bindEntityTexture(Entity entity);

    @Shadow
    public abstract void bindTexture(ResourceLocation location);

    @Final
    @Shadow
    protected RenderManager renderManager;

    @Shadow
    public void doRender(Entity entity, double x, double y, double z, float entityYaw, float partialTicks) {
    }


    @Inject(method = "renderLivingLabel", at = @At("HEAD"), cancellable = true)
    protected void renderLivingLabel(Entity entityIn, String str, double x, double y, double z, int maxDistance, CallbackInfo ci) {
        if (LevelTag.health.getValue())
            LevelTag.renderHealth(entityIn, str, x, y, z, maxDistance);
        LevelTag.renderName(entityIn, str, x, y, z, maxDistance);
        ci.cancel();
    }

    @Inject(method = "renderName", at = @At("HEAD"), cancellable = true)
    public void ignore(Entity entity, double x, double y, double z, CallbackInfo ci) {
        if (Performance.using && Performance.ignoreStands.getValue() && entity instanceof EntityArmorStand) {
            ci.cancel();
        }
    }

}




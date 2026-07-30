package top.fpsmaster.forge.mixin;

import net.minecraft.client.renderer.entity.RenderArrow;
import net.minecraft.entity.projectile.EntityArrow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.fpsmaster.features.impl.optimizes.Performance;
import top.fpsmaster.forge.mixin.accessor.EntityArrowAccessor;

/**
 * Skips arrows that have landed.
 *
 * <p>Only the landed ones. An arrow in flight is information — where it came from, where it is
 * going — and there are never many at once; the ones worth removing are the fifty stuck in the floor
 * of a bridge fight, which stay for a minute each and tell nobody anything.
 */
@Mixin(RenderArrow.class)
public class RenderArrowMixin_HideGround {

    @Inject(method = "doRender(Lnet/minecraft/entity/projectile/EntityArrow;DDDFF)V",
            at = @At("HEAD"), cancellable = true)
    private void fpsmaster$hideGroundArrows(EntityArrow entity, double x, double y, double z,
                                            float entityYaw, float partialTicks, CallbackInfo ci) {
        if (Performance.using && Performance.hideGroundArrows.getValue()
                && ((EntityArrowAccessor) entity).fpsmaster$isInGround()) {
            ci.cancel();
        }
    }
}

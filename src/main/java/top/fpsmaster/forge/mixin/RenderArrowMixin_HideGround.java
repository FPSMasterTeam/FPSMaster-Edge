package top.fpsmaster.forge.mixin;

import net.minecraft.client.renderer.entity.RenderArrow;
import net.minecraft.entity.projectile.EntityArrow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.fpsmaster.features.impl.optimizes.Performance;
import top.fpsmaster.utils.render.ArrowState;

/**
 * Skips arrows that have landed.
 *
 * <p>The {@code inGround} read goes through {@link ArrowState} rather than casting here. Mixin 0.7
 * resolves a cast to another mixin's interface against this mixin's own target, so the direct cast
 * failed and the whole mixin was dropped at load with a warning — which is how this switch shipped
 * doing nothing at all.
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
                && ArrowState.isInGround(entity)) {
            ci.cancel();
        }
    }
}

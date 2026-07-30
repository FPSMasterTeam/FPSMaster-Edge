package top.fpsmaster.forge.mixin;

import net.minecraft.client.renderer.entity.layers.LayerArrow;
import net.minecraft.entity.EntityLivingBase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.fpsmaster.features.impl.optimizes.Performance;

/**
 * Skips the arrows sticking out of someone who has been hit.
 *
 * <p>Each one is a separate model with its own matrix stack and its own draw, and a player in a
 * ranged fight can be carrying several. The layer draws nothing else, so cancelling it is the whole
 * feature.
 */
@Mixin(LayerArrow.class)
public class LayerArrowMixin_HideStuck {

    @Inject(method = "doRenderLayer", at = @At("HEAD"), cancellable = true)
    private void fpsmaster$hideStuckArrows(EntityLivingBase entity, float limbSwing,
                                           float limbSwingAmount, float partialTicks, float ageInTicks,
                                           float netHeadYaw, float headPitch, float scale,
                                           CallbackInfo ci) {
        if (Performance.using && Performance.hideStuckArrows.getValue()) {
            ci.cancel();
        }
    }
}

package top.fpsmaster.forge.mixin;

import net.minecraft.client.renderer.entity.layers.LayerHeldItem;
import net.minecraft.entity.EntityLivingBase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.fpsmaster.benchmark.BenchCounters;
import top.fpsmaster.benchmark.BenchmarkMode;
import top.fpsmaster.benchmark.Experiments;

/**
 * Ceiling probe for the held-item layer.
 *
 * <p>Separate from the armour probe because the two cost different things: armour renders a biped
 * model per piece, the held item goes through the item model pipeline with its own texture binds and
 * transforms. Splitting them is what decides which one is worth work.
 */
@Mixin(LayerHeldItem.class)
public class LayerHeldItemMixin_Probe {

    @Inject(method = "doRenderLayer", at = @At("HEAD"), cancellable = true)
    private void fpsmasterHeldItemProbe(EntityLivingBase entity, float limbSwing, float limbSwingAmount,
                                        float partialTicks, float ageInTicks, float netHeadYaw,
                                        float headPitch, float scale, CallbackInfo ci) {
        if (!BenchmarkMode.ACTIVE) {
            return;
        }
        if (Experiments.active(Experiments.NO_HELD_ITEM)) {
            ci.cancel();
            return;
        }
        BenchCounters.heldItemLayerRenders++;
    }
}

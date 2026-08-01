package top.fpsmaster.forge.mixin;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.renderer.entity.layers.LayerArmorBase;
import net.minecraft.entity.EntityLivingBase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.fpsmaster.benchmark.BenchCounters;
import top.fpsmaster.benchmark.BenchmarkMode;
import top.fpsmaster.benchmark.Experiments;

/**
 * Counts and, under a ceiling probe, deletes the worn-armour layer.
 *
 * <p>On a recorded Hypixel match the per-entity layer stack is the largest section of the frame,
 * and armour is the part of it that scales with how well equipped the players are. Vanilla draws an
 * enchanted piece three times — the piece itself, then the glint twice, each pass reloading the
 * texture matrix — so a fully enchanted set costs far more than the same set unenchanted. Whether
 * that is worth attacking is a question about this specific workload, so it is measured here rather
 * than assumed.
 */
@Mixin(LayerArmorBase.class)
public class LayerArmorBaseMixin_Probe {

    @Inject(method = "doRenderLayer", at = @At("HEAD"), cancellable = true)
    private void fpsmasterArmorLayerProbe(EntityLivingBase entity, float limbSwing, float limbSwingAmount,
                                          float partialTicks, float ageInTicks, float netHeadYaw,
                                          float headPitch, float scale, CallbackInfo ci) {
        if (!BenchmarkMode.ACTIVE) {
            return;
        }
        if (Experiments.active(Experiments.NO_ARMOR)) {
            ci.cancel();
            return;
        }
        BenchCounters.armorLayerRenders++;
    }

    @Inject(method = "renderLayer", at = @At("HEAD"))
    private void fpsmasterArmorPieceProbe(EntityLivingBase entity, float limbSwing, float limbSwingAmount,
                                          float partialTicks, float ageInTicks, float netHeadYaw,
                                          float headPitch, float scale, int slot, CallbackInfo ci) {
        if (BenchmarkMode.ACTIVE) {
            BenchCounters.armorPiecesRendered++;
        }
    }

    @Inject(method = "renderGlint", at = @At("HEAD"), cancellable = true)
    private void fpsmasterGlintProbe(EntityLivingBase entity, ModelBase model, float limbSwing,
                                     float limbSwingAmount, float partialTicks, float ageInTicks,
                                     float netHeadYaw, float headPitch, float scale, CallbackInfo ci) {
        if (!BenchmarkMode.ACTIVE) {
            return;
        }
        if (Experiments.active(Experiments.NO_GLINT)) {
            ci.cancel();
            return;
        }
        // Two model renders per call; counting them rather than the calls keeps the number
        // comparable with the model renders the rest of the entity pass performs.
        BenchCounters.armorGlintModelRenders += 2;
    }
}

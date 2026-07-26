package top.fpsmaster.forge.mixin;

import net.minecraft.client.renderer.entity.RendererLivingEntity;
import net.minecraft.entity.EntityLivingBase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import top.fpsmaster.benchmark.BenchProfiler;
import top.fpsmaster.benchmark.BenchmarkMode;

/**
 * Splits the per-entity render into its phases.
 *
 * <p>The entity pass is 46.9% of an entity-dense frame and 82% of that is inside the per-entity
 * render — 9.6us per armour stand, roughly 30,000 cycles for about a dozen boxes. That ratio says
 * the cost is fixed overhead rather than geometry, but "fixed overhead" is not something you can
 * optimise until you know which part of it. These brackets separate the model, the layer stack, the
 * brightness lookup and the shadow.
 */
@Mixin(RendererLivingEntity.class)
public class RendererLivingEntityMixin_SectionTiming {

    @Inject(method = "renderModel", at = @At("HEAD"))
    private void fpsmasterBeginModel(EntityLivingBase entity, float limbSwing, float limbSwingAmount,
                                     float ageInTicks, float netHeadYaw, float headPitch, float scale,
                                     CallbackInfo ci) {
        if (BenchmarkMode.ACTIVE) {
            BenchProfiler.begin(BenchProfiler.SECTION_ENTITY_MODEL);
        }
    }

    @Inject(method = "renderModel", at = @At("RETURN"))
    private void fpsmasterEndModel(EntityLivingBase entity, float limbSwing, float limbSwingAmount,
                                   float ageInTicks, float netHeadYaw, float headPitch, float scale,
                                   CallbackInfo ci) {
        if (BenchmarkMode.ACTIVE) {
            BenchProfiler.end(BenchProfiler.SECTION_ENTITY_MODEL);
        }
    }

    @Inject(method = "renderLayers", at = @At("HEAD"))
    private void fpsmasterBeginLayers(EntityLivingBase entity, float limbSwing, float limbSwingAmount,
                                      float partialTicks, float ageInTicks, float netHeadYaw,
                                      float headPitch, float scale, CallbackInfo ci) {
        if (BenchmarkMode.ACTIVE) {
            BenchProfiler.begin(BenchProfiler.SECTION_ENTITY_LAYERS);
        }
    }

    @Inject(method = "renderLayers", at = @At("RETURN"))
    private void fpsmasterEndLayers(EntityLivingBase entity, float limbSwing, float limbSwingAmount,
                                    float partialTicks, float ageInTicks, float netHeadYaw,
                                    float headPitch, float scale, CallbackInfo ci) {
        if (BenchmarkMode.ACTIVE) {
            BenchProfiler.end(BenchProfiler.SECTION_ENTITY_LAYERS);
        }
    }

    @Inject(method = "setBrightness", at = @At("HEAD"))
    private void fpsmasterBeginBrightness(EntityLivingBase entity, float partialTicks,
                                          boolean combineTextures, CallbackInfoReturnable<Boolean> cir) {
        if (BenchmarkMode.ACTIVE) {
            BenchProfiler.begin(BenchProfiler.SECTION_ENTITY_BRIGHTNESS);
        }
    }

    @Inject(method = "setBrightness", at = @At("RETURN"))
    private void fpsmasterEndBrightness(EntityLivingBase entity, float partialTicks,
                                        boolean combineTextures, CallbackInfoReturnable<Boolean> cir) {
        if (BenchmarkMode.ACTIVE) {
            BenchProfiler.end(BenchProfiler.SECTION_ENTITY_BRIGHTNESS);
        }
    }
}

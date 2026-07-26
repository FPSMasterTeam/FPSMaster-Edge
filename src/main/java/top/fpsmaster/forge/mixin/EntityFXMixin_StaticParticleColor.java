package top.fpsmaster.forge.mixin;

import net.minecraft.client.particle.EntityFX;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import top.fpsmaster.benchmark.BenchCounters;
import top.fpsmaster.benchmark.BenchmarkMode;
import top.fpsmaster.features.impl.optimizes.Performance;

@Mixin(EntityFX.class)
public class EntityFXMixin_StaticParticleColor {
    @Redirect(method = "renderParticle", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/particle/EntityFX;getBrightnessForRender(F)I"))
    private int edge$staticParticleColor(EntityFX entityFX, float partialTicks) {
        if (Performance.using && Performance.staticParticleColor.getValue()) {
            if (BenchmarkMode.ACTIVE) {
                BenchCounters.staticParticleColorHits++;
            }
            return 15728880;
        }
        return entityFX.getBrightnessForRender(partialTicks);
    }
}




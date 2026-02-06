package top.fpsmaster.forge.mixin;

import net.minecraft.client.particle.EntityFX;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import top.fpsmaster.features.impl.optimizes.Performance;

@Mixin(EntityFX.class)
public class EntityFXMixin_StaticParticleColor {
    @Redirect(method = "renderParticle", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/particle/EntityFX;getBrightnessForRender(F)I"))
    private int patcher$staticParticleColor(EntityFX entityFX, float partialTicks) {
        return Performance.staticParticleColor.getValue() ? 15728880 : entityFX.getBrightnessForRender(partialTicks);
    }
}




package top.fpsmaster.forge.mixin;

import com.google.common.collect.Lists;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.EffectRenderer;
import net.minecraft.client.particle.EntityParticleEmitter;
import net.minecraft.entity.Entity;
import net.minecraft.util.EnumParticleTypes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import top.fpsmaster.features.impl.optimizes.Performance;

import java.util.List;

@Mixin(EffectRenderer.class)
public class MixinParticleManager {

    @Shadow
    private List<EntityParticleEmitter> particleEmitters = Lists.newArrayList();

    /**
     * @author SuperSkidder
     * @reason particles limit
     */
    @Overwrite
    public void emitParticleAtEntity(Entity entityIn, EnumParticleTypes particleTypes) {
        if (Performance.using && particleEmitters.size() > Performance.particlesLimit.getValue().intValue()) {
            this.particleEmitters.remove(0);
        }
        this.particleEmitters.add(new EntityParticleEmitter(Minecraft.getMinecraft().theWorld, entityIn, particleTypes));
    }
}




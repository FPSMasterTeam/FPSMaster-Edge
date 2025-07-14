package top.fpsmaster.forge.mixin;

import net.minecraft.client.multiplayer.WorldClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import top.fpsmaster.features.impl.optimizes.Performance;

@Mixin(WorldClient.class)
public class WorldClientMixin_AnimationTick {
    @ModifyConstant(method = "doVoidFogParticles", constant = @Constant(intValue = 1000))
    private int patcher$lowerTickCount(int original) {
        return Performance.lowAnimationTick.getValue() ? 100 : original;
    }
}
package top.fpsmaster.forge.mixin;

import net.minecraft.block.BlockLiquid;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import top.fpsmaster.features.impl.optimizes.Performance;

/**
 * Stops lava throwing its ambient sparks.
 *
 * <p>Cut where the particle would be created rather than where it would be drawn, so nothing is
 * allocated, ticked, stored or later swept. A lava lake spawns these continuously for as long as it
 * is in render distance.
 *
 * <p>Filtered by particle type instead of by call position: {@code randomDisplayTick} also spawns
 * water suspension and both drip types, and an ordinal would silently move onto one of those the
 * next time the method changed shape.
 */
@Mixin(BlockLiquid.class)
public class BlockLiquidMixin_HideLavaParticles {

    @Redirect(method = "randomDisplayTick",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/World;spawnParticle(Lnet/minecraft/util/EnumParticleTypes;DDDDDD[I)V"))
    private void fpsmaster$skipLavaSpark(World world, EnumParticleTypes type, double x, double y, double z,
                                         double xOffset, double yOffset, double zOffset, int[] arguments) {
        if (Performance.using && Performance.hideLavaParticles.getValue()
                && type == EnumParticleTypes.LAVA) {
            return;
        }
        world.spawnParticle(type, x, y, z, xOffset, yOffset, zOffset, arguments);
    }
}

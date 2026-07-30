package top.fpsmaster.forge.mixin;

import net.minecraft.block.BlockPortal;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import top.fpsmaster.features.impl.optimizes.Performance;

/**
 * Stops a nether portal throwing particles, without silencing it.
 *
 * <p>A portal emits four particles per display tick per block, and a portal is at least six blocks.
 * The sound is left alone — it is on a separate branch of the same method, it costs nothing, and it
 * is how a player knows the portal is there when they cannot see it.
 */
@Mixin(BlockPortal.class)
public class BlockPortalMixin_HideParticles {

    @Redirect(method = "randomDisplayTick",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/World;spawnParticle(Lnet/minecraft/util/EnumParticleTypes;DDDDDD[I)V"))
    private void fpsmaster$skipPortalParticle(World world, EnumParticleTypes type, double x, double y, double z,
                                              double xOffset, double yOffset, double zOffset, int[] arguments) {
        if (Performance.using && Performance.hidePortalParticles.getValue()) {
            return;
        }
        world.spawnParticle(type, x, y, z, xOffset, yOffset, zOffset, arguments);
    }
}

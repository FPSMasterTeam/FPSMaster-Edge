package top.fpsmaster.forge.mixin;

import net.minecraft.tileentity.MobSpawnerBaseLogic;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import top.fpsmaster.features.impl.optimizes.Performance;

/**
 * Stops a spawner smoking.
 *
 * <p>Only the two particle calls are redirected, not the method. The client's half of
 * {@code updateSpawner} also counts the spawn delay down and advances the rotation of the mob inside
 * — cancelling it at the head would freeze the mob mid-turn and stop the spawner ever appearing to
 * do anything.
 */
@Mixin(MobSpawnerBaseLogic.class)
public class MobSpawnerBaseLogicMixin_HideParticles {

    @Redirect(method = "updateSpawner",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/World;spawnParticle(Lnet/minecraft/util/EnumParticleTypes;DDDDDD[I)V"))
    private void fpsmaster$skipSpawnerParticle(World world, EnumParticleTypes type, double x, double y, double z,
                                               double xOffset, double yOffset, double zOffset, int[] arguments) {
        if (Performance.using && Performance.hideSpawnerParticles.getValue()) {
            return;
        }
        world.spawnParticle(type, x, y, z, xOffset, yOffset, zOffset, arguments);
    }
}

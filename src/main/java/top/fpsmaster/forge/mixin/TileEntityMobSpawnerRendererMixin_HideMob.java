package top.fpsmaster.forge.mixin;

import net.minecraft.client.renderer.tileentity.TileEntityMobSpawnerRenderer;
import net.minecraft.tileentity.TileEntityMobSpawner;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.fpsmaster.features.impl.optimizes.Performance;

/**
 * Skips the miniature mob spinning inside a spawner.
 *
 * <p>The most expensive thing in this group by a distance. That mob goes through the whole entity
 * renderer — its model, its texture bind, every layer it has — once per spawner per frame, and the
 * spawner is a block, so a dungeon room can have several of them in view with nothing else to
 * amortise the cost against. The cage itself is ordinary chunk geometry and stays.
 */
@Mixin(TileEntityMobSpawnerRenderer.class)
public class TileEntityMobSpawnerRendererMixin_HideMob {

    @Inject(method = "renderTileEntityAt(Lnet/minecraft/tileentity/TileEntityMobSpawner;DDDFI)V",
            at = @At("HEAD"), cancellable = true)
    private void fpsmaster$hideMob(TileEntityMobSpawner te, double x, double y, double z,
                                   float partialTicks, int destroyStage, CallbackInfo ci) {
        if (Performance.using && Performance.hideMobInSpawner.getValue()) {
            ci.cancel();
        }
    }
}

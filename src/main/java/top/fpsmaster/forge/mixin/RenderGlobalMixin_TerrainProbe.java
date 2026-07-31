package top.fpsmaster.forge.mixin;

import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.culling.ICamera;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.fpsmaster.benchmark.TerrainProbe;

/**
 * Times {@code setupTerrain} and records whether it rebuilt the visible-chunk list.
 *
 * <p>The marker is anchored on the {@code Lists.newLinkedList} that creates the walk's queue. That
 * call exists only inside the branch guarded by {@code displayListEntitiesDirty}, so reaching it is
 * exactly the condition being measured, and it is a call rather than a field write, which is what
 * an injection point can be attached to. The list the walk fills is allocated one line earlier by
 * {@code newArrayList}, which is a different method and so cannot be confused with it.
 */
@Mixin(RenderGlobal.class)
public class RenderGlobalMixin_TerrainProbe {

    @Inject(method = "setupTerrain", at = @At("HEAD"))
    private void fpsmaster$beginSetupTerrain(Entity viewEntity, double partialTicks, ICamera camera,
                                             int frameCount, boolean playerSpectator, CallbackInfo ci) {
        if (TerrainProbe.enabled()) {
            TerrainProbe.begin();
        }
    }

    @Inject(method = "setupTerrain",
            at = @At(value = "INVOKE",
                    target = "Lcom/google/common/collect/Lists;newLinkedList()Ljava/util/LinkedList;"))
    private void fpsmaster$walkedVisibleChunks(Entity viewEntity, double partialTicks, ICamera camera,
                                               int frameCount, boolean playerSpectator, CallbackInfo ci) {
        if (TerrainProbe.enabled()) {
            TerrainProbe.walked(viewEntity.posX, viewEntity.posY, viewEntity.posZ,
                    viewEntity.rotationYaw, viewEntity.rotationPitch);
        }
    }

    @Inject(method = "setupTerrain",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/chunk/ChunkRenderDispatcher;clearChunkUpdates()V"))
    private void fpsmaster$walkEnded(Entity viewEntity, double partialTicks, ICamera camera,
                                     int frameCount, boolean playerSpectator, CallbackInfo ci) {
        if (TerrainProbe.enabled()) {
            TerrainProbe.tailBegins();
        }
    }

    @Inject(method = "setupTerrain", at = @At("RETURN"))
    private void fpsmaster$endSetupTerrain(Entity viewEntity, double partialTicks, ICamera camera,
                                           int frameCount, boolean playerSpectator, CallbackInfo ci) {
        if (TerrainProbe.enabled()) {
            TerrainProbe.end();
        }
    }
}

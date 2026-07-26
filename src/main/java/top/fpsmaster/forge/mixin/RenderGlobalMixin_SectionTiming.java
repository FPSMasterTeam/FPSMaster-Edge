package top.fpsmaster.forge.mixin;

import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.culling.ICamera;
import net.minecraft.entity.Entity;
import net.minecraft.util.EnumWorldBlockLayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import top.fpsmaster.benchmark.BenchProfiler;
import top.fpsmaster.benchmark.BenchmarkMode;

/**
 * Brackets the world-render phases so each can be timed on its own.
 *
 * <p>Judging a targeted change by whole-frame frame rate asks it to move a number it only
 * contributes a fraction of, which usually puts a real improvement inside the run-to-run noise
 * band. These brackets let a change to the entity pass be measured against the entity pass.
 *
 * <p>Purely benchmark instrumentation: every call is behind {@link BenchmarkMode#ACTIVE}, a static
 * final boolean, so the whole body folds away in normal play.
 */
@Mixin(RenderGlobal.class)
public class RenderGlobalMixin_SectionTiming {

    @Inject(method = "renderBlockLayer(Lnet/minecraft/util/EnumWorldBlockLayer;DILnet/minecraft/entity/Entity;)I",
            at = @At("HEAD"))
    private void fpsmasterBeginTerrain(EnumWorldBlockLayer layer, double partialTicks, int pass,
                                       Entity entity, CallbackInfoReturnable<Integer> cir) {
        if (BenchmarkMode.ACTIVE) {
            BenchProfiler.begin(BenchProfiler.SECTION_TERRAIN);
        }
    }

    @Inject(method = "renderBlockLayer(Lnet/minecraft/util/EnumWorldBlockLayer;DILnet/minecraft/entity/Entity;)I",
            at = @At("RETURN"))
    private void fpsmasterEndTerrain(EnumWorldBlockLayer layer, double partialTicks, int pass,
                                     Entity entity, CallbackInfoReturnable<Integer> cir) {
        if (BenchmarkMode.ACTIVE) {
            BenchProfiler.end(BenchProfiler.SECTION_TERRAIN);
        }
    }

    @Inject(method = "renderEntities", at = @At("HEAD"))
    private void fpsmasterBeginEntities(Entity renderViewEntity, ICamera camera, float partialTicks,
                                        CallbackInfo ci) {
        if (BenchmarkMode.ACTIVE) {
            BenchProfiler.begin(BenchProfiler.SECTION_ENTITIES);
        }
    }

    @Inject(method = "renderEntities", at = @At("RETURN"))
    private void fpsmasterEndEntities(Entity renderViewEntity, ICamera camera, float partialTicks,
                                      CallbackInfo ci) {
        if (BenchmarkMode.ACTIVE) {
            BenchProfiler.end(BenchProfiler.SECTION_ENTITIES);
        }
    }

    @Inject(method = "updateChunks", at = @At("HEAD"))
    private void fpsmasterBeginChunkUpload(long finishTimeNano, CallbackInfo ci) {
        if (BenchmarkMode.ACTIVE) {
            BenchProfiler.begin(BenchProfiler.SECTION_CHUNK_UPLOAD);
        }
    }

    @Inject(method = "updateChunks", at = @At("RETURN"))
    private void fpsmasterEndChunkUpload(long finishTimeNano, CallbackInfo ci) {
        if (BenchmarkMode.ACTIVE) {
            BenchProfiler.end(BenchProfiler.SECTION_CHUNK_UPLOAD);
        }
    }
}

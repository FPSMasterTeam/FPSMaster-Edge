package top.fpsmaster.forge.mixin;

import net.minecraft.client.renderer.chunk.ChunkCompileTaskGenerator;
import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;
import net.minecraft.client.renderer.chunk.RenderChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import top.fpsmaster.benchmark.BenchCounters;
import top.fpsmaster.benchmark.BenchmarkMode;
import top.fpsmaster.features.impl.optimizes.Performance;

@Mixin(ChunkRenderDispatcher.class)
public class ChunkRenderDispatcherMixin_LimitUpdates {
    @SuppressWarnings("BusyWait")
    @Inject(method = "getNextChunkUpdate", at = @At("HEAD"))
    private void edge$limitChunkUpdates(CallbackInfoReturnable<ChunkCompileTaskGenerator> cir) throws InterruptedException {
        while (Performance.using && Performance.limitChunks.getValue() && RenderChunk.renderChunksUpdated >= Performance.chunkUpdateLimit.getValue().intValue()) {
            if (BenchmarkMode.ACTIVE) {
                BenchCounters.chunkThrottleSleeps++;
            }
            Thread.sleep(50L);
        }
    }
}



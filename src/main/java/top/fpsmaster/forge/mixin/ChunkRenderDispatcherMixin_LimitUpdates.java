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
import top.fpsmaster.utils.render.ChunkUpdateBudget;

@Mixin(ChunkRenderDispatcher.class)
public class ChunkRenderDispatcherMixin_LimitUpdates {
    @Inject(method = "getNextChunkUpdate", at = @At("HEAD"))
    private void edge$limitChunkUpdates(CallbackInfoReturnable<ChunkCompileTaskGenerator> cir) throws InterruptedException {
        while (Performance.using && Performance.limitChunks.getValue()
                && RenderChunk.renderChunksUpdated >= ChunkUpdateBudget.allowance()) {
            if (BenchmarkMode.ACTIVE) {
                BenchCounters.chunkThrottleSleeps++;
            }
            // Waiting on the budget rather than sleeping a fixed interval: the budget doubles the
            // moment the player stops, and a builder halfway through a 50ms sleep would sit out the
            // rest of it before noticing.
            ChunkUpdateBudget.await();
        }
    }
}



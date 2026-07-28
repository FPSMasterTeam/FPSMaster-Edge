package top.fpsmaster.forge.mixin;

import net.minecraft.client.renderer.chunk.ChunkCompileTaskGenerator;
import net.minecraft.client.renderer.chunk.RenderChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.fpsmaster.benchmark.BenchCounters;
import top.fpsmaster.benchmark.BenchmarkMode;

/**
 * Counts chunk compiles, so a zero from the per-block probe can be told apart from a scenario
 * that simply never rebuilt anything inside its measured window.
 */
@Mixin(RenderChunk.class)
public class RenderChunkMixin_BuildProbe {

    @Inject(method = "rebuildChunk", at = @At("HEAD"))
    private void edge$countRebuild(float x, float y, float z, ChunkCompileTaskGenerator generator, CallbackInfo ci) {
        if (BenchmarkMode.ACTIVE) {
            BenchCounters.chunkRebuilds++;
        }
    }
}

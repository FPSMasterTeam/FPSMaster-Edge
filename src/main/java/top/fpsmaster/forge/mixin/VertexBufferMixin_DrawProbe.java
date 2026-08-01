package top.fpsmaster.forge.mixin;

import net.minecraft.client.renderer.vertex.VertexBuffer;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.fpsmaster.benchmark.BenchCounters;
import top.fpsmaster.benchmark.BenchProfiler;
import top.fpsmaster.benchmark.Experiments;
import top.fpsmaster.benchmark.BenchmarkMode;

/**
 * Counts the draw calls the terrain costs.
 *
 * <p>With VBOs on, every visible chunk section is bound and drawn separately for every block layer
 * it has geometry in. Grouping neighbouring sections into one buffer would collapse that, which is
 * the last idea from the survey that could plausibly be worth an order of magnitude rather than a
 * percent. Whether it is depends on how many draws there actually are, and nobody here has counted.
 *
 * <p>Counting is also the only kind of measurement this machine can still be trusted with: it is
 * both graphics-limited and, at the moment, contended for processor time.
 */
@Mixin(VertexBuffer.class)
public class VertexBufferMixin_DrawProbe {

    @Shadow
    private int count;

    @Unique
    private static int edge$drawOrdinal;

    @Inject(method = "drawArrays", at = @At("HEAD"), cancellable = true)
    private void edge$countTerrainDraw(int mode, CallbackInfo ci) {
        if (BenchmarkMode.ACTIVE) {
            BenchCounters.terrainDrawCalls++;
            BenchProfiler.begin(BenchProfiler.SECTION_TERRAIN_DRAW);
        }
        // Ceiling probe: half the terrain gone. Wrecks the picture, which is the point — it prices
        // what the draws and their triangles are worth together before anything is built to merge
        // them. See Experiments.HALF_TERRAIN_DRAWS.
        if (Experiments.active(Experiments.SPLIT_TERRAIN_DRAWS) && count >= 8) {
            // Same vertices, same triangles, twice the draw calls. The cut is a multiple of four
            // because these are GL_QUADS and splitting inside a quad would shear the chunk.
            int half = (count / 2) & ~3;
            GL11.glDrawArrays(mode, 0, half);
            GL11.glDrawArrays(mode, half, count - half);
            if (BenchmarkMode.ACTIVE) {
                BenchCounters.terrainDrawCalls++;
                BenchProfiler.end(BenchProfiler.SECTION_TERRAIN_DRAW);
            }
            ci.cancel();
            return;
        }
        if (Experiments.active(Experiments.HALF_TERRAIN_DRAWS) && (edge$drawOrdinal++ & 1) == 0) {
            if (BenchmarkMode.ACTIVE) {
                BenchProfiler.end(BenchProfiler.SECTION_TERRAIN_DRAW);
            }
            ci.cancel();
        }
    }

    @Inject(method = "drawArrays", at = @At("RETURN"))
    private void edge$endTerrainDraw(int mode, CallbackInfo ci) {
        if (BenchmarkMode.ACTIVE) {
            BenchProfiler.end(BenchProfiler.SECTION_TERRAIN_DRAW);
        }
    }
}

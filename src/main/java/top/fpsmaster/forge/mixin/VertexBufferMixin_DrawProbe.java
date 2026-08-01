package top.fpsmaster.forge.mixin;

import net.minecraft.client.renderer.vertex.VertexBuffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.fpsmaster.benchmark.BenchCounters;
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

    @Unique
    private static int edge$drawOrdinal;

    @Inject(method = "drawArrays", at = @At("HEAD"), cancellable = true)
    private void edge$countTerrainDraw(int mode, CallbackInfo ci) {
        if (BenchmarkMode.ACTIVE) {
            BenchCounters.terrainDrawCalls++;
        }
        // Ceiling probe: half the terrain gone. Wrecks the picture, which is the point — it prices
        // what the draws and their triangles are worth together before anything is built to merge
        // them. See Experiments.HALF_TERRAIN_DRAWS.
        if (Experiments.active(Experiments.HALF_TERRAIN_DRAWS) && (edge$drawOrdinal++ & 1) == 0) {
            ci.cancel();
        }
    }
}

package top.fpsmaster.forge.mixin;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.Tessellator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import top.fpsmaster.benchmark.Experiments;

/**
 * Splits the sky pass so its cost can be attributed before anything is built to reduce it.
 *
 * <p>Deleting the whole pass is worth 12.5% of average fps on a recorded lobby, which is the largest
 * lever measured — but the pass is a display list for the dome, another for the lower cap, two
 * textured quads for sun and moon, and a handful of immediate-mode vertices. None of that obviously
 * costs a seventh of a frame, and building the wrong cache is expensive. These narrow it to the
 * part that actually pays.
 *
 * <pre>
 *   -Dedge.exp.noSkyLists=true      skip the compiled geometry: dome, lower cap, stars
 *   -Dedge.exp.noSkyImmediate=true  skip what is submitted per frame: gradient, void plane, quads
 * </pre>
 */
@Mixin(RenderGlobal.class)
public class RenderGlobalMixin_SkyParts {

    @Redirect(method = "renderSky(FI)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/GlStateManager;callList(I)V"))
    private void skipSkyLists(int list) {
        if (Experiments.active(Experiments.NO_SKY_LISTS)) {
            return;
        }
        GlStateManager.callList(list);
    }

    @Redirect(method = "renderSky(FI)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/Tessellator;draw()V"))
    private void skipSkyImmediate(Tessellator tessellator) {
        if (Experiments.active(Experiments.NO_SKY_IMMEDIATE)) {
            // The buffer still has to be closed and reset. Skipping draw() outright leaves the
            // world renderer mid-build and the next begin() throws; only the upload is dropped.
            tessellator.getWorldRenderer().finishDrawing();
            tessellator.getWorldRenderer().reset();
            return;
        }
        tessellator.draw();
    }
}

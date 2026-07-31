package top.fpsmaster.forge.mixin;

import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.GlStateManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import top.fpsmaster.benchmark.BenchCounters;
import top.fpsmaster.benchmark.BenchmarkMode;
import top.fpsmaster.benchmark.Experiments;

/**
 * Two ceiling probes that separate what a model box costs into its two candidates.
 *
 * <p>A box costs 0.72us, about 2200 cycles, for a translate, up to three rotates and one
 * {@code callList}. The standing hypothesis is that display lists are emulated on modern drivers,
 * which this project has evidence for — but from a different usage pattern, one list per glyph and
 * thousands of calls a frame rather than twelve per entity. Three assumptions about where cost
 * lives have already been proven wrong in this campaign before any code was written for them, so
 * this measures instead of extending the analogy.
 *
 * <pre>
 *   -Dedge.exp.noModelCallList=true    keep every transform, draw nothing
 *   -Dedge.exp.noModelTransforms=true  keep every draw, transform nothing
 * </pre>
 *
 * <p>Both wreck the picture, which is the point: a ceiling probe answers what a piece of work is
 * worth by deleting it, and the two together should account for the bracket. If neither moves it,
 * the cost is somewhere else entirely and the plan built on this hypothesis is void.
 */
@Mixin(ModelRenderer.class)
public class ModelRendererMixin_CostProbe {

    @Redirect(method = "render", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/GlStateManager;callList(I)V"))
    private void edge$maybeCallList(int list) {
        if (BenchmarkMode.ACTIVE) {
            BenchCounters.modelCallLists++;
        }
        if (Experiments.active(Experiments.NO_MODEL_CALL_LIST)) {
            return;
        }
        GlStateManager.callList(list);
    }

    @Redirect(method = "render", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/GlStateManager;translate(FFF)V"))
    private void edge$maybeTranslate(float x, float y, float z) {
        if (Experiments.active(Experiments.NO_MODEL_TRANSFORMS)) {
            return;
        }
        GlStateManager.translate(x, y, z);
    }

    @Redirect(method = "render", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/GlStateManager;rotate(FFFF)V"))
    private void edge$maybeRotate(float angle, float x, float y, float z) {
        if (Experiments.active(Experiments.NO_MODEL_TRANSFORMS)) {
            return;
        }
        GlStateManager.rotate(angle, x, y, z);
    }
}

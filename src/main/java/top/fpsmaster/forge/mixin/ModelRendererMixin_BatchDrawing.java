package top.fpsmaster.forge.mixin;

import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.fpsmaster.benchmark.BenchCounters;
import top.fpsmaster.benchmark.BenchmarkMode;
import top.fpsmaster.utils.render.ModelBatching;

/**
 * Holds one vertex batch open across a whole box compile, so its quads submit together.
 *
 * <p>Paired with {@link TexturedQuadMixin_BatchDraw}, which makes the individual quads join this
 * batch rather than opening their own.
 *
 * <p>The compiled geometry is baked into a display list, so a box already compiled keeps whatever
 * the setting was at compile time. Toggling the setting therefore has to invalidate the list, which
 * is what the recorded compile state is for — without it, turning the option off would leave every
 * already-compiled model batched until something else happened to recompile it.
 */
@Mixin(ModelRenderer.class)
public class ModelRendererMixin_BatchDrawing {

    @Shadow
    private boolean compiled;

    @Unique
    private boolean edge$compiledBatched;

    @Inject(method = "render", at = @At("HEAD"))
    private void edge$invalidateOnToggle(float scale, CallbackInfo ci) {
        if (edge$compiledBatched != ModelBatching.isEnabled()) {
            this.compiled = false;
        }
    }

    @Inject(method = "compileDisplayList", at = @At(value = "INVOKE_ASSIGN",
            target = "Lnet/minecraft/client/renderer/Tessellator;getWorldRenderer()Lnet/minecraft/client/renderer/WorldRenderer;"))
    private void edge$openBatch(CallbackInfo ci) {
        this.edge$compiledBatched = ModelBatching.isEnabled();
        if (this.edge$compiledBatched) {
            Tessellator.getInstance().getWorldRenderer()
                    .begin(GL_QUADS, DefaultVertexFormats.OLDMODEL_POSITION_TEX_NORMAL);
        }
    }

    @Inject(method = "compileDisplayList", at = @At(value = "INVOKE",
            target = "Lorg/lwjgl/opengl/GL11;glEndList()V", remap = false))
    private void edge$submitBatch(CallbackInfo ci) {
        if (ModelBatching.isEnabled()) {
            if (BenchmarkMode.ACTIVE) {
                BenchCounters.batchedModelDraws++;
            }
            Tessellator.getInstance().draw();
        }
    }

    /** {@code GL11.GL_QUADS}; named rather than left as a bare 7 at the call site. */
    @Unique
    private static final int GL_QUADS = 7;
}

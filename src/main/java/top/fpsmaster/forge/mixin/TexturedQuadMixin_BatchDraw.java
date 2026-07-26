package top.fpsmaster.forge.mixin;

import net.minecraft.client.model.TexturedQuad;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.renderer.vertex.VertexFormat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import top.fpsmaster.utils.render.ModelBatching;

/**
 * Lets a quad join a batch already opened by the model instead of opening its own.
 *
 * <p>Vanilla has each quad bracket its own vertices with a begin and a draw, so a six-sided box
 * costs six submissions. When {@link ModelBatching} reports a batch is already open, both brackets
 * are skipped and the vertices simply accumulate into it.
 *
 * <p>Both redirects fall through to the original call whenever batching is off or this quad is the
 * one that opened the batch, so the vanilla path stays intact.
 */
@Mixin(TexturedQuad.class)
public class TexturedQuadMixin_BatchDraw {

    @Redirect(method = "draw", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/WorldRenderer;begin(ILnet/minecraft/client/renderer/vertex/VertexFormat;)V"))
    private void edge$openBatchOnlyIfUnowned(WorldRenderer renderer, int glMode, VertexFormat format) {
        if (ModelBatching.quadMustOpenBatch(renderer)) {
            renderer.begin(glMode, DefaultVertexFormats.POSITION_TEX_NORMAL);
        }
    }

    @Redirect(method = "draw", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/Tessellator;draw()V"))
    private void edge$finishBatchOnlyIfOwned(Tessellator tessellator) {
        if (ModelBatching.quadMustFinishBatch()) {
            tessellator.draw();
        }
    }
}

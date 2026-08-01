package top.fpsmaster.utils.render;

import net.minecraft.client.renderer.WorldRenderer;
import top.fpsmaster.features.impl.optimizes.Performance;
import top.fpsmaster.forge.mixin.accessor.WorldRendererAccessor;

/**
 * Tracks who owns the vertex batch while a model is being compiled.
 *
 * <p>Vanilla builds a model box one quad at a time, and every quad opens its own batch and issues
 * its own draw. Holding a single batch open across the whole box turns those six submissions into
 * one, which is worth doing because it happens once per box per display-list compile.
 *
 * <p>The state has to be remembered rather than re-derived: once the outer batch is open,
 * {@code isDrawing()} is true regardless of who opened it, so a quad about to finish cannot tell
 * from the renderer alone whether the batch is its own. Keeping that on a helper rather than as a
 * field on every {@code TexturedQuad} makes it clear the flag describes the current draw rather
 * than the quad.
 *
 * <p>Render-thread only, which is where model compilation happens.
 */
public final class ModelBatching {

    private static boolean quadOwnsBatch;

    private ModelBatching() {
    }

    public static boolean isEnabled() {
        return Performance.using && Performance.batchModelRendering.getValue();
    }

    /**
     * Decides whether a quad about to draw must open its own batch, and remembers the answer for
     * {@link #quadMustFinishBatch()}.
     */
    public static boolean quadMustOpenBatch(WorldRenderer renderer) {
        quadOwnsBatch = !((WorldRendererAccessor) renderer).isDrawing();
        return quadOwnsBatch || !isEnabled();
    }

    /** Whether the quad that just emitted its vertices is the one that opened the batch. */
    public static boolean quadMustFinishBatch() {
        return quadOwnsBatch || !isEnabled();
    }
}

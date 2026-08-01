package top.fpsmaster.forge.mixin.accessor;

import net.minecraft.client.renderer.entity.Render;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Reaches {@code Render.renderName}, which is protected.
 *
 * <p>Needed by the switches that hide an entity's model but keep its name label. Vanilla only ever
 * draws the label as the last thing {@code doRender} does, so a mixin that cancels {@code doRender}
 * has to draw it itself or lose it.
 */
@Mixin(Render.class)
public interface RenderAccessor {

    @Invoker("renderName")
    void fpsmaster$invokeRenderName(Entity entity, double x, double y, double z);
}

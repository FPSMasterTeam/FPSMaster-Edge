package top.fpsmaster.utils.render;

import net.minecraft.entity.projectile.EntityArrow;
import top.fpsmaster.forge.mixin.accessor.EntityArrowAccessor;

/**
 * Reads an arrow's {@code inGround} flag from outside a mixin.
 *
 * <p>This class exists for one reason. Mixin 0.7 resolves a cast to another mixin's interface
 * against the casting mixin's own target, so casting an {@code EntityArrow} to its accessor from
 * inside a mixin applied to {@code RenderArrow} cannot be resolved — and the failure is a warning
 * at load and the entire mixin silently dropped, which is how {@code HideGroundArrows} shipped
 * doing nothing at all. An ordinary class is not processed that way and the cast resolves normally.
 *
 * <p>An access transformer entry would be tidier and is what this should become; adding one needs
 * dependencies this workspace has no offline copy of.
 */
public final class ArrowState {

    private ArrowState() {
    }

    public static boolean isInGround(EntityArrow arrow) {
        return ((EntityArrowAccessor) arrow).fpsmaster$isInGround();
    }
}

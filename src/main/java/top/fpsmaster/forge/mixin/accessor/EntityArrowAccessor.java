package top.fpsmaster.forge.mixin.accessor;

import net.minecraft.entity.projectile.EntityArrow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Exposes whether an arrow has landed. The field is private and there is no getter, but it is the
 * only thing separating an arrow in flight from one stuck in a wall.
 */
@Mixin(EntityArrow.class)
public interface EntityArrowAccessor {

    @Accessor("inGround")
    boolean fpsmaster$isInGround();
}

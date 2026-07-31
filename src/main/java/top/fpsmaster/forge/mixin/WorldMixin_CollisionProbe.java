package top.fpsmaster.forge.mixin;

import com.google.common.base.Predicate;
import net.minecraft.entity.Entity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import top.fpsmaster.benchmark.CollisionProbe;

import java.util.List;

/**
 * Brackets one entity AABB query. Every other form of the call funnels through this one.
 */
@Mixin(World.class)
public class WorldMixin_CollisionProbe {

    @Inject(method = "getEntitiesInAABBexcluding", at = @At("HEAD"))
    private void fpsmaster$beginQuery(Entity entityIn, AxisAlignedBB boundingBox,
                                      Predicate<? super Entity> predicate,
                                      CallbackInfoReturnable<List<Entity>> cir) {
        if (CollisionProbe.enabled()) {
            CollisionProbe.beginQuery(boundingBox.minX, boundingBox.minY, boundingBox.minZ,
                    boundingBox.maxX, boundingBox.maxY, boundingBox.maxZ);
        }
    }

    @Inject(method = "getEntitiesInAABBexcluding", at = @At("RETURN"))
    private void fpsmaster$endQuery(Entity entityIn, AxisAlignedBB boundingBox,
                                    Predicate<? super Entity> predicate,
                                    CallbackInfoReturnable<List<Entity>> cir) {
        if (CollisionProbe.enabled()) {
            List<Entity> found = cir.getReturnValue();
            CollisionProbe.endQuery(found == null ? 0 : found.size());
        }
    }
}

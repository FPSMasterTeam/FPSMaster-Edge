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
 * Brackets the two entity AABB queries the world offers.
 *
 * <p>There are two, not one, and only counting the first was wrong. {@code getEntitiesInAABBexcluding}
 * is what collision goes through and funnels every {@code excludingEntity} form; the typed
 * {@code getEntitiesWithinAABB} is a separate walk over the same chunks into a different chunk
 * method, and it is what item merging, nearest-entity searches and anything looking for a kind of
 * entity uses. Counting one and calling it "the entity query" undercounts by however much the other
 * is doing.
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

    @Inject(method = "getCollidingBoundingBoxes", at = @At("HEAD"))
    private void fpsmaster$beginMove(Entity entityIn, AxisAlignedBB bb,
                                     CallbackInfoReturnable<List<AxisAlignedBB>> cir) {
        if (CollisionProbe.enabled()) {
            CollisionProbe.beginMove(bb.minX, bb.minY, bb.minZ, bb.maxX, bb.maxY, bb.maxZ);
        }
    }

    @Inject(method = "getCollidingBoundingBoxes", at = @At("RETURN"))
    private void fpsmaster$endMove(Entity entityIn, AxisAlignedBB bb,
                                   CallbackInfoReturnable<List<AxisAlignedBB>> cir) {
        if (CollisionProbe.enabled()) {
            List<AxisAlignedBB> boxes = cir.getReturnValue();
            CollisionProbe.endMove(boxes == null ? 0 : boxes.size());
        }
    }

    @Inject(method = "getEntitiesWithinAABB(Ljava/lang/Class;Lnet/minecraft/util/AxisAlignedBB;Lcom/google/common/base/Predicate;)Ljava/util/List;",
            at = @At("HEAD"))
    private void fpsmaster$beginTypedQuery(Class<?> clazz, AxisAlignedBB aabb, Predicate<?> filter,
                                           CallbackInfoReturnable<List<?>> cir) {
        if (CollisionProbe.enabled()) {
            CollisionProbe.beginTypedQuery(aabb.minX, aabb.minY, aabb.minZ,
                    aabb.maxX, aabb.maxY, aabb.maxZ);
        }
    }

    @Inject(method = "getEntitiesWithinAABB(Ljava/lang/Class;Lnet/minecraft/util/AxisAlignedBB;Lcom/google/common/base/Predicate;)Ljava/util/List;",
            at = @At("RETURN"))
    private void fpsmaster$endTypedQuery(Class<?> clazz, AxisAlignedBB aabb, Predicate<?> filter,
                                         CallbackInfoReturnable<List<?>> cir) {
        if (CollisionProbe.enabled()) {
            List<?> found = cir.getReturnValue();
            CollisionProbe.endQuery(found == null ? 0 : found.size());
        }
    }
}

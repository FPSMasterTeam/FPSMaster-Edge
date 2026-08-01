package top.fpsmaster.forge.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.fpsmaster.benchmark.BenchCounters;
import top.fpsmaster.benchmark.BenchmarkMode;
import top.fpsmaster.features.impl.optimizes.Performance;

import java.util.Collections;
import java.util.List;

/**
 * Skips the entity half of block collision when nothing in the world can supply a collision box.
 *
 * <p>{@code getCollidingBoundingBoxes} walks the blocks under an entity and then queries the
 * entities around it, and the second half is measured at roughly two thirds of the whole on a Bed
 * Wars recording — 494us of 768us a tick. Almost all of it produces nothing: 0.37 boxes returned
 * per call, and on the quieter windows 0.14.
 *
 * <p>Which is not an accident of the workload. The query's results are used in exactly two ways:
 * the other entity's {@code getCollisionBoundingBox}, and the mover's own
 * {@code getCollisionBox(other)}. Reading every override of both in the 1.8.9 entity tree, there
 * are two classes — {@code EntityBoat} and {@code EntityMinecart}. Nothing else can put a box in
 * that list, ever. A Bed Wars map has neither, so several hundred queries a tick are asked and
 * answered with nothing for the whole match.
 *
 * <p>So the skip is not a heuristic about what is probably nearby. It is the two conditions under
 * which the loop can produce output, checked directly: does the world hold anything whose collision
 * box is non-null, and is the mover itself something that collides with others.
 *
 * <p>The count is maintained per instance rather than per class, so a mod entity that supplies a box
 * is counted like a boat. What it assumes is that an entity which reports no collision box when it
 * enters the world does not acquire one later — true of both vanilla classes, whose boxes are simply
 * their bounding boxes. Off by default while that assumption is only argued and not measured.
 */
@Mixin(World.class)
public class WorldMixin_FastCollision {

    @Unique
    private int fpsmaster$collidableEntities;

    @Inject(method = "onEntityAdded", at = @At("HEAD"))
    private void fpsmaster$countAdded(Entity entityIn, CallbackInfo ci) {
        if (entityIn.getCollisionBoundingBox() != null) {
            fpsmaster$collidableEntities++;
        }
    }

    @Inject(method = "onEntityRemoved", at = @At("HEAD"))
    private void fpsmaster$countRemoved(Entity entityIn, CallbackInfo ci) {
        if (entityIn.getCollisionBoundingBox() != null && fpsmaster$collidableEntities > 0) {
            fpsmaster$collidableEntities--;
        }
    }

    @Redirect(method = "getCollidingBoundingBoxes",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/World;getEntitiesWithinAABBExcludingEntity(Lnet/minecraft/entity/Entity;Lnet/minecraft/util/AxisAlignedBB;)Ljava/util/List;"))
    private List<Entity> fpsmaster$skipEmptyEntityQuery(World world, Entity entityIn, AxisAlignedBB box) {
        if (Performance.using && Performance.fastCollision.getValue()
                && fpsmaster$collidableEntities == 0
                // Asked of itself: the method's answer depends on the mover's class, not on which
                // entity it is handed, and a mover that returns null here returns null for anything.
                && entityIn.getCollisionBox(entityIn) == null) {
            if (BenchmarkMode.ACTIVE) {
                BenchCounters.collisionQueriesSkipped++;
            }
            return Collections.emptyList();
        }
        return world.getEntitiesWithinAABBExcludingEntity(entityIn, box);
    }
}

package top.fpsmaster.forge.mixin;

import com.google.common.base.Predicate;
import net.minecraft.entity.Entity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.ClassInheritanceMultiMap;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.fpsmaster.benchmark.CollisionProbe;

import java.util.List;

/**
 * Counts what one chunk's share of a query actually walks.
 *
 * <p>The section range is recomputed here rather than read out of the method, because the values
 * vanilla derives it into are locals. It is four lines of arithmetic on the box and the same four
 * lines vanilla uses, so the risk is that it drifts if vanilla changes — which for a probe against
 * a frozen version is no risk at all.
 *
 * <p>What is counted is the entities in the sections that will be walked, not the ones that pass:
 * the difference between the two is the scan waste, and the scan waste is the thing any fix would
 * have to remove.
 */
@Mixin(Chunk.class)
public class ChunkMixin_CollisionProbe {

    @Shadow
    private ClassInheritanceMultiMap<Entity>[] entityLists;

    @Inject(method = "getEntitiesWithinAABBForEntity", at = @At("HEAD"))
    private void fpsmaster$countWalk(Entity entityIn, AxisAlignedBB aabb, List<Entity> listToFill,
                                     Predicate<? super Entity> predicate, CallbackInfo ci) {
        fpsmaster$countSections(aabb);
    }

    @Inject(method = "getEntitiesOfTypeWithinAAAB", at = @At("HEAD"))
    private void fpsmaster$countTypedWalk(Class<?> entityClass, AxisAlignedBB aabb, List<?> listToFill,
                                          Predicate<?> predicate, CallbackInfo ci) {
        fpsmaster$countSections(aabb);
    }

    @Unique
    private void fpsmaster$countSections(AxisAlignedBB aabb) {
        if (!CollisionProbe.enabled()) {
            return;
        }
        int from = MathHelper.floor_double((aabb.minY - World.MAX_ENTITY_RADIUS) / 16.0d);
        int to = MathHelper.floor_double((aabb.maxY + World.MAX_ENTITY_RADIUS) / 16.0d);
        from = MathHelper.clamp_int(from, 0, entityLists.length - 1);
        to = MathHelper.clamp_int(to, 0, entityLists.length - 1);
        int entities = 0;
        for (int section = from; section <= to; section++) {
            entities += entityLists[section].size();
        }
        CollisionProbe.chunkWalked(to - from + 1, entities);
    }
}

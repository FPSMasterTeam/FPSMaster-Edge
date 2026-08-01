package top.fpsmaster.forge.mixin;

import net.minecraft.util.ClassInheritanceMultiMap;
import net.minecraft.world.chunk.Chunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import top.fpsmaster.benchmark.BenchCounters;
import top.fpsmaster.benchmark.BenchmarkMode;

/**
 * Prices keeping a list of only the chunks that hold entities.
 *
 * <p>The entity pass walks every visible chunk, asks the world for the chunk object, indexes its
 * entity lists and moves on when the list is empty. Other clients avoid that by building a filtered
 * list during the terrain walk, which is already visiting the same chunks. Whether that is worth
 * anything depends entirely on the ratio, and the ratio has never been measured here — so it is
 * measured before anything is built, which is the same order fast math and Smart Animations were
 * held to.
 *
 * <p>{@code getEntityLists} is called once per visible chunk from that walk and hardly anywhere
 * else during a frame, so counting it counts the walk.
 */
@Mixin(Chunk.class)
public class ChunkMixin_EntityWalkProbe {

    @Inject(method = "getEntityLists", at = @At("RETURN"))
    private void edge$countEntityListLookup(CallbackInfoReturnable<ClassInheritanceMultiMap[]> callback) {
        if (!BenchmarkMode.ACTIVE) {
            return;
        }
        BenchCounters.entityListLookups++;
        ClassInheritanceMultiMap[] lists = callback.getReturnValue();
        if (lists == null) {
            return;
        }
        for (int i = 0; i < lists.length; i++) {
            if (lists[i] != null && !lists[i].isEmpty()) {
                BenchCounters.entityListNonEmpty++;
                return;
            }
        }
    }
}

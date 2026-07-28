package top.fpsmaster.forge.mixin;

import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.culling.ICamera;
import net.minecraft.entity.Entity;
import net.minecraft.util.BlockPos;
import net.minecraft.world.chunk.Chunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.fpsmaster.benchmark.BenchCounters;
import top.fpsmaster.benchmark.BenchmarkMode;
import top.fpsmaster.features.impl.optimizes.Performance;

/**
 * Remembers the chunk the entity walk just asked for.
 *
 * <p>The entity pass visits every visible chunk section — 2893 of them a frame on a recorded lobby
 * at render distance 12 — and asks the world for the chunk each one belongs to, which is a hash
 * lookup, only to find the section holds no entities and move on. A column has sixteen sections
 * and the terrain walk that produced the list explores neighbours together, so the same answer is
 * asked for repeatedly in a row.
 *
 * <p>One entry is enough for that shape and cheap enough to be worth having; a larger cache would
 * cost a hash of its own to avoid a hash. The counters say whether the shape is what it looks like.
 *
 * <p>Cleared at the start of every pass. A chunk object can be replaced between frames when one
 * arrives from the server, and a cache that outlived the frame would read entities out of the
 * chunk that used to be there.
 */
@Mixin(RenderGlobal.class)
public class RenderGlobalMixin_EntityChunkCache {

    /**
     * Direct-mapped on the column, 512 slots.
     *
     * <p>One entry was the first attempt and it hit 0.1% of the time: the terrain walk is a
     * breadth-first expansion outwards from the camera, so consecutive entries are horizontal
     * neighbours in different columns, never the sixteen sections of one column in a row. At render
     * distance twelve those 2893 sections come from about 180 columns, so a table this size holds
     * all of them at once and an index is cheaper than the hash it replaces.
     */
    @Unique
    private static final int EDGE$SLOTS = 512;
    @Unique
    private final long[] edge$columns = new long[EDGE$SLOTS];
    @Unique
    private final Chunk[] edge$chunks = new Chunk[EDGE$SLOTS];

    @Inject(method = "renderEntities", at = @At("HEAD"))
    private void edge$dropCache(Entity renderViewEntity, ICamera camera, float partialTicks,
                                CallbackInfo ci) {
        java.util.Arrays.fill(edge$chunks, null);
    }

    @Redirect(
            method = "renderEntities",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/multiplayer/WorldClient;getChunkFromBlockCoords"
                            + "(Lnet/minecraft/util/BlockPos;)Lnet/minecraft/world/chunk/Chunk;"))
    private Chunk edge$chunkForEntityWalk(WorldClient world, BlockPos pos) {
        if (!Performance.using || !Performance.cacheEntityChunkLookup.getValue()) {
            return world.getChunkFromBlockCoords(pos);
        }
        int chunkX = pos.getX() >> 4;
        int chunkZ = pos.getZ() >> 4;
        long column = ((long) chunkX << 32) | (chunkZ & 0xFFFFFFFFL);
        int slot = (chunkX * 31 + chunkZ) & (EDGE$SLOTS - 1);
        if (edge$chunks[slot] != null && edge$columns[slot] == column) {
            if (BenchmarkMode.ACTIVE) {
                BenchCounters.entityChunkHits++;
            }
            return edge$chunks[slot];
        }
        Chunk chunk = world.getChunkFromBlockCoords(pos);
        edge$columns[slot] = column;
        edge$chunks[slot] = chunk;
        if (BenchmarkMode.ACTIVE) {
            BenchCounters.entityChunkMisses++;
        }
        return chunk;
    }
}

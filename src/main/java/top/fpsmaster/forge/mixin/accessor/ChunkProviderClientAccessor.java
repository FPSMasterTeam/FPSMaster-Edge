package top.fpsmaster.forge.mixin.accessor;

import net.minecraft.client.multiplayer.ChunkProviderClient;
import net.minecraft.world.chunk.Chunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

/** Exposes the loaded-chunk list so a recording can seed chunks the server will not resend. */
@Mixin(ChunkProviderClient.class)
public interface ChunkProviderClientAccessor {
    @Accessor("chunkListing")
    List<Chunk> getChunkListing();
}

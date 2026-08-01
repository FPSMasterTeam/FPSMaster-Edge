package top.fpsmaster.forge.mixin.accessor;

import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.network.NetHandlerPlayClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Lets replay playback install a world without going through {@code handleJoinGame}.
 *
 * <p>That handler resolves the dimension through Forge's {@code NetworkDispatcher.get(manager)},
 * which reads an attribute off the connection's Netty channel. Playback has no channel, so calling
 * it would dereference null. Everything else the handler does is a handful of plain assignments that
 * playback performs itself — all except this one, which is private.
 */
@Mixin(NetHandlerPlayClient.class)
public interface NetHandlerPlayClientAccessor {
    @Accessor("clientWorldController")
    void setClientWorldController(WorldClient world);
}

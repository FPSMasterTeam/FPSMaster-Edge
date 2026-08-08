package top.fpsmaster.forge.mixin;

import io.netty.channel.ChannelHandlerContext;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.fpsmaster.event.EventDispatcher;
import top.fpsmaster.event.events.EventPacket;
import top.fpsmaster.replay.ReplayRecorder;

@Mixin(NetworkManager.class)
public class MixinNetworkManager {
    private static final Logger logger = LogManager.getLogger("FPSMasterPingDebug");

    @Inject(method = "exceptionCaught", at = @At("HEAD"))
    private void debugException(ChannelHandlerContext ctx, Throwable th, CallbackInfo ci) {
        logger.error("PINGDEBUG pipeline exception", th);
    }

    @Inject(method = "channelRead0*", at = @At("HEAD"), cancellable = true)
    private void read(ChannelHandlerContext context, Packet<?> packet, CallbackInfo callback) {
        EventPacket eventPacket = new EventPacket(EventPacket.PacketType.RECEIVE, packet);
        EventDispatcher.dispatchEvent(eventPacket);
        if (eventPacket.isCanceled()) {
            callback.cancel();
            return;
        }
        // After the cancel check: a packet the client never processed must not be in the recording,
        // or playback would diverge from what was actually seen.
        ReplayRecorder.instance().onPacket(packet);
    }

    @Inject(method = "sendPacket(Lnet/minecraft/network/Packet;)V", at = @At("HEAD"), cancellable = true)
    private void send(Packet<?> packet, CallbackInfo callback) {
        EventPacket eventPacket = new EventPacket(EventPacket.PacketType.SEND, packet);
        EventDispatcher.dispatchEvent(eventPacket);
        if (eventPacket.isCanceled())
            callback.cancel();
    }
}



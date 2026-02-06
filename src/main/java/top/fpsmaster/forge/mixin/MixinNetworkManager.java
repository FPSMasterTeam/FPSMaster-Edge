package top.fpsmaster.forge.mixin;

import io.netty.channel.ChannelHandlerContext;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.fpsmaster.event.EventDispatcher;
import top.fpsmaster.event.events.EventPacket;

@Mixin(NetworkManager.class)
public class MixinNetworkManager {

    @Inject(method = "channelRead0*", at = @At("HEAD"), cancellable = true)
    private void read(ChannelHandlerContext context, Packet<?> packet, CallbackInfo callback) {
        EventPacket eventPacket = new EventPacket(EventPacket.PacketType.RECEIVE, packet);
        EventDispatcher.dispatchEvent(eventPacket);
        if (eventPacket.isCanceled())
            callback.cancel();
    }

    @Inject(method = "sendPacket(Lnet/minecraft/network/Packet;)V", at = @At("HEAD"), cancellable = true)
    private void send(Packet<?> packet, CallbackInfo callback) {
        EventPacket eventPacket = new EventPacket(EventPacket.PacketType.SEND, packet);
        EventDispatcher.dispatchEvent(eventPacket);
        if (eventPacket.isCanceled())
            callback.cancel();
    }
}
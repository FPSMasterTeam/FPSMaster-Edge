package top.fpsmaster.impl.network;

import net.minecraft.network.play.server.S38PacketPlayerListItem;
import org.jetbrains.annotations.NotNull;
import top.fpsmaster.api.domain.network.IPlayerListPacketHandler;
import top.fpsmaster.forge.api.IS38PacketPlayerListItem;

import java.util.ArrayList;
import java.util.List;

public class PlayerListPacketHandlerImpl implements IPlayerListPacketHandler {
    
    @Override
    public boolean isPlayerListPacket(@NotNull Object packet) {
        return packet instanceof S38PacketPlayerListItem;
    }
    
    @Override
    public boolean isActionAdd(@NotNull Object packet) {
        if (packet instanceof S38PacketPlayerListItem) {
            S38PacketPlayerListItem p = (S38PacketPlayerListItem) packet;
            return p.getAction() == S38PacketPlayerListItem.Action.ADD_PLAYER;
        }
        return false;
    }
    
    @Override
    public boolean isActionRemove(@NotNull Object packet) {
        if (packet instanceof S38PacketPlayerListItem) {
            S38PacketPlayerListItem p = (S38PacketPlayerListItem) packet;
            return p.getAction() == S38PacketPlayerListItem.Action.REMOVE_PLAYER;
        }
        return false;
    }
    
    @Override
    public List<String> getPlayerNames(@NotNull Object packet) {
        List<String> names = new ArrayList<>();
        if (packet instanceof S38PacketPlayerListItem) {
            IS38PacketPlayerListItem p = (IS38PacketPlayerListItem) packet;
            for (S38PacketPlayerListItem.AddPlayerData data : p.getPlayers()) {
                if (data.getProfile() != null && data.getProfile().getName() != null) {
                    names.add(data.getProfile().getName());
                }
            }
        }
        return names;
    }
}

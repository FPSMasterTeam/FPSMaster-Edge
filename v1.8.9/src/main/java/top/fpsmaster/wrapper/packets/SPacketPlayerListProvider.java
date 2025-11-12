package top.fpsmaster.wrapper.packets;

import net.minecraft.network.play.server.S38PacketPlayerListItem;
import org.jetbrains.annotations.NotNull;
import top.fpsmaster.api.wrapper.packets.AddPlayerDataWrap;
import top.fpsmaster.api.wrapper.packets.PlayerListItemWrap;

import java.util.List;
import java.util.stream.Collectors;

public class SPacketPlayerListProvider implements PlayerListItemWrap {
    public boolean isPacket(@NotNull Object p) {
        return p instanceof S38PacketPlayerListItem;
    }

    public boolean isActionJoin(@NotNull Object p) {
        return isPacket(p) && ((S38PacketPlayerListItem) p).getAction() == S38PacketPlayerListItem.Action.ADD_PLAYER;
    }

    public boolean isActionLeave(@NotNull Object p) {
        return isPacket(p) && ((S38PacketPlayerListItem) p).getAction() == S38PacketPlayerListItem.Action.REMOVE_PLAYER;
    }

    public List<AddPlayerDataWrap> getEntries(@NotNull Object p) {
        List<S38PacketPlayerListItem.AddPlayerData> entries = ((S38PacketPlayerListItem) p).getEntries();
        return entries.stream().map(WrapperAddPlayerData::new).collect(Collectors.toList());
    }

}


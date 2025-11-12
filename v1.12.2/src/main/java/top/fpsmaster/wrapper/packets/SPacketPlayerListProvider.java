package top.fpsmaster.wrapper.packets;

import net.minecraft.network.Packet;
import net.minecraft.network.play.server.SPacketPlayerListItem;
import org.jetbrains.annotations.NotNull;
import top.fpsmaster.api.wrapper.packets.PlayerListItemAddWrap;
import top.fpsmaster.api.wrapper.packets.PlayerListItemWrap;

import java.util.List;
import java.util.stream.Collectors;

public class SPacketPlayerListProvider implements PlayerListItemWrap {
    public boolean isPacket(Object p) {
        return p instanceof SPacketPlayerListItem;
    }

    public boolean isActionJoin(Object p) {
        return isPacket(p) && ((SPacketPlayerListItem) p).getAction() == SPacketPlayerListItem.Action.ADD_PLAYER;
    }

    public boolean isActionLeave(Object p) {
        return isPacket(p) && ((SPacketPlayerListItem) p).getAction() == SPacketPlayerListItem.Action.REMOVE_PLAYER;
    }

    public java.util.List<PlayerListItemAddWrap> getEntries(Object p) {
        List<SPacketPlayerListItem.AddPlayerData> entries = ((SPacketPlayerListItem) p).getEntries();
        return entries.stream().map(WrapperAddPlayerData::new).collect(Collectors.toList());
    }
}


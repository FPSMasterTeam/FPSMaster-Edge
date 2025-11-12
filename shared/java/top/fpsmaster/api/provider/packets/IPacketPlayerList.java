package top.fpsmaster.api.provider.packets;

import top.fpsmaster.api.wrapper.packets.PlayerListItemAddWrap;

import java.util.List;

public interface IPacketPlayerList extends IPacket {
    boolean isActionJoin(Object p);
    boolean isActionLeave(Object p);
    List<PlayerListItemAddWrap> getEntries(Object p);
}
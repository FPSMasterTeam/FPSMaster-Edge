package top.fpsmaster.api.wrapper.packets;

import java.util.List;

public interface PlayerListItemWrap {
    boolean isPacket(Object p);
    boolean isActionJoin(Object p);
    boolean isActionLeave(Object p);
    List<PlayerListItemAddWrap> getEntries(Object p);
}
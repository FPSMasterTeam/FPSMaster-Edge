package top.fpsmaster.wrapper.packets;


import net.minecraft.network.play.server.SPacketPlayerListItem;
import top.fpsmaster.api.wrapper.packets.PlayerListItemAddWrap;

public class WrapperAddPlayerData implements PlayerListItemAddWrap {
    public SPacketPlayerListItem.AddPlayerData entity;

    public WrapperAddPlayerData(SPacketPlayerListItem.AddPlayerData entity) {
        this.entity = entity;
    }

    public String getName() {
        return entity.getProfile().getName();
    }
}

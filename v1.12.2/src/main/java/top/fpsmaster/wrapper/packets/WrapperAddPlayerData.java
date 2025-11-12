package top.fpsmaster.wrapper.packets;


import net.minecraft.network.play.server.SPacketPlayerListItem;
import top.fpsmaster.api.wrapper.packets.AddPlayerDataWrap;

public class WrapperAddPlayerData implements AddPlayerDataWrap {
    public SPacketPlayerListItem.AddPlayerData entity;

    public WrapperAddPlayerData(SPacketPlayerListItem.AddPlayerData entity) {
        this.entity = entity;
    }

    public String getName() {
        return entity.getProfile().getName();
    }
}

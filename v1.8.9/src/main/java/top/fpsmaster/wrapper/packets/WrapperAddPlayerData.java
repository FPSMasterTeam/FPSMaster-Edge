package top.fpsmaster.wrapper.packets;

import net.minecraft.network.play.server.S38PacketPlayerListItem;
import org.jetbrains.annotations.NotNull;
import top.fpsmaster.api.wrapper.packets.PlayerListItemAddWrap;

public class WrapperAddPlayerData implements PlayerListItemAddWrap {
    public S38PacketPlayerListItem.AddPlayerData entity;

    public WrapperAddPlayerData(S38PacketPlayerListItem.AddPlayerData entity) {
        this.entity = entity;
    }

    @NotNull
    public String getName() {
        return entity.getProfile().getName();
    }
}

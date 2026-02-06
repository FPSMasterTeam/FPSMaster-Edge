package top.fpsmaster.forge.api;

import net.minecraft.network.play.server.S38PacketPlayerListItem;

import java.util.List;

public interface IS38PacketPlayerListItem {
    List<S38PacketPlayerListItem.AddPlayerData> getPlayers();
}




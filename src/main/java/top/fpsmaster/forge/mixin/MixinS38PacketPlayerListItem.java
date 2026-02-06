package top.fpsmaster.forge.mixin;

import com.google.common.collect.Lists;
import net.minecraft.network.play.server.S38PacketPlayerListItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import top.fpsmaster.forge.api.IS38PacketPlayerListItem;

import java.util.Collections;
import java.util.List;

@Mixin(S38PacketPlayerListItem.class)
public class MixinS38PacketPlayerListItem implements IS38PacketPlayerListItem {

    @Shadow
    private final List<S38PacketPlayerListItem.AddPlayerData> players = Lists.newArrayList();

    @Override
    public List<S38PacketPlayerListItem.AddPlayerData> getPlayers() {
        return players;
    }
}




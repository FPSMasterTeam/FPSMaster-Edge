package top.fpsmaster.api.interfaces;

import net.minecraft.client.entity.EntityPlayerSP;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class IPlayerImpl implements IPlayer{
    private final EntityPlayerSP playerSP;

    public IPlayerImpl(EntityPlayerSP playerSP) {
        this.playerSP = playerSP;
    }

    @Override
    public @NotNull String getName() {
        return playerSP.getName();
    }

    @Override
    public @NotNull UUID getUniqueId() {
        return playerSP.getUniqueID();
    }
}

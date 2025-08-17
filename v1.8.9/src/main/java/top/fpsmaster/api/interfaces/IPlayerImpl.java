package top.fpsmaster.api.interfaces;

import net.minecraft.client.entity.EntityPlayerSP;

import java.util.UUID;

public class IPlayerImpl implements IPlayer{
    private final EntityPlayerSP playerSP;

    public IPlayerImpl(EntityPlayerSP playerSP) {
        this.playerSP = playerSP;
    }

    @Override
    public String getName() {
        return playerSP.getName();
    }

    @Override
    public UUID getUniqueId() {
        return playerSP.getUniqueID();
    }
}

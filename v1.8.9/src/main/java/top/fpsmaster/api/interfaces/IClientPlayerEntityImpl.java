package top.fpsmaster.api.interfaces;

import net.minecraft.client.entity.EntityPlayerSP;
import org.jetbrains.annotations.NotNull;
import top.fpsmaster.api.interfaces.client.IClientPlayerEntity;

import java.util.UUID;

public class IClientPlayerEntityImpl implements IClientPlayerEntity {
    private final EntityPlayerSP playerSP;

    public IClientPlayerEntityImpl(EntityPlayerSP playerSP) {
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

    @Override
    public boolean isSprinting() {
        return playerSP.isSprinting();
    }

    @Override
    public void setSprinting(boolean sprinting) {
        playerSP.setSprinting(sprinting);
    }
}

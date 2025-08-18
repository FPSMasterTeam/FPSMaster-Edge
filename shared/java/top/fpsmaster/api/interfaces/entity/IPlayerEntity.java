package top.fpsmaster.api.interfaces.entity;

import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public interface IPlayerEntity {
    @NotNull String getName();

    @NotNull UUID getUniqueId();

    boolean isSprinting();

    void setSprinting(boolean sprinting);
}

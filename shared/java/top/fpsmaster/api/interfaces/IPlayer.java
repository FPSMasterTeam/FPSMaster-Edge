package top.fpsmaster.api.interfaces;

import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public interface IPlayer {
    @NotNull String getName();

    @NotNull UUID getUniqueId();
}

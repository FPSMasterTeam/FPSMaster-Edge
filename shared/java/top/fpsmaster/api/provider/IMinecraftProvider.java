package top.fpsmaster.api.provider;

import org.jetbrains.annotations.NotNull;
import top.fpsmaster.api.interfaces.IMinecraft;

public interface IMinecraftProvider {
    @NotNull IMinecraft getMinecraft();
}

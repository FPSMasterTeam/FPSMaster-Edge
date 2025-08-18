package top.fpsmaster.api.provider;

import org.jetbrains.annotations.NotNull;
import top.fpsmaster.api.interfaces.client.IMinecraft;

public interface IMinecraftProvider {
    @NotNull IMinecraft getMinecraft();
}

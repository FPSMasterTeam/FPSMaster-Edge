package top.fpsmaster.api.provider;

import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.NotNull;
import top.fpsmaster.api.interfaces.IMinecraft;
import top.fpsmaster.api.interfaces.IMinecraftImpl;

public class IMinecraftProviderImpl implements IMinecraftProvider {
    private IMinecraftImpl mcImpl;
    private Minecraft mc;

    @Override
    public @NotNull IMinecraft getMinecraft() {
        if (mcImpl == null || mc != Minecraft.getMinecraft()) {
            mc = Minecraft.getMinecraft();
            mcImpl = new IMinecraftImpl(mc);
        }
        return mcImpl;
    }
}

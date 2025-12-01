package top.fpsmaster.impl.client;

import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.Nullable;
import top.fpsmaster.api.domain.client.*;

import java.io.File;

/**
 * 客户端 API 实现 (v1.8.9)
 */
public class ClientAPIImpl implements IClientAPI {
    
    private MinecraftImpl minecraftWrapper;
    
    @Override
    public IMinecraft getMinecraft() {
        if (minecraftWrapper == null) {
            minecraftWrapper = new MinecraftImpl();
        }
        return minecraftWrapper;
    }
    
    @Override
    @Nullable
    public IClientPlayer getPlayer() {
        net.minecraft.client.entity.EntityPlayerSP mcPlayer = Minecraft.getMinecraft().thePlayer;
        if (mcPlayer == null) return null;
        return new ClientPlayerImpl(mcPlayer);
    }
    
    @Override
    public IGameSettings getSettings() {
        return new GameSettingsImpl(Minecraft.getMinecraft().gameSettings);
    }
    
    @Override
    public void removeClickDelay() {
        // Reset left click counter to remove hit delay
        ((top.fpsmaster.forge.api.IMinecraft) Minecraft.getMinecraft()).arch$setLeftClickCounter(0);
    }
    
    @Override
    public File getGameDirectory() {
        return Minecraft.getMinecraft().mcDataDir;
    }
    
    @Override
    @Nullable
    public Object getCurrentScreen() {
        return Minecraft.getMinecraft().currentScreen;
    }
    
    @Override
    public String getServerAddress() {
        if (Minecraft.getMinecraft().isSingleplayer()) {
            return "localhost";
        }
        return Minecraft.getMinecraft().getNetHandler().getNetworkManager().getRemoteAddress().toString();
    }
    
    @Override
    public boolean isSingleplayer() {
        return Minecraft.getMinecraft().isSingleplayer();
    }
}

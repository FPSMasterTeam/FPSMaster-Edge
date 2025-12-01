package top.fpsmaster.impl.client;

import net.minecraft.client.Minecraft;
import top.fpsmaster.api.domain.client.IMinecraft;

/**
 * Minecraft 实例实现 (v1.8.9)
 */
public class MinecraftImpl implements IMinecraft {
    
    private final Minecraft mc;
    
    public MinecraftImpl() {
        this.mc = Minecraft.getMinecraft();
    }
    
    @Override
    public boolean isSingleplayer() {
        return mc.isSingleplayer();
    }
    
    @Override
    public int getFramerateLimit() {
        return mc.gameSettings.limitFramerate;
    }
    
    @Override
    public float getPartialTicks() {
        return ((top.fpsmaster.forge.api.IMinecraft) mc).arch$getTimer().renderPartialTicks;
    }
    
    @Override
    public void removeLeftClickDelay() {
        ((top.fpsmaster.forge.api.IMinecraft) mc).arch$setLeftClickCounter(0);
    }
    
    @Override
    public void printChatMessage(String message) {
        mc.ingameGUI.getChatGUI().printChatMessage(new net.minecraft.util.ChatComponentText(message));
    }
    
    @Override
    public Object getFontRenderer() {
        return mc.fontRendererObj;
    }
}

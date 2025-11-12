package top.fpsmaster.wrapper;

import net.minecraft.client.Minecraft;
import top.fpsmaster.forge.api.IMinecraft;
import top.fpsmaster.interfaces.ProviderManager;
import top.fpsmaster.api.wrapper.TimerWrap;

public class TimerProvider implements TimerWrap {
    public float getRenderPartialTicks(){
        return ((IMinecraft) Minecraft.getMinecraft()).arch$getTimer().renderPartialTicks;
    }
}

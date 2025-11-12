package top.fpsmaster.wrapper;

import net.minecraft.client.Minecraft;
import top.fpsmaster.forge.api.IRenderManager;
import top.fpsmaster.api.wrapper.RenderManagerWrap;

public class RenderManagerProvider implements RenderManagerWrap {
    public double renderPosX(){
        return ((IRenderManager) Minecraft.getMinecraft().getRenderManager()).renderPosX();
    }

    public double renderPosY(){
        return ((IRenderManager) Minecraft.getMinecraft().getRenderManager()).renderPosY();
    }

    public double renderPosZ(){
        return ((IRenderManager) Minecraft.getMinecraft().getRenderManager()).renderPosZ();
    }
}

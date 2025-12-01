package top.fpsmaster.impl.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.RenderManager;
import top.fpsmaster.api.domain.render.IRenderManager;

public class RenderManagerImpl implements IRenderManager {
    
    private final IRenderManager rm;
    
    public RenderManagerImpl() {
        this.rm = (IRenderManager) Minecraft.getMinecraft().getRenderManager();
    }
    
    @Override
    public double getRenderPosX() {
        return rm.getRenderPosX();
    }
    
    @Override
    public double getRenderPosY() {
        return rm.getRenderPosY();
    }
    
    @Override
    public double getRenderPosZ() {
        return rm.getRenderPosZ();
    }
}

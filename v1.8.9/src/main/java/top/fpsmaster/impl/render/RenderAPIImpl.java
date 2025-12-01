package top.fpsmaster.impl.render;

import net.minecraft.client.Minecraft;
import top.fpsmaster.api.domain.render.IParticleManager;
import top.fpsmaster.api.domain.render.IRenderAPI;
import top.fpsmaster.api.domain.render.IRenderManager;

public class RenderAPIImpl implements IRenderAPI {
    
    @Override
    public IRenderManager getRenderManager() {
        return new RenderManagerImpl();
    }
    
    @Override
    public float getPartialTicks() {
        return ((top.fpsmaster.forge.api.IMinecraft) Minecraft.getMinecraft()).arch$getTimer().renderPartialTicks;
    }
    
    @Override
    public IParticleManager getParticleManager() {
        return new ParticleManagerImpl();
    }
}

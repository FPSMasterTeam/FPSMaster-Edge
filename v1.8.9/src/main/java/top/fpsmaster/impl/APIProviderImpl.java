package top.fpsmaster.impl;

import top.fpsmaster.api.APIProvider;
import top.fpsmaster.api.domain.client.IClientAPI;
import top.fpsmaster.api.domain.entity.IEntityAPI;
import top.fpsmaster.api.domain.gui.IGuiAPI;
import top.fpsmaster.api.domain.network.INetworkAPI;
import top.fpsmaster.api.domain.render.IRenderAPI;
import top.fpsmaster.api.domain.sound.ISoundAPI;
import top.fpsmaster.api.domain.world.IWorldAPI;
import top.fpsmaster.impl.client.ClientAPIImpl;
import top.fpsmaster.impl.entity.EntityAPIImpl;
import top.fpsmaster.impl.gui.GuiAPIImpl;
import top.fpsmaster.impl.network.NetworkAPIImpl;
import top.fpsmaster.impl.render.RenderAPIImpl;
import top.fpsmaster.impl.sound.SoundAPIImpl;
import top.fpsmaster.impl.world.WorldAPIImpl;

/**
 * API Provider 实现 (v1.8.9)
 * 提供所有域的 API 实例（懒加载 + 缓存）
 */
public class APIProviderImpl implements APIProvider {
    
    // 懒加载缓存
    private IClientAPI clientAPI;
    private IWorldAPI worldAPI;
    private IRenderAPI renderAPI;
    private INetworkAPI networkAPI;
    private IGuiAPI guiAPI;
    private ISoundAPI soundAPI;
    private IEntityAPI entityAPI;
    
    @Override
    public IClientAPI getClientAPI() {
        if (clientAPI == null) {
            clientAPI = new ClientAPIImpl();
        }
        return clientAPI;
    }
    
    @Override
    public IWorldAPI getWorldAPI() {
        if (worldAPI == null) {
            worldAPI = new WorldAPIImpl();
        }
        return worldAPI;
    }
    
    @Override
    public IRenderAPI getRenderAPI() {
        if (renderAPI == null) {
            renderAPI = new RenderAPIImpl();
        }
        return renderAPI;
    }
    
    @Override
    public INetworkAPI getNetworkAPI() {
        if (networkAPI == null) {
            networkAPI = new NetworkAPIImpl();
        }
        return networkAPI;
    }
    
    @Override
    public IGuiAPI getGuiAPI() {
        if (guiAPI == null) {
            guiAPI = new GuiAPIImpl();
        }
        return guiAPI;
    }
    
    @Override
    public ISoundAPI getSoundAPI() {
        if (soundAPI == null) {
            soundAPI = new SoundAPIImpl();
        }
        return soundAPI;
    }
    
    @Override
    public IEntityAPI getEntityAPI() {
        if (entityAPI == null) {
            entityAPI = new EntityAPIImpl();
        }
        return entityAPI;
    }
}

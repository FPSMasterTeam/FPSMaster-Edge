package top.fpsmaster.api;

import top.fpsmaster.api.domain.client.IClientAPI;
import top.fpsmaster.api.domain.entity.IEntityAPI;
import top.fpsmaster.api.domain.world.IWorldAPI;
import top.fpsmaster.api.domain.render.IRenderAPI;
import top.fpsmaster.api.domain.network.INetworkAPI;
import top.fpsmaster.api.domain.gui.IGuiAPI;
import top.fpsmaster.api.domain.sound.ISoundAPI;

/**
 * API Provider 接口
 * 由各个版本的实现模块提供具体实现
 * 
 * 实现类应该位于版本特定的包中:
 * - v1.8.9: top.fpsmaster.impl.APIProviderImpl
 * - v1.12.2: top.fpsmaster.impl.APIProviderImpl
 */
public interface APIProvider {
    
    /**
     * 获取客户端 API 实现
     */
    IClientAPI getClientAPI();
    
    /**
     * 获取世界 API 实现
     */
    IWorldAPI getWorldAPI();
    
    /**
     * 获取渲染 API 实现
     */
    IRenderAPI getRenderAPI();
    
    /**
     * 获取网络 API 实现
     */
    INetworkAPI getNetworkAPI();
    
    /**
     * 获取 GUI API 实现
     */
    IGuiAPI getGuiAPI();
    
    /**
     * 获取音效 API 实现
     */
    ISoundAPI getSoundAPI();
    
    /**
     * 获取实体 API 实现
     */
    IEntityAPI getEntityAPI();
}

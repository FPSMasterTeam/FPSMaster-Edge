package top.fpsmaster.api;

import top.fpsmaster.api.domain.client.IClientAPI;
import top.fpsmaster.api.domain.entity.IEntityAPI;
import top.fpsmaster.api.domain.world.IWorldAPI;
import top.fpsmaster.api.domain.render.IRenderAPI;
import top.fpsmaster.api.domain.network.INetworkAPI;
import top.fpsmaster.api.domain.gui.IGuiAPI;
import top.fpsmaster.api.domain.sound.ISoundAPI;

/**
 * FPSMaster 跨版本 API 统一访问入口
 * 所有业务代码应该通过此类访问 Minecraft 相关功能
 * 
 * 使用示例:
 * <pre>
 * IClientPlayer player = MinecraftAPI.client().getPlayer();
 * IWorld world = MinecraftAPI.world().getWorld();
 * </pre>
 */
public final class MinecraftAPI {
    
    private static volatile APIProvider provider;
    
    /**
     * 私有构造函数，防止实例化
     */
    private MinecraftAPI() {
        throw new UnsupportedOperationException("Utility class");
    }
    
    /**
     * 初始化 API Provider (由各版本的入口调用)
     * 
     * @param apiProvider API 提供者实现
     * @throws IllegalStateException 如果已经初始化
     */
    public static void initialize(APIProvider apiProvider) {
        if (provider != null) {
            throw new IllegalStateException("MinecraftAPI already initialized");
        }
        if (apiProvider == null) {
            throw new IllegalArgumentException("APIProvider cannot be null");
        }
        provider = apiProvider;
    }
    
    /**
     * 检查 API 是否已初始化
     */
    public static boolean isInitialized() {
        return provider != null;
    }
    
    /**
     * 获取 Provider（内部使用）
     */
    private static APIProvider getProvider() {
        if (provider == null) {
            throw new IllegalStateException("MinecraftAPI not initialized. Call MinecraftAPI.initialize() first.");
        }
        return provider;
    }
    
    /**
     * 客户端相关 API
     * 包括: Minecraft实例、玩家、设置、会话等
     * 
     * @return 客户端 API 实例
     */
    public static IClientAPI client() {
        return getProvider().getClientAPI();
    }
    
    /**
     * 世界相关 API
     * 包括: 方块、实体、时间、天气等
     * 
     * @return 世界 API 实例
     */
    public static IWorldAPI world() {
        return getProvider().getWorldAPI();
    }
    
    /**
     * 渲染相关 API
     * 包括: RenderManager、粒子效果、字体渲染等
     * 
     * @return 渲染 API 实例
     */
    public static IRenderAPI render() {
        return getProvider().getRenderAPI();
    }
    
    /**
     * 网络包相关 API
     * 包括: 数据包解析、发送等
     * 
     * @return 网络 API 实例
     */
    public static INetworkAPI network() {
        return getProvider().getNetworkAPI();
    }
    
    /**
     * GUI 相关 API
     * 包括: 主菜单、聊天、游戏内GUI等
     * 
     * @return GUI API 实例
     */
    public static IGuiAPI gui() {
        return getProvider().getGuiAPI();
    }
    
    /**
     * 音效相关 API
     * 包括: 声音播放、音效管理等
     * 
     * @return 音效 API 实例
     */
    public static ISoundAPI sound() {
        return getProvider().getSoundAPI();
    }
    
    /**
     * 实体相关 API
     * 包括: 实体查找、实体信息等
     * 
     * @return 实体 API 实例
     */
    public static IEntityAPI entity() {
        return getProvider().getEntityAPI();
    }
}

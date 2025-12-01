package top.fpsmaster.api.domain.client;

import org.jetbrains.annotations.Nullable;
import top.fpsmaster.api.model.GameMode;

import java.io.File;

/**
 * 客户端 API
 * 提供客户端相关的所有功能
 */
public interface IClientAPI {
    
    /**
     * 获取 Minecraft 实例接口
     * 
     * @return Minecraft 实例
     */
    IMinecraft getMinecraft();
    
    /**
     * 获取当前客户端玩家
     * 
     * @return 玩家实例，可能为 null（未加入世界时）
     */
    @Nullable
    IClientPlayer getPlayer();
    
    /**
     * 获取游戏设置
     * 
     * @return 游戏设置实例
     */
    IGameSettings getSettings();
    
    /**
     * 移除点击延迟（用于 NoHitDelay 等优化功能）
     * 将 Minecraft 的点击计数器重置为 0
     */
    void removeClickDelay();
    
    /**
     * 获取游戏目录
     * 
     * @return 游戏根目录
     */
    File getGameDirectory();
    
    /**
     * 获取当前打开的屏幕
     * 
     * @return GUI屏幕对象，可能为 null（没有打开任何界面时）
     */
    @Nullable
    Object getCurrentScreen();
    
    /**
     * 获取服务器地址
     * 
     * @return 服务器地址，单人游戏返回 "localhost"
     */
    String getServerAddress();
    
    /**
     * 是否单人游戏
     * 
     * @return true 如果是单人游戏
     */
    boolean isSingleplayer();
}

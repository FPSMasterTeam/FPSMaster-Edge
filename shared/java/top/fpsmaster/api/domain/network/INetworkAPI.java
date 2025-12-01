package top.fpsmaster.api.domain.network;

/**
 * 网络 API
 * 提供网络包相关的所有功能
 */
public interface INetworkAPI {
    
    /**
     * 获取聊天包解析器
     * 
     * @return 聊天包处理器
     */
    IChatPacketHandler getChatPacketHandler();
    
    /**
     * 获取玩家列表包处理器
     * 
     * @return 玩家列表包处理器
     */
    IPlayerListPacketHandler getPlayerListPacketHandler();
    
    /**
     * 获取时间更新包处理器
     * 
     * @return 时间更新包处理器
     */
    ITimeUpdatePacketHandler getTimeUpdatePacketHandler();
}

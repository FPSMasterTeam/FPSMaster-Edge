package top.fpsmaster.api.domain.network;

import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * 玩家列表包处理器
 */
public interface IPlayerListPacketHandler {
    
    /**
     * 判断是否为玩家列表包
     * 
     * @param packet 数据包对象
     * @return true 如果是玩家列表包
     */
    boolean isPlayerListPacket(@NotNull Object packet);
    
    /**
     * 判断是否为加入动作
     * 
     * @param packet 数据包对象
     * @return true 如果是加入动作
     */
    boolean isActionAdd(@NotNull Object packet);
    
    /**
     * 判断是否为离开动作
     * 
     * @param packet 数据包对象
     * @return true 如果是离开动作
     */
    boolean isActionRemove(@NotNull Object packet);
    
    /**
     * 获取玩家名称列表
     * 
     * @param packet 数据包对象
     * @return 玩家名称列表
     */
    List<String> getPlayerNames(@NotNull Object packet);
}

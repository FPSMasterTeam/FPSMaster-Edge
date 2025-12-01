package top.fpsmaster.api.domain.network;

import org.jetbrains.annotations.NotNull;

/**
 * 聊天包处理器
 */
public interface IChatPacketHandler {
    
    /**
     * 判断是否为聊天包
     * 
     * @param packet 数据包对象
     * @return true 如果是聊天包
     */
    boolean isChatPacket(@NotNull Object packet);
    
    /**
     * 从包中获取未格式化的文本
     * 
     * @param packet 数据包对象
     * @return 未格式化的文本
     */
    @NotNull
    String getUnformattedText(@NotNull Object packet);
    
    /**
     * 获取聊天类型
     * 
     * @param packet 数据包对象
     * @return 聊天类型（0=聊天，1=系统消息，2=游戏信息）
     */
    int getChatType(@NotNull Object packet);
}

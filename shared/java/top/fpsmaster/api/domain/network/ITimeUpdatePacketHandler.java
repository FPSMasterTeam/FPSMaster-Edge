package top.fpsmaster.api.domain.network;

import org.jetbrains.annotations.NotNull;

/**
 * 时间更新包处理器
 */
public interface ITimeUpdatePacketHandler {
    
    /**
     * 判断是否为时间更新包
     * 
     * @param packet 数据包对象
     * @return true 如果是时间更新包
     */
    boolean isTimeUpdatePacket(@NotNull Object packet);
    
    /**
     * 获取世界时间
     * 
     * @param packet 数据包对象
     * @return 世界时间
     */
    long getWorldTime(@NotNull Object packet);
}

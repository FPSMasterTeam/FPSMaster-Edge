package top.fpsmaster.api.domain.world;

import org.jetbrains.annotations.Nullable;

/**
 * 世界 API
 * 提供世界相关的所有功能
 */
public interface IWorldAPI {
    
    /**
     * 获取当前客户端世界
     * 
     * @return 世界实例，可能为 null（未加入世界时）
     */
    @Nullable
    IWorld getWorld();
}

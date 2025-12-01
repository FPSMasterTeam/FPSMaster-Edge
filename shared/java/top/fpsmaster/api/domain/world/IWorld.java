package top.fpsmaster.api.domain.world;

import org.jetbrains.annotations.Nullable;
import top.fpsmaster.api.model.BlockPos;
import top.fpsmaster.api.model.WeatherType;

/**
 * 世界接口
 * 注意：完全不依赖 Minecraft 原生类型
 */
public interface IWorld {
    
    /**
     * 获取世界名称
     * 
     * @return 世界名称
     */
    String getWorldName();
    
    /**
     * 获取世界时间（tick）
     * 
     * @return 世界时间
     */
    long getWorldTime();
    
    /**
     * 设置世界时间
     * 
     * @param time 新的世界时间
     */
    void setWorldTime(long time);
    
    /**
     * 获取方块 ID
     * 
     * @param pos 方块位置
     * @return 方块的资源位置，如 "minecraft:stone"
     */
    String getBlockId(BlockPos pos);
    
    /**
     * 检查是否为空气方块
     * 
     * @param pos 方块位置
     * @return true 如果是空气
     */
    boolean isAirBlock(BlockPos pos);
    
    /**
     * 获取世界出生点
     * 
     * @return 出生点位置
     */
    BlockPos getSpawnPoint();
    
    /**
     * 获取天气状态
     * 
     * @return 天气类型
     */
    WeatherType getWeatherType();
    
    /**
     * 获取雨强度
     * 
     * @return 雨强度 (0.0 - 1.0)
     */
    float getRainStrength();
    
    /**
     * 获取方块光照等级
     * 
     * @param pos 方块位置
     * @return 光照等级 (0-15)
     */
    int getLightLevel(BlockPos pos);
    
    /**
     * 获取天空光照等级
     * 
     * @param pos 方块位置
     * @return 天空光照等级 (0-15)
     */
    int getSkyLightLevel(BlockPos pos);
    
    /**
     * 获取方块发出的光照等级
     * 
     * @param pos 方块位置
     * @return 方块光照等级 (0-15)
     */
    int getBlockLightLevel(BlockPos pos);
    
    /**
     * 获取原始 Minecraft 世界对象（用于需要底层访问的功能）
     * 注意：这是版本相关的原生类型，仅在必要时使用
     * 返回类型为 WorldClient 在 1.8.9/1.12.2
     * 
     * @return Minecraft 原生世界对象
     */
    Object getRawWorld();
}

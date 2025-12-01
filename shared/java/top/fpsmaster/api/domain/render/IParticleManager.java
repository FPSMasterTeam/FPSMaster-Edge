package top.fpsmaster.api.domain.render;

import top.fpsmaster.api.model.BlockPos;

/**
 * 粒子管理器接口
 */
public interface IParticleManager {
    
    /**
     * 添加红石破碎粒子效果
     * 
     * @param pos 方块位置
     */
    void addRedstoneBreakParticle(BlockPos pos);
    
    /**
     * 添加方块破坏粒子
     * 
     * @param pos 方块位置
     * @param blockId 方块ID
     */
    void addBlockBreakParticle(BlockPos pos, String blockId);
}

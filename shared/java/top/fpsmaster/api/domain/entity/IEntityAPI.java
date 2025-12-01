package top.fpsmaster.api.domain.entity;

import top.fpsmaster.api.model.Vec3;

import java.util.List;

/**
 * 实体 API
 * 提供实体相关的所有功能
 */
public interface IEntityAPI {
    
    /**
     * 获取所有加载的实体
     * 
     * @return 实体列表
     */
    List<IEntity> getAllEntities();
    
    /**
     * 根据ID获取实体
     * 
     * @param entityId 实体ID
     * @return 实体对象，未找到则返回 null
     */
    IEntity getEntityById(int entityId);
    
    /**
     * 获取玩家附近的实体
     * 
     * @param radius 半径
     * @return 实体列表
     */
    List<IEntity> getNearbyEntities(double radius);
    
    /**
     * 获取指定位置附近的实体
     * 
     * @param pos 中心位置
     * @param radius 半径
     * @return 实体列表
     */
    List<IEntity> getNearbyEntities(Vec3 pos, double radius);
}

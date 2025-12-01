package top.fpsmaster.api.domain.entity;

import top.fpsmaster.api.model.Vec3;

import java.util.List;

/**
 * 实体接口
 * 代表游戏中的任意实体（玩家、生物、物品等）
 */
public interface IEntity {
    
    /**
     * 获取实体唯一ID
     * 
     * @return 实体ID
     */
    int getEntityId();
    
    /**
     * 获取实体UUID
     * 
     * @return UUID字符串
     */
    String getUUID();
    
    /**
     * 获取实体位置
     * 
     * @return 位置向量
     */
    Vec3 getPosition();
    
    /**
     * 获取实体名称
     * 
     * @return 实体名称
     */
    String getName();
    
    /**
     * 获取自定义名称
     * 
     * @return 自定义名称，如果没有则返回 null
     */
    String getCustomName();
    
    /**
     * 是否有自定义名称
     * 
     * @return true 如果有自定义名称
     */
    boolean hasCustomName();
    
    /**
     * 获取实体视角方向（yaw）
     * 
     * @return yaw角度
     */
    float getYaw();
    
    /**
     * 获取实体俯仰角（pitch）
     * 
     * @return pitch角度
     */
    float getPitch();
    
    /**
     * 获取实体速度
     * 
     * @return 速度向量
     */
    Vec3 getMotion();
    
    /**
     * 实体是否在地面上
     * 
     * @return true 如果在地面
     */
    boolean isOnGround();
    
    /**
     * 实体是否在水中
     * 
     * @return true 如果在水中
     */
    boolean isInWater();
    
    /**
     * 实体是否在岩浆中
     * 
     * @return true 如果在岩浆中
     */
    boolean isInLava();
    
    /**
     * 实体是否燃烧
     * 
     * @return true 如果燃烧
     */
    boolean isBurning();
    
    /**
     * 实体是否潜行
     * 
     * @return true 如果潜行
     */
    boolean isSneaking();
    
    /**
     * 实体是否疾跑
     * 
     * @return true 如果疾跑
     */
    boolean isSprinting();
    
    /**
     * 实体是否隐身
     * 
     * @return true 如果隐身
     */
    boolean isInvisible();
    
    /**
     * 获取实体到另一个位置的距离
     * 
     * @param pos 目标位置
     * @return 距离
     */
    double getDistanceTo(Vec3 pos);
    
    /**
     * 获取实体的眼睛高度
     * 
     * @return 眼睛高度
     */
    float getEyeHeight();
}

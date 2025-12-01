package top.fpsmaster.api.domain.client;

import org.jetbrains.annotations.Nullable;
import top.fpsmaster.api.model.Vec3;

import java.util.List;
import java.util.UUID;

/**
 * 客户端玩家接口
 * 注意：不直接返回 Minecraft 的 EntityPlayerSP 等类型
 */
public interface IClientPlayer {
    
    /**
     * 获取玩家名称
     * 
     * @return 玩家名称
     */
    String getName();
    
    /**
     * 获取玩家 UUID
     * 
     * @return 玩家唯一 ID
     */
    UUID getUniqueId();
    
    /**
     * 获取玩家 UUID 字符串
     * 
     * @return UUID 字符串
     */
    String getUUID();
    
    /**
     * 获取玩家位置
     * 
     * @return 位置向量
     */
    Vec3 getPosition();
    
    /**
     * 获取玩家视角方向（Yaw）
     * 
     * @return Yaw 角度
     */
    float getYaw();
    
    /**
     * 获取玩家视角俯仰角（Pitch）
     * 
     * @return Pitch 角度
     */
    float getPitch();
    
    /**
     * 获取玩家生命值
     * 
     * @return 当前生命值
     */
    float getHealth();
    
    /**
     * 获取玩家最大生命值
     * 
     * @return 最大生命值
     */
    float getMaxHealth();
    
    /**
     * 获取玩家饥饿值
     * 
     * @return 饥饿值 (0-20)
     */
    int getFoodLevel();
    
    /**
     * 是否在地面上
     * 
     * @return true 如果在地面上
     */
    boolean isOnGround();
    
    /**
     * 是否在潜行
     * 
     * @return true 如果在潜行
     */
    boolean isSneaking();
    
    /**
     * 是否在疾跑
     * 
     * @return true 如果在疾跑
     */
    boolean isSprinting();
    
    /**
     * 获取手持物品（使用简化的物品数据）
     * 
     * @return 物品数据，可能为 null
     */
    @Nullable
    IItemStack getHeldItem();
    
    /**
     * 获取护甲栏物品列表
     * 索引 0-3 分别对应：靴子、护腿、胸甲、头盔
     * 
     * @return 护甲物品列表（固定4个元素，可能包含null）
     */
    List<IItemStack> getArmorItems();
    
    /**
     * 获取网络延迟（ms）
     * 
     * @return 延迟毫秒数
     */
    int getPing();
    
    /**
     * 发送聊天消息
     * 
     * @param message 消息内容
     */
    void sendChatMessage(String message);
    
    /**
     * 获取玩家的移动速度
     * 
     * @return 速度向量
     */
    Vec3 getMotion();
    
    /**
     * 获取下落距离（用于摔落伤害计算）
     * 
     * @return 下落距离
     */
    float getFallDistance();
    
    /**
     * 获取实体 ID
     * 
     * @return 实体 ID
     */
    int getEntityId();
    
    /**
     * 获取原始 Minecraft 玩家实体（用于需要底层访问的功能）
     * 注意：这是版本相关的原生类型，仅在必要时使用（如战斗机制）
     * 返回类型为 EntityPlayerSP 在 1.8.9/1.12.2
     * 
     * @return Minecraft 原生玩家实体
     */
    Object getRawPlayer();
}

package top.fpsmaster.api.domain.client;

import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * 物品堆接口
 * 使用简化数据结构，不直接暴露 ItemStack
 */
public interface IItemStack {
    
    /**
     * 获取物品 ID
     * 
     * @return 资源位置，如 "minecraft:diamond_sword"
     */
    String getItemId();
    
    /**
     * 获取物品显示名称
     * 
     * @return 显示名称
     */
    String getDisplayName();
    
    /**
     * 获取数量
     * 
     * @return 物品数量
     */
    int getCount();
    
    /**
     * 获取耐久度（当前/最大）
     * 
     * @return 数组 [current, max]，如果不可损坏则返回 null
     */
    @Nullable
    int[] getDurability();
    
    /**
     * 是否有附魔
     * 
     * @return true 如果物品有附魔
     */
    boolean isEnchanted();
    
    /**
     * 获取附魔列表
     * 
     * @return Map<附魔ID, 等级>，如 {"minecraft:sharpness": 5}
     */
    Map<String, Integer> getEnchantments();
    
    /**
     * 获取物品标签数据（简化的 NBT）
     * 
     * @return 键值对，只包含基础类型
     */
    Map<String, Object> getNbtData();
    
    /**
     * 是否为空
     * 
     * @return true 如果物品为空或数量为0
     */
    boolean isEmpty();
    
    /**
     * 是否可堆叠
     * 
     * @return true 如果可堆叠
     */
    boolean isStackable();
    
    /**
     * 获取最大堆叠数量
     * 
     * @return 最大堆叠数
     */
    int getMaxStackSize();
}

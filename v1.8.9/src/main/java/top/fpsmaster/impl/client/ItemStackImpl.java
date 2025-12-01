package top.fpsmaster.impl.client;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import top.fpsmaster.api.domain.client.IItemStack;

import java.util.HashMap;
import java.util.Map;

/**
 * 物品堆实现 (v1.8.9)
 * 将 Minecraft 的 ItemStack 封装为简化的接口
 */
public class ItemStackImpl implements IItemStack {
    
    private final ItemStack stack;
    
    public ItemStackImpl(ItemStack stack) {
        this.stack = stack;
    }
    
    @Override
    public String getItemId() {
        if (stack.getItem().getRegistryName() != null) {
            return stack.getItem().getRegistryName().toString();
        }
        return "minecraft:air";
    }
    
    @Override
    public String getDisplayName() {
        return stack.getDisplayName();
    }
    
    @Override
    public int getCount() {
        return stack.stackSize;
    }
    
    @Override
    @Nullable
    public int[] getDurability() {
        if (!stack.getItem().isDamageable()) return null;
        int current = stack.getMaxDamage() - stack.getItemDamage();
        int max = stack.getMaxDamage();
        return new int[]{current, max};
    }
    
    @Override
    public boolean isEnchanted() {
        return stack.isItemEnchanted();
    }
    
    @Override
    public Map<String, Integer> getEnchantments() {
        Map<String, Integer> enchants = new HashMap<>();
        
        @SuppressWarnings("unchecked")
        Map<Integer, Integer> mcEnchants = EnchantmentHelper.getEnchantments(stack);
        for (Map.Entry<Integer, Integer> entry : mcEnchants.entrySet()) {
            Enchantment ench = Enchantment.getEnchantmentById(entry.getKey());
            if (ench != null && ench.getName() != null) {
                enchants.put(ench.getName(), entry.getValue());
            }
        }
        return enchants;
    }
    
    @Override
    public Map<String, Object> getNbtData() {
        Map<String, Object> data = new HashMap<>();
        // 简化实现，可以根据需要扩展
        if (stack.hasTagCompound()) {
            // 这里可以解析 NBT，但只暴露基础类型
        }
        return data;
    }
    
    @Override
    public boolean isEmpty() {
        return stack == null || stack.stackSize <= 0;
    }
    
    @Override
    public boolean isStackable() {
        return stack.isStackable();
    }
    
    @Override
    public int getMaxStackSize() {
        return stack.getMaxStackSize();
    }
}

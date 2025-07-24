package top.fpsmaster.features.settings.impl;

import net.minecraft.item.ItemStack;
import top.fpsmaster.features.settings.Setting;

import java.util.ArrayList;

public class MutipleItemSetting extends Setting<ArrayList<ItemStack>> {
    public static final int MAX_CAPACITY = 7;
    public MutipleItemSetting(String name) {
        super(name, new ArrayList<>());
    }

    public MutipleItemSetting(String name, VisibleCondition condition) {
        super(name, new ArrayList<>(), condition);
    }

    public void addItem(ItemStack itemStack) {
        if (this.getValue().size() < MAX_CAPACITY) {
            this.getValue().add(itemStack);
        }
    }

    public void removeItem(int index) {
        ItemStack itemStack = this.getValue().get(index);
        this.getValue().remove(itemStack);
    }

}

package top.fpsmaster.ui.custom.impl;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import top.fpsmaster.features.impl.interfaces.ItemCountDisplay;
import top.fpsmaster.ui.custom.Component;
import top.fpsmaster.utils.world.ItemsUtil;
import top.fpsmaster.utils.world.PotionMetadata;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import static top.fpsmaster.utils.Utility.mc;

public class ItemCountDisplayComponent extends Component {

    private static final float DISPLAY_HEIGHT = 27;
    private static final float ITEM_WIDTH = 17;
    public Map<Integer, ItemStack[]> modeItems = new HashMap<>();
    private List<ItemStack> itemStacks = new ArrayList<>();
    public ItemCountDisplayComponent() {
        super(ItemCountDisplay.class);
        modeItems.put(0,
                new ItemStack[]{
                        ItemsUtil.getItemStack(Items.ender_pearl),
                        ItemsUtil.getItemStackWithMetadata(Items.potionitem, PotionMetadata.SPLASH_HEALING_II),
                        ItemsUtil.getItemStackWithMetadata(Items.potionitem, PotionMetadata.SPEED_II)
                });
        modeItems.put(1,
                new ItemStack[]{
                        ItemsUtil.getItemStack(Items.golden_apple),
                        ItemsUtil.getItemStack(Items.arrow),
                });

    }

    @Override
    public void draw(float x, float y) {
        super.draw(x, y);
        ItemCountDisplay displayModule = ((ItemCountDisplay) mod);
        ItemStack[] mainInventory = mc.thePlayer.inventory.mainInventory;

        if(displayModule.modes.getValue() != 2) itemStacks = Arrays.stream(modeItems.get(displayModule.modes.getValue())).collect(Collectors.toList());
        else itemStacks = displayModule.itemsSetting.getValue();
        int index = 0;
        for (ItemStack itemStack : itemStacks) {
            AtomicLong count = new AtomicLong();
            Arrays.stream(mainInventory)
                    .filter((stack) -> {
                        if (stack == null) {
                            return false;
                        }
                        return stack.getItem() == itemStack.getItem() && stack.getMetadata() == itemStack.getMetadata();
                    })
                    .collect(Collectors.toList())
                    .forEach((stack) -> count.addAndGet(stack.stackSize));
            float xOffset = x + index * (ITEM_WIDTH + mod.spacing.getValue().intValue());
            float textOffset = xOffset + 8f - (getStringWidth(20,String.valueOf(count)) / 2);
            drawRect(xOffset,y,ITEM_WIDTH,DISPLAY_HEIGHT, mod.backgroundColor.getColor());
            ItemsUtil.renderItem(itemStack, xOffset, y);
            drawString(20, String.valueOf(count), textOffset, y + ITEM_WIDTH, -1);
            index++;
        }
        width = index * (ITEM_WIDTH + mod.spacing.getValue().intValue());
        height = DISPLAY_HEIGHT;
    }


}

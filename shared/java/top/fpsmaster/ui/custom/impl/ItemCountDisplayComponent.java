package top.fpsmaster.ui.custom.impl;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import top.fpsmaster.features.impl.interfaces.ItemCountDisplay;
import top.fpsmaster.interfaces.ProviderManager;
import top.fpsmaster.ui.custom.Component;
import top.fpsmaster.utils.world.ItemsUtil;
import top.fpsmaster.utils.world.PotionMetadata;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import static top.fpsmaster.utils.Utility.mc;

public class ItemCountDisplayComponent extends Component {

    private static final float DISPLAY_HEIGHT = 27;
    private static final float ITEM_WIDTH = 17;
    public Map<Integer, ItemStack[]> modeItems = new HashMap<>();

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
        modeItems.put(2,
                new ItemStack[]{});
//        allowScale = true;
    }

    @Override
    public void draw(float x, float y) {
        super.draw(x, y);
        ItemCountDisplay displayModule = ((ItemCountDisplay) mod);
        ItemStack[] itemStacks = ProviderManager.mcProvider.getPlayer().inventory.mainInventory;
        int index = 0;
        for (ItemStack itemStack : modeItems.get(displayModule.mode.getValue())) {
            AtomicLong count = new AtomicLong();
            Arrays.stream(itemStacks)
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
            renderItem(itemStack, xOffset, y);
            drawString(20, String.valueOf(count), textOffset, y + ITEM_WIDTH, -1);
            index++;
        }
        width = index * (ITEM_WIDTH + mod.spacing.getValue().intValue());
        height = DISPLAY_HEIGHT;
    }

    public void renderItem(ItemStack itemStack, float x, float y) {
        GlStateManager.pushMatrix();
        GlStateManager.disableCull();
        GlStateManager.disableBlend();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);

        GlStateManager.enableRescaleNormal();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        RenderHelper.enableGUIStandardItemLighting();
        GlStateManager.pushMatrix();
        mc.getRenderItem().renderItemIntoGUI(itemStack, (int) x, (int) y);
        GlStateManager.popMatrix();
        mc.getRenderItem().renderItemOverlays(ProviderManager.mcProvider.getFontRenderer(), itemStack, (int) x, (int) y);

        RenderHelper.disableStandardItemLighting();
        GlStateManager.disableRescaleNormal();
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }
}

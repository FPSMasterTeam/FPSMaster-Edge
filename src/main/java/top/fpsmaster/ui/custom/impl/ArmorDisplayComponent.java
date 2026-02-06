package top.fpsmaster.ui.custom.impl;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.item.ItemStack;
import top.fpsmaster.features.impl.interfaces.ArmorDisplay;
import top.fpsmaster.ui.custom.Component;

import java.awt.*;
import java.util.Arrays;
import java.util.List;

import static top.fpsmaster.utils.core.Utility.mc;

public class ArmorDisplayComponent extends Component {

    public ArmorDisplayComponent() {
        super(ArmorDisplay.class);
    }

    @Override
    public void draw(float x, float y) {
        super.draw(x, y);
        List<ItemStack> armorInventory = Arrays.asList(mc.thePlayer.inventory.armorInventory);

        for (int i = 0; i < armorInventory.size(); i++) {
            ItemStack itemStack = armorInventory.get(i);
            int x1 = (int) (x + i * (mod.spacing.getValue().intValue() + 18)) - mod.spacing.getValue().intValue();
            int y1 = (int) y;

            switch (ArmorDisplay.mode.getValue()) {
                case 0:
                    itemStack = armorInventory.get(armorInventory.size() - 1 - i);
                    break;
                case 1:
                case 2:
                    itemStack = armorInventory.get(armorInventory.size() - 1 - i);
                    x1 = (int) x;
                    y1 = (int) (y + i * (mod.spacing.getValue().intValue() + 18)) - mod.spacing.getValue().intValue();
                    break;
            }

            drawRect(x1, y1, 16f, 16f, mod.backgroundColor.getColor());

            if (itemStack == null) continue;
            GlStateManager.disableCull();
            GlStateManager.disableBlend();
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);

            GlStateManager.enableRescaleNormal();
            GlStateManager.enableBlend();
            GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
            RenderHelper.enableGUIStandardItemLighting();


            GlStateManager.pushMatrix();
            mc.getRenderItem().renderItemIntoGUI(itemStack, x1, y1);
            GlStateManager.popMatrix();
            mc.getRenderItem().renderItemOverlays(mc.fontRendererObj, itemStack, x1, y1);

            RenderHelper.disableStandardItemLighting();
            GlStateManager.disableRescaleNormal();
            GlStateManager.disableBlend();

            if (ArmorDisplay.mode.getValue() == 2) {
                // Draw durability
                int durability = itemStack.getMaxDamage() - itemStack.getItemDamage();
                float dura = (float) durability / itemStack.getMaxDamage();
                int color = -1;

                if (dura < 0.5) {
                    color = (dura < 0.2) ? new Color(255, 20, 20).getRGB() : new Color(255, 255, 20).getRGB();
                }

                String durabilityString = durability > 0 ? durability + "/" + itemStack.getMaxDamage() : "0/" + itemStack.getMaxDamage();

                drawRect(
                        x1 + 18,
                        y1,
                        getStringWidth(16, durabilityString) + 4,
                        16f,
                        mod.backgroundColor.getColor()
                );

                drawString(16, durabilityString, x1 + 20, y1 + 2, color);
            }
        }

        switch (ArmorDisplay.mode.getValue()) {
            case 0:
                width = 70f + mod.spacing.getValue().intValue();
                height = 18f;
                break;
            case 1:
            case 2:
                width = 70f;
                height = 4 + mod.spacing.getValue().intValue() + armorInventory.size() * 16;
                break;
        }
    }
}




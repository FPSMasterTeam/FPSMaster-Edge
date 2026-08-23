package top.fpsmaster.ui.custom.impl;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.init.Items;
import top.fpsmaster.features.impl.interfaces.ArmorDisplay;
import top.fpsmaster.ui.custom.Component;

import java.awt.*;
import java.util.Arrays;
import java.util.List;

import static top.fpsmaster.utils.core.Utility.mc;

public class ArmorDisplayComponent extends Component {

    public ArmorDisplayComponent() {
        super(ArmorDisplay.class);
        allowScale = true;
    }

    private static final int SLOTS = 4;
    private static final float SLOT_SIZE = 16f;

    @Override
    public void measure() {
        float spacing = mod.spacing.getValue().floatValue();
        if (ArmorDisplay.mode.getValue() == 0) {
            width = 70f + spacing;
            height = 18f;
        } else {
            width = 70f;
            height = 4 + spacing + SLOTS * SLOT_SIZE;
        }
    }

    /**
     * Top-left of slot {@code index}, already scaled. Shared by draw() and backgroundShape() so the
     * blur mask lands on the same boxes — the default single-rectangle mask would cover the gaps
     * between slots, which this HUD leaves empty.
     */
    private float slotOffset(int index) {
        float spacing = mod.spacing.getValue().floatValue();
        // The step was previously left unscaled, so enlarging the HUD made the slots overlap.
        return (index * (spacing + 18) - spacing) * scale;
    }

    @Override
    public void backgroundShape(ShapeSink sink, float originX, float originY) {
        boolean horizontal = ArmorDisplay.mode.getValue() == 0;
        for (int i = 0; i < SLOTS; i++) {
            float offset = slotOffset(i);
            sink.rect(horizontal ? originX + offset : originX,
                    horizontal ? originY : originY + offset,
                    SLOT_SIZE, SLOT_SIZE);
        }
    }

    @Override
    public void draw(float x, float y) {
        super.draw(x, y);
        drawItems(x, y, Arrays.asList(mc.thePlayer.inventory.armorInventory));
    }

    @Override
    public void drawPreview(float x, float y) {
        drawItems(x, y, Arrays.asList(
                new ItemStack(Items.diamond_helmet),
                new ItemStack(Items.diamond_chestplate),
                new ItemStack(Items.diamond_leggings),
                new ItemStack(Items.diamond_boots)));
    }

    private void drawItems(float x, float y, List<ItemStack> armorInventory) {
        boolean horizontal = ArmorDisplay.mode.getValue() == 0;

        for (int i = 0; i < armorInventory.size(); i++) {
            ItemStack itemStack = armorInventory.get(armorInventory.size() - 1 - i);
            float offset = slotOffset(i);
            int x1 = (int) (horizontal ? x + offset : x);
            int y1 = (int) (horizontal ? y : y + offset);

            drawRect(x1, y1, SLOT_SIZE, SLOT_SIZE, mod.backgroundColor.getColor());

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

    }
}



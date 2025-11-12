package top.fpsmaster.ui.custom.impl;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import org.lwjgl.opengl.GL11;
import top.fpsmaster.features.impl.interfaces.InventoryDisplay;
import top.fpsmaster.api.ProviderManager;
import top.fpsmaster.ui.custom.Component;

import static top.fpsmaster.utils.Utility.mc;

public class InventoryDisplayComponent extends Component {

    public InventoryDisplayComponent() {
        super(InventoryDisplay.class);
        allowScale = true;
    }

    @Override
    public void draw(float x, float y) {
        super.draw(x, y);
        drawRect(x - 2, y, 164f, 64f, mod.backgroundColor.getColor());

        int count = 0;
        int count2 = 0;
        int linecount = 0;
        GlStateManager.pushMatrix();
        GL11.glTranslated(x, y, 0);
        if (scale != 1)
            GL11.glScaled(scale, scale, 0);
        for (Slot slot : ProviderManager.mcProvider.getPlayer().inventoryContainer.inventorySlots) {
            count2++;

            if (count2 <= 9 || count2 > 36) {
                continue;
            }

            if (slot.getStack() != null) {
                ItemStack itemStack = slot.getStack();
                int x1 = count * 18;
                int y1 = linecount * 20;

                GlStateManager.disableCull();
                GlStateManager.enableBlend();
                GlStateManager.disableBlend();
                RenderHelper.enableGUIStandardItemLighting();
                mc.getRenderItem().renderItemAndEffectIntoGUI(itemStack, x1, y1);
                mc.getRenderItem().renderItemOverlays(ProviderManager.mcProvider.getFontRenderer(), itemStack, x1, y1);
                RenderHelper.disableStandardItemLighting();

                GlStateManager.enableAlpha();
                GlStateManager.disableCull();
                GlStateManager.disableBlend();
                GlStateManager.disableLighting();
                GlStateManager.clear(256);
            }

            count++;
            if (count >= 9) {
                count = 0;
                linecount++;
            }
        }
        if (scale != 1)
            GL11.glScaled(1 / scale, 1 / scale, 0);
        GlStateManager.popMatrix();

        width = 164f;
        height = 64f;
    }
}

package top.fpsmaster.ui.custom.impl;

import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;
import top.fpsmaster.features.impl.interfaces.SaturationDisplay;
import top.fpsmaster.ui.custom.Component;
import top.fpsmaster.ui.custom.Position;
import top.fpsmaster.utils.render.draw.Images;

import java.awt.Color;

import static top.fpsmaster.utils.core.Utility.mc;

/** Draws the food icons and the exact saturation value in the HUD editor coordinate space. */
public class SaturationDisplayComponent extends Component {
    private static final ResourceLocation ICONS = new ResourceLocation("textures/gui/icons.png");
    private static final float ICON_SIZE = 9f;
    private static final float ICON_ROW_WIDTH = 90f;
    private static final float VALUE_GAP = 4f;

    public SaturationDisplayComponent() {
        super(SaturationDisplay.class);
        position = Position.RB;
        // Vanilla food HUD right edge is 10 px from the screen edge; HUD coordinates are doubled.
        x = 0.02f;
        y = 0.075f;
        allowScale = true;
    }

    @Override
    public void measure() {
        String value = saturationText();
        width = ICON_ROW_WIDTH + VALUE_GAP + getStringWidth(14, value) + 4f;
        height = 12f;
    }

    @Override
    public void draw(float x, float y) {
        super.draw(x, y);
        if (mc == null || mc.thePlayer == null) {
            return;
        }

        drawRect(x, y, width, height, mod.backgroundColor.getColor());

        int food = Math.max(0, Math.min(20, mc.thePlayer.getFoodStats().getFoodLevel()));
        GlStateManager.enableBlend();
        GlStateManager.color(1f, 1f, 1f, 1f);
        mc.getTextureManager().bindTexture(ICONS);
        for (int icon = 0; icon < 10; icon++) {
            float iconX = x + icon * ICON_SIZE * scale;
            drawIcon(iconX, y + 1f * scale, 16, 27);
            int halfFood = food - icon * 2;
            if (halfFood >= 2) {
                drawIcon(iconX, y + 1f * scale, 52, 27);
            } else if (halfFood == 1) {
                drawIcon(iconX, y + 1f * scale, 61, 27);
            }
        }
        GlStateManager.disableBlend();

        drawString(14, saturationText(), x + ICON_ROW_WIDTH * scale + VALUE_GAP * scale, y + 1f * scale, Color.WHITE.getRGB());
        GlStateManager.color(1f, 1f, 1f, 1f);
    }

    private void drawIcon(float x, float y, int textureX, int textureY) {
        // 四边形按 scale 放大，但采样跨度固定 ICON_SIZE，否则会把 icons.png 里相邻的图标一起采进来
        Images.drawModalRectWithCustomSizedTexture(x, y, textureX, textureY,
                ICON_SIZE * scale, ICON_SIZE * scale, ICON_SIZE, ICON_SIZE, 256f, 256f);
    }

    private String saturationText() {
        if (mc == null || mc.thePlayer == null) {
            return "0.0";
        }
        return String.format(java.util.Locale.ROOT, "%.1f", mc.thePlayer.getFoodStats().getSaturationLevel());
    }
}

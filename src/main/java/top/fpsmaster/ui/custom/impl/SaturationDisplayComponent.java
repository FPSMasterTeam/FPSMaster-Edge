package top.fpsmaster.ui.custom.impl;

import top.fpsmaster.features.impl.interfaces.SaturationDisplay;
import top.fpsmaster.ui.custom.Component;
import top.fpsmaster.ui.custom.Position;
import top.fpsmaster.utils.render.draw.Rects;

import java.awt.Color;

import static top.fpsmaster.utils.core.Utility.mc;

/**
 * 饱和度：一根 0–20 的条加精确数值。
 *
 * <p>只画饱和度本身，不碰原版饥饿条——饱和度是原版隐藏的数值，把它显示出来不需要接管一个
 * 本来就正常渲染的 HUD 元素。
 */
public class SaturationDisplayComponent extends Component {
    private static final float MAX_SATURATION = 20f;
    private static final float BAR_WIDTH = 40f;
    private static final float BAR_HEIGHT = 4f;
    private static final float GAP = 4f;
    private static final float PADDING = 2f;
    private static final Color TRACK = new Color(0, 0, 0, 120);

    public SaturationDisplayComponent() {
        super(SaturationDisplay.class);
        position = Position.RB;
        // 默认落在原版饥饿条上方，视觉上仍然和饥饿相关，但不遮挡它。
        x = 0.02f;
        y = 0.11f;
        allowScale = true;
    }

    @Override
    public void measure() {
        width = PADDING * 2f + BAR_WIDTH + GAP + getStringWidth(14, saturationText());
        height = 12f;
    }

    @Override
    public void draw(float x, float y) {
        super.draw(x, y);
        if (mc == null || mc.thePlayer == null) {
            return;
        }

        drawRect(x, y, width, height, mod.backgroundColor.getColor());

        float saturation = mc.thePlayer.getFoodStats().getSaturationLevel();
        saturation = Math.max(0f, Math.min(MAX_SATURATION, saturation));

        float barWidth = BAR_WIDTH * scale;
        float barHeight = BAR_HEIGHT * scale;
        float barX = x + PADDING * scale;
        float barY = y + (height * scale - barHeight) / 2f;

        Rects.fill(barX, barY, barWidth, barHeight, TRACK);
        if (saturation > 0f) {
            Rects.fill(barX, barY, barWidth * (saturation / MAX_SATURATION), barHeight,
                    SaturationDisplay.barColor.getColor());
        }

        drawString(14, saturationText(), barX + barWidth + GAP * scale, y + 1f * scale, Color.WHITE.getRGB());
    }

    private String saturationText() {
        if (mc == null || mc.thePlayer == null) {
            return "0.0";
        }
        return String.format(java.util.Locale.ROOT, "%.1f", mc.thePlayer.getFoodStats().getSaturationLevel());
    }
}

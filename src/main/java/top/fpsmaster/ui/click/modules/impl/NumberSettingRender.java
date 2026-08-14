package top.fpsmaster.ui.click.modules.impl;

import top.fpsmaster.FPSMaster;
import top.fpsmaster.features.manager.Module;
import top.fpsmaster.features.settings.impl.NumberSetting;
import top.fpsmaster.ui.click.ClickGuiTheme;
import top.fpsmaster.ui.click.modules.SettingRender;
import top.fpsmaster.ui.common.binding.SettingBinding;
import top.fpsmaster.utils.math.anim.AnimMath;
import top.fpsmaster.utils.render.draw.Rects;
import top.fpsmaster.utils.render.gui.ScaledGuiScreen;

import java.text.DecimalFormat;
import java.util.Locale;

public class NumberSettingRender extends SettingRender<NumberSetting> {
    private static final DecimalFormat DF = new DecimalFormat("#.##");
    private static final float TRACK_H = 2.5f;
    private static final float THUMB = 5.5f;

    private float aWidth = 0f;
    private final SettingBinding<Number> binding;
    private final String captureId;

    public NumberSettingRender(Module mod, NumberSetting setting) {
        super(setting);
        this.mod = mod;
        this.binding = new SettingBinding<>(setting);
        this.captureId = mod.name + ":" + setting.name + ":number";
    }

    @Override
    public void render(ScaledGuiScreen screen, float x, float y, float width, float height, float mouseX, float mouseY, boolean custom) {
        String label = FPSMaster.i18n.get((mod.name + "." + setting.name).toLowerCase(Locale.getDefault()));
        String valueText = DF.format(setting.getValue());
        float labelW = FPSMaster.fontManager.getFont(13).getStringWidth(label);
        // Fixed readout width (prototype .num: min-width + right-align): sized for the widest
        // value this setting can produce, so the slider track never shifts while dragging.
        float valueW = reservedValueWidth();
        float min = setting.min.floatValue();
        float max = setting.max.floatValue();
        float range = max - min;
        float percent = range == 0f ? 0f : (setting.getValue().floatValue() - min) / range;
        percent = Math.max(0f, Math.min(1f, percent));

        float sliderW = Math.min(72f, width - 15f - labelW - valueW);
        boolean stacked = sliderW < 30f;
        float rowH = stacked ? 28f : 19f;
        if (stacked) {
            sliderW = Math.max(36f, width - 15f - valueW);
        }

        FPSMaster.fontManager.getFont(13).drawString(label, x + 5, stacked ? y + 4 : y + 6, ClickGuiTheme.textPrimary().getRGB());

        float sliderX = stacked ? x + 5 : x + width - 5 - valueW - 5 - sliderW;
        float sliderY = stacked ? y + 17 : y + 8.25f;
        aWidth = (float) AnimMath.base(aWidth, sliderW * percent, 0.2);

        Rects.rounded(sliderX, sliderY, sliderW, TRACK_H, 1, ClickGuiTheme.layerActive().getRGB(), false);
        if (aWidth > 0.5f) {
            Rects.rounded(sliderX, sliderY, aWidth, TRACK_H, 1, ClickGuiTheme.sliderFill().getRGB(), false);
        }
        float thumbX = sliderX + Math.max(0f, Math.min(sliderW - THUMB, aWidth - THUMB / 2f));
        Rects.rounded(
                thumbX,
                sliderY + (TRACK_H - THUMB) / 2f,
                THUMB,
                THUMB,
                (int) (THUMB / 2f),
                0xFFFFFFFF,
                false
        );
        float textW = FPSMaster.fontManager.getFont(12).getStringWidth(valueText);
        FPSMaster.fontManager.getFont(12).drawString(
                valueText,
                sliderX + sliderW + 5 + (valueW - textW),
                sliderY - 2f,
                ClickGuiTheme.textSecondary().getRGB()
        );

        screen.beginPointerCapture(captureId, 0, sliderX, sliderY - 4, sliderW, TRACK_H + 8);
        if (screen.isPointerCapturedBy(captureId, 0)) {
            float mPercent = Math.max(0f, Math.min(1f, (mouseX - sliderX) / sliderW));
            binding.set(min + range * mPercent);
        }
        this.height = rowH;
    }

    /**
     * Width of the widest string {@link #DF} can produce for this setting's range: sign, the
     * integer digits of the larger bound, and two decimals ("8" is the widest digit glyph).
     */
    private float reservedValueWidth() {
        long largest = Math.max(Math.abs((long) setting.min.doubleValue()), Math.abs((long) setting.max.doubleValue()));
        StringBuilder template = new StringBuilder();
        if (setting.min.doubleValue() < 0) {
            template.append('-');
        }
        do {
            template.append('8');
            largest /= 10;
        } while (largest > 0);
        template.append(".88");
        return Math.max(17f, FPSMaster.fontManager.getFont(12).getStringWidth(template.toString()));
    }
}

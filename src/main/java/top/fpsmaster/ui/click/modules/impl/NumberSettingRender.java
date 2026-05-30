package top.fpsmaster.ui.click.modules.impl;

import top.fpsmaster.utils.render.draw.Hover;
import top.fpsmaster.utils.render.draw.Rects;

import top.fpsmaster.FPSMaster;
import top.fpsmaster.features.manager.Module;
import top.fpsmaster.ui.click.ClickGuiTheme;
import top.fpsmaster.features.settings.impl.NumberSetting;
import top.fpsmaster.ui.click.modules.SettingRender;
import top.fpsmaster.utils.math.anim.AnimMath;
import top.fpsmaster.ui.common.binding.SettingBinding;
import top.fpsmaster.utils.render.gui.ScaledGuiScreen;

import java.awt.*;
import java.text.DecimalFormat;
import java.util.Locale;

public class NumberSettingRender extends SettingRender<NumberSetting> {
    private static final DecimalFormat df = new DecimalFormat("#.##");
    private static final float SLIDER_WIDTH = 160f;
    // animation
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
        float fw = FPSMaster.fontManager.s16.drawString(
                FPSMaster.i18n.get((mod.name + "." + setting.name).toLowerCase(Locale.getDefault())),
                x + 10, y + 2, ClickGuiTheme.textSecondary().getRGB()
        );
        Rects.rounded(Math.round(x + 16 + fw), Math.round(y + 3), Math.round(SLIDER_WIDTH), 6, ClickGuiTheme.inputBg().getRGB());
        float percent = (setting.getValue().floatValue() - setting.min.floatValue()) / (setting.max.floatValue() - setting.min.floatValue());
        aWidth = (float) AnimMath.base(aWidth, SLIDER_WIDTH * percent, 0.2);
        Rects.rounded(Math.round(x + 16 + fw), Math.round(y + 3), Math.round(aWidth), 6, ClickGuiTheme.sliderFill().getRGB());
        FPSMaster.fontManager.s16.drawString(
                df.format(setting.getValue()),
                x + fw + 20 + SLIDER_WIDTH,
                y + 2,
                ClickGuiTheme.textSecondary().getRGB()
        );

        float labelWidth = FPSMaster.fontManager.s16.getStringWidth(
                FPSMaster.i18n.get((mod.name + "." + setting.name).toLowerCase(Locale.getDefault()))
        );
        screen.beginPointerCapture(captureId, 0, x + 16 + labelWidth, y, SLIDER_WIDTH, height);

        if (screen.isPointerCapturedBy(captureId, 0)) {
            float v = mouseX - x - 16 - labelWidth;
            float mPercent = v / SLIDER_WIDTH;
            float newValue = (setting.max.floatValue() - setting.min.floatValue()) * mPercent + setting.min.floatValue();
            binding.set(newValue);
        }
        this.height = 12f;
    }

}





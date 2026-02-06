package top.fpsmaster.ui.click.modules.impl;

import top.fpsmaster.utils.render.draw.Hover;
import top.fpsmaster.utils.render.draw.Rects;

import org.lwjgl.input.Mouse;
import top.fpsmaster.FPSMaster;
import top.fpsmaster.features.manager.Module;
import top.fpsmaster.features.settings.impl.NumberSetting;
import top.fpsmaster.ui.click.MainPanel;
import top.fpsmaster.ui.click.modules.SettingRender;
import top.fpsmaster.utils.math.animation.AnimationUtils;

import java.awt.*;
import java.text.DecimalFormat;
import java.util.Locale;

public class NumberSettingRender extends SettingRender<NumberSetting> {
    private static final DecimalFormat df = new DecimalFormat("#.##");
    // animation
    private float aWidth = 0f;

    public NumberSettingRender(Module mod, NumberSetting setting) {
        super(setting);
        this.mod = mod;
    }

    @Override
    public void render(float x, float y, float width, float height, float mouseX, float mouseY, boolean custom) {
        float fw = FPSMaster.fontManager.s16.drawString(
                FPSMaster.i18n.get((mod.name + "." + setting.name).toLowerCase(Locale.getDefault())),
                x + 10, y + 2, new Color(162, 162, 162).getRGB()
        );
        Rects.rounded(x + 16 + fw, y + 3, 160f, 6f, new Color(0,0,0,80));
        float percent = (setting.getValue().floatValue() - setting.min.floatValue()) / (setting.max.floatValue() - setting.min.floatValue());
        aWidth = (float) AnimationUtils.base(aWidth, 160 * percent, 0.2);
        Rects.rounded(x + 16 + fw, y + 3, aWidth, 6f, -1);
        FPSMaster.fontManager.s16.drawString(
                df.format(setting.getValue()),
                x + fw + 20 + 160,
                y + 2,
                new Color(128, 128, 128).getRGB()
        );
        if (!Mouse.isButtonDown(0)) MainPanel.dragLock = "null";
        if (MainPanel.dragLock.equals(mod.name + setting.name + 4)) {
            float v = mouseX - x - 16 - FPSMaster.fontManager.s16.getStringWidth(
                    FPSMaster.i18n.get((mod.name + "." + setting.name).toLowerCase(Locale.getDefault()))
            );
            float mPercent = v / 160;
            float newValue = (setting.max.floatValue() - setting.min.floatValue()) * mPercent + setting.min.floatValue();
            setting.setValue(newValue);
        }
        this.height = 12f;
    }

    @Override
    public void mouseClick(float x, float y, float width, float height, float mouseX, float mouseY, int btn) {
        float fw = FPSMaster.fontManager.s16.drawString(
                FPSMaster.i18n.get((mod.name + "." + setting.name).toLowerCase(Locale.getDefault())),
                x + 10, y + 2, new Color(182, 182, 182).getRGB()
        );
        if (Hover.is(x + 16 + fw, y, 160f, height, (int) mouseX, (int) mouseY) && Mouse.isButtonDown(0)) {
            if (btn == 0 && MainPanel.dragLock.equals("null")) {
                MainPanel.dragLock = mod.name + setting.name + 4;
            }
        }
    }
}





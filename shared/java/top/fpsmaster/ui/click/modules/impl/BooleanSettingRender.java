package top.fpsmaster.ui.click.modules.impl;

import top.fpsmaster.FPSMaster;
import top.fpsmaster.features.manager.Module;
import top.fpsmaster.features.settings.impl.BooleanSetting;
import top.fpsmaster.ui.click.modules.SettingRender;
import top.fpsmaster.utils.Utility;
import top.fpsmaster.utils.math.animation.ColorAnimation;
import top.fpsmaster.utils.math.animation.Type;
import top.fpsmaster.utils.render.Render2DUtils;

import java.awt.Color;
import java.util.Locale;

public class BooleanSettingRender extends SettingRender<BooleanSetting> {
    // animation
    private ColorAnimation box = new ColorAnimation(new Color(255, 255, 255, 0));

    public BooleanSettingRender(Module mod, BooleanSetting setting) {
        super(setting);
        this.mod = mod;
    }

    @Override
    public void render(float x, float y, float width, float height, float mouseX, float mouseY, boolean custom) {
        box.update();
        if (setting.getValue()) {
            box.start(box.getColor(), new Color(255, 255, 255), 0.2f, Type.EASE_IN_OUT_QUAD);
        } else {
            box.start(box.getColor(), new Color(129, 129, 129), 0.2f, Type.EASE_IN_OUT_QUAD);
        }
        Render2DUtils.drawOptimizedRoundedRect(x + 14, y + 3, 6f, 6f, 3, box.getColor().getRGB());
        FPSMaster.fontManager.s16.drawString(
            FPSMaster.i18n.get((mod.name + "." + setting.name).toLowerCase(Locale.getDefault())),
            x + 26, y + 1, FPSMaster.theme.getTextColorDescription().getRGB()
        );
        this.height = 12f;
    }

    @Override
    public void mouseClick(float x, float y, float width, float height, float mouseX, float mouseY, int btn) {
        if (Render2DUtils.isHovered(x, y, width, height, (int) mouseX, (int) mouseY)) {
            setting.toggle();
        }
    }
}

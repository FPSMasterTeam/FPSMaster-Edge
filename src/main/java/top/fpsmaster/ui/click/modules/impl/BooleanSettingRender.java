package top.fpsmaster.ui.click.modules.impl;

import top.fpsmaster.utils.render.draw.Hover;
import top.fpsmaster.utils.render.draw.Rects;

import top.fpsmaster.FPSMaster;
import top.fpsmaster.features.manager.Module;
import top.fpsmaster.ui.click.ClickGuiTheme;
import top.fpsmaster.features.settings.impl.BooleanSetting;
import top.fpsmaster.ui.click.modules.SettingRender;
import top.fpsmaster.ui.common.binding.SettingBinding;
import top.fpsmaster.utils.math.anim.ColorAnimator;
import top.fpsmaster.utils.math.anim.Easings;
import top.fpsmaster.utils.render.gui.ScaledGuiScreen;

import java.awt.*;
import java.util.Locale;

public class BooleanSettingRender extends SettingRender<BooleanSetting> {
    // animation
    private final ColorAnimator box = new ColorAnimator(new Color(255, 255, 255, 0));
    private final SettingBinding<Boolean> binding;

    public BooleanSettingRender(Module mod, BooleanSetting setting) {
        super(setting);
        this.mod = mod;
        this.binding = new SettingBinding<>(setting);
    }

    @Override
    public void render(ScaledGuiScreen screen, float x, float y, float width, float height, float mouseX, float mouseY, boolean custom) {
        box.update();
        if (binding.get()) {
            box.animateTo(ClickGuiTheme.toggleEnabled(), 0.2f, Easings.QUAD_IN_OUT);
        } else {
            box.animateTo(new Color(129, 129, 129), 0.2f, Easings.QUAD_IN_OUT);
        }
        Rects.rounded(Math.round(x + 14), Math.round(y + 3), 6, 6, 3, box.getColor().getRGB());
        FPSMaster.fontManager.s16.drawString(
            FPSMaster.i18n.get((mod.name + "." + setting.name).toLowerCase(Locale.getDefault())),
            x + 26, y + 1, ClickGuiTheme.textSecondary().getRGB()
        );

        if (screen.consumePressInBounds(x, y, width, height) != null) {
            binding.set(!binding.get());
        }
        this.height = 12f;
    }

}





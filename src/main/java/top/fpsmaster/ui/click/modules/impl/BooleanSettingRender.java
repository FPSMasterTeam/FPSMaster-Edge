package top.fpsmaster.ui.click.modules.impl;

import top.fpsmaster.FPSMaster;
import top.fpsmaster.features.manager.Module;
import top.fpsmaster.features.settings.impl.BooleanSetting;
import top.fpsmaster.ui.click.ClickGuiTheme;
import top.fpsmaster.ui.click.UiChrome;
import top.fpsmaster.ui.click.modules.SettingRender;
import top.fpsmaster.ui.common.binding.SettingBinding;
import top.fpsmaster.utils.math.anim.AnimMath;
import top.fpsmaster.utils.render.gui.ScaledGuiScreen;

import java.util.Locale;

public class BooleanSettingRender extends SettingRender<BooleanSetting> {
    private final SettingBinding<Boolean> binding;
    private float knobT;

    public BooleanSettingRender(Module mod, BooleanSetting setting) {
        super(setting);
        this.mod = mod;
        this.binding = new SettingBinding<>(setting);
        this.knobT = Boolean.TRUE.equals(setting.getValue()) ? 1f : 0f;
    }

    @Override
    public void render(ScaledGuiScreen screen, float x, float y, float width, float height, float mouseX, float mouseY, boolean custom) {
        boolean on = Boolean.TRUE.equals(binding.get());
        knobT = (float) AnimMath.base(knobT, on ? 1.0 : 0.0, 0.25f);

        FPSMaster.fontManager.getFont(13).drawString(
                FPSMaster.i18n.get((mod.name + "." + setting.name).toLowerCase(Locale.getDefault())),
                x + 5,
                y + 6,
                ClickGuiTheme.textPrimary().getRGB()
        );

        float switchX = x + width - 5 - UiChrome.SWITCH_SM_W;
        float switchY = y + (19f - UiChrome.SWITCH_SM_H) / 2f;
        UiChrome.drawSwitchSm(switchX, switchY, on, knobT);

        if (screen.consumePressInBounds(switchX, switchY, UiChrome.SWITCH_SM_W, UiChrome.SWITCH_SM_H) != null
                || screen.consumePressInBounds(x, y, width, 19f) != null) {
            binding.set(!on);
        }
        this.height = 19f;
    }
}

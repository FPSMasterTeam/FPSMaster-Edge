package top.fpsmaster.ui.click.modules.impl;

import top.fpsmaster.utils.render.draw.Hover;
import top.fpsmaster.utils.render.draw.Rects;

import org.lwjgl.input.Keyboard;
import top.fpsmaster.FPSMaster;
import top.fpsmaster.features.manager.Module;
import top.fpsmaster.ui.click.ClickGuiTheme;
import top.fpsmaster.features.settings.impl.BindSetting;
import top.fpsmaster.font.impl.UFontRenderer;
import top.fpsmaster.ui.click.MainPanel;
import top.fpsmaster.ui.click.modules.SettingRender;
import top.fpsmaster.utils.math.anim.ColorAnimator;
import top.fpsmaster.ui.common.binding.SettingBinding;
import top.fpsmaster.utils.render.gui.ScaledGuiScreen;

import java.awt.*;
import java.util.Locale;

public class BindSettingRender extends SettingRender<BindSetting> {
    ColorAnimator colorAnimation = new ColorAnimator();
    private final SettingBinding<Integer> binding;

    public BindSettingRender(Module module, BindSetting setting) {
        super(setting);
        this.mod = module;
        this.binding = new SettingBinding<>(setting);
    }

    @Override
    public void render(ScaledGuiScreen screen, float x, float y, float width, float height, float mouseX, float mouseY, boolean custom) {
        float fw = FPSMaster.fontManager.s16.drawString(
            FPSMaster.i18n.get((mod.name + "." + setting.name).toLowerCase(Locale.getDefault())),
            x + 10, y + 2, ClickGuiTheme.textPrimary().getRGB()
        );
        String keyName = Keyboard.getKeyName(binding.get());
        UFontRenderer s16b = FPSMaster.fontManager.s16;
        float width1 = 10 + s16b.getStringWidth(keyName);
        if (Hover.is(x + 15 + fw, y, width1, 14f, (int) mouseX, (int) mouseY)) {
            Rects.rounded(
                Math.round(x + 14.5f + fw),
                Math.round(y - 0.5f),
                Math.round(width1 + 1),
                13,
                ClickGuiTheme.bindBgInactive().getRGB()
            );
        }
        Rects.rounded(Math.round(x + 15 + fw), Math.round(y), Math.round(width1), 12, colorAnimation.getColor());
        s16b.drawString(keyName, x + 18 + fw, y + 2, ClickGuiTheme.textPrimary().getRGB());
        if (MainPanel.bindLock.equals(setting.name)) {
            colorAnimation.base(new Color(255,255,255,80));
        } else {
            colorAnimation.base(ClickGuiTheme.textFieldBg());
        }

        ScaledGuiScreen.PointerEvent click = screen.consumePressInBounds(x + 25 + fw, y, 10f + s16b.getStringWidth(keyName), 12f, 0);
        if (click != null && MainPanel.bindLock.isEmpty()) {
            MainPanel.bindLock = setting.name;
        }
        this.height = 16f;
    }

    @Override
    public void keyTyped(char typedChar, int keyCode) {
        if (MainPanel.bindLock.equals(setting.name)) {
            binding.set(Keyboard.getEventKey());
            MainPanel.bindLock = "";
        }
    }
}





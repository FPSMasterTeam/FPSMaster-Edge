package top.fpsmaster.ui.click.modules.impl;

import org.lwjgl.input.Keyboard;
import top.fpsmaster.FPSMaster;
import top.fpsmaster.features.manager.Module;
import top.fpsmaster.features.settings.impl.BindSetting;
import top.fpsmaster.font.impl.UFontRenderer;
import top.fpsmaster.ui.click.MainPanel;
import top.fpsmaster.ui.click.modules.SettingRender;
import top.fpsmaster.utils.Utility;
import top.fpsmaster.utils.math.animation.ColorAnimation;
import top.fpsmaster.utils.render.Render2DUtils;

import java.awt.*;
import java.util.Locale;

public class BindSettingRender extends SettingRender<BindSetting> {
    ColorAnimation colorAnimation = new ColorAnimation();

    public BindSettingRender(Module module, BindSetting setting) {
        super(setting);
        this.mod = module;
    }

    @Override
    public void render(float x, float y, float width, float height, float mouseX, float mouseY, boolean custom) {
        float fw = FPSMaster.fontManager.s16.drawString(
            FPSMaster.i18n.get((mod.name + "." + setting.name).toLowerCase(Locale.getDefault())),
            x + 10, y + 2, FPSMaster.theme.getTextColorTitle().getRGB()
        );
        String keyName = Keyboard.getKeyName(setting.getValue());
        UFontRenderer s16b = FPSMaster.fontManager.s16;
        float width1 = 10 + s16b.getStringWidth(keyName);
        if (Render2DUtils.isHovered(x + 15 + fw, y, width1, 14f, (int) mouseX, (int) mouseY)) {
            Render2DUtils.drawOptimizedRoundedRect(
                x + 14.5f + fw,
                y - 0.5f,
                width1 + 1,
                13f,
                new Color(0,0,0,80)
            );
        }
        Render2DUtils.drawOptimizedRoundedRect(x + 15 + fw, y, width1, 12f, colorAnimation.getColor());
        s16b.drawString(keyName, x + 18 + fw, y + 2, FPSMaster.theme.getTextColorTitle().getRGB());
        if (MainPanel.bindLock.equals(setting.name)) {
            colorAnimation.base(new Color(255,255,255,80));
        } else {
            colorAnimation.base(new Color(0,0,0,80));
        }
        this.height = 16f;
    }

    @Override
    public void mouseClick(float x, float y, float width, float height, float mouseX, float mouseY, int btn) {
        float fw = FPSMaster.fontManager.s16.getStringWidth(
            FPSMaster.i18n.get((mod.name + "." + setting.name).toLowerCase(Locale.getDefault()))
        );
        String keyName = Keyboard.getKeyName(setting.getValue());
        UFontRenderer s16b = FPSMaster.fontManager.s16;
        if (Render2DUtils.isHovered(
                x + 25 + fw,
                y,
                10f + s16b.getStringWidth(keyName),
                12f,
                (int) mouseX, (int) mouseY
            ) && btn == 0
        ) {
            if (MainPanel.bindLock.isEmpty()) {
                MainPanel.bindLock = setting.name;
            }
        }
    }

    @Override
    public void keyTyped(char typedChar, int keyCode) {
        if (MainPanel.bindLock.equals(setting.name)) {
            setting.setValue(Keyboard.getEventKey());
            MainPanel.bindLock = "";
        }
    }
}

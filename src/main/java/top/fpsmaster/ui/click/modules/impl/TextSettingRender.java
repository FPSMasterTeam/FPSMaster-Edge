package top.fpsmaster.ui.click.modules.impl;

import top.fpsmaster.utils.render.draw.Hover;

import top.fpsmaster.FPSMaster;
import top.fpsmaster.features.manager.Module;
import top.fpsmaster.features.settings.impl.TextSetting;
import top.fpsmaster.ui.click.modules.SettingRender;
import top.fpsmaster.ui.common.TextField;

import java.awt.*;
import java.util.Locale;

public class TextSettingRender extends SettingRender<TextSetting> {
    private final TextField inputBox;

    public TextSettingRender(Module mod, TextSetting setting) {
        super(setting);
        this.mod = mod;
        inputBox = new TextField(FPSMaster.fontManager.s16, false, "输入名称", -1, new Color(50, 50, 50).getRGB(), 1500);
        inputBox.setText(setting.getValue());
    }

    @Override
    public void render(float x, float y, float width, float height, float mouseX, float mouseY, boolean custom) {
        inputBox.backGroundColor = new Color(58, 58, 58).getRGB();
        inputBox.fontColor = new Color(234, 234, 234).getRGB();
        String text = FPSMaster.i18n.get((mod.name + "." + setting.name).toLowerCase(Locale.getDefault()));
        FPSMaster.fontManager.s16.drawString(text, x + 18, y + 6, new Color(162, 162, 162).getRGB());
        inputBox.drawTextBox(
                x + Math.max(FPSMaster.fontManager.s16.getStringWidth(inputBox.placeHolder), FPSMaster.fontManager.s16.getStringWidth(text)) + 20,
                y + 2,
                Math.max(FPSMaster.fontManager.s16.getStringWidth(inputBox.placeHolder), FPSMaster.fontManager.s18.getStringWidth(inputBox.getText())) + 20f,
                16f
        );
        this.height = 24f;
    }

    @Override
    public void mouseClick(float x, float y, float width, float height, float mouseX, float mouseY, int btn) {
        if (Hover.is(x, y, width, height, (int) mouseX, (int) mouseY)) {
            inputBox.mouseClicked((int) mouseX, (int) mouseY, btn);
        } else {
            inputBox.setFocused(false);
        }
    }

    @Override
    public void keyTyped(char typedChar, int keyCode) {
        inputBox.textboxKeyTyped(typedChar, keyCode);
        setting.setValue(inputBox.getText());
    }
}





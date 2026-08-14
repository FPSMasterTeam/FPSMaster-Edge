package top.fpsmaster.ui.click.modules.impl;

import top.fpsmaster.FPSMaster;
import top.fpsmaster.features.manager.Module;
import top.fpsmaster.ui.click.ClickGuiTheme;
import top.fpsmaster.ui.click.UiChrome;
import top.fpsmaster.features.settings.impl.TextSetting;
import top.fpsmaster.ui.click.modules.SettingRender;
import top.fpsmaster.ui.common.TextField;
import top.fpsmaster.ui.common.binding.SettingBinding;
import top.fpsmaster.ui.common.control.BoundTextFieldControl;
import top.fpsmaster.utils.render.gui.ScaledGuiScreen;

import java.util.Locale;

public class TextSettingRender extends SettingRender<TextSetting> {
    private final BoundTextFieldControl input;

    public TextSettingRender(Module mod, TextSetting setting) {
        super(setting);
        this.mod = mod;
        TextField inputBox = new TextField(FPSMaster.fontManager.getFont(12), false, "输入名称", -1, ClickGuiTheme.textFieldBg().getRGB(), 1500);
        String value = setting.getValue();
        if (isPlayTimeLabel() && (value == null || value.trim().isEmpty())) {
            value = getPlayTimeDefaultLabel();
            setting.setValue(value);
        }
        inputBox.setText(value);
        input = new BoundTextFieldControl(inputBox, new SettingBinding<>(setting));
    }

    @Override
    public void render(ScaledGuiScreen screen, float x, float y, float width, float height, float mouseX, float mouseY, boolean custom) {
        TextField inputBox = input.getTextField();
        if (isPlayTimeLabel() && (setting.getValue() == null || setting.getValue().trim().isEmpty())) {
            setting.setValue(getPlayTimeDefaultLabel());
        }
        inputBox.backGroundColor = 0;
        inputBox.fontColor = ClickGuiTheme.textFieldText().getRGB();
        String text = FPSMaster.i18n.get((mod.name + "." + setting.name).toLowerCase(Locale.getDefault()));
        float labelW = FPSMaster.fontManager.getFont(13).getStringWidth(text);
        FPSMaster.fontManager.getFont(13).drawString(text, x + 5, y + 6, ClickGuiTheme.textPrimary().getRGB());
        float fieldW = Math.min(72f, Math.max(40f, width - 15f - labelW));
        float fieldX = x + width - 5f - fieldW;
        UiChrome.inputBox(fieldX, y + 2f, fieldW, 15f, inputBox.isFocused());
        input.renderInScreen(screen, fieldX + 3f, y + 3f, fieldW - 6f, 13f, mouseX, mouseY);
        this.height = 19f;
    }

    private boolean isPlayTimeLabel() {
        return "PlayTime".equals(mod.name) && "Label".equals(setting.name);
    }

    private String getPlayTimeDefaultLabel() {
        String value = FPSMaster.i18n.get("playtime.defaultlabel");
        return "playtime.defaultlabel".equals(value) ? "游玩时间：" : value;
    }

    @Override
    public void keyTyped(char typedChar, int keyCode) {
        input.keyTyped(typedChar, keyCode);
    }
}

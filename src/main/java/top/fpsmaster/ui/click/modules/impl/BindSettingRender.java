package top.fpsmaster.ui.click.modules.impl;

import org.lwjgl.input.Keyboard;
import top.fpsmaster.FPSMaster;
import top.fpsmaster.features.manager.Module;
import top.fpsmaster.features.settings.impl.BindSetting;
import top.fpsmaster.ui.click.ClickGuiTheme;
import top.fpsmaster.ui.click.MainPanel;
import top.fpsmaster.ui.click.UiChrome;
import top.fpsmaster.ui.click.modules.SettingRender;
import top.fpsmaster.ui.common.binding.SettingBinding;
import top.fpsmaster.utils.render.draw.Hover;
import top.fpsmaster.utils.render.gui.ScaledGuiScreen;

import java.util.Locale;

public class BindSettingRender extends SettingRender<BindSetting> {
    private final SettingBinding<Integer> binding;

    public BindSettingRender(Module module, BindSetting setting) {
        super(setting);
        this.mod = module;
        this.binding = new SettingBinding<>(setting);
    }

    @Override
    public void render(ScaledGuiScreen screen, float x, float y, float width, float height, float mouseX, float mouseY, boolean custom) {
        FPSMaster.fontManager.getFont(13).drawString(
                FPSMaster.i18n.get((mod.name + "." + setting.name).toLowerCase(Locale.getDefault())),
                x + 5,
                y + 6,
                ClickGuiTheme.textPrimary().getRGB()
        );

        String keyName = UiChrome.keyName(binding.get());
        float chipW = UiChrome.keyChipWidth(keyName);
        float chipH = 11.5f;
        float chipX = x + width - 5 - chipW;
        float chipY = y + (19f - chipH) / 2f;
        boolean active = MainPanel.bindLock.equals(lockId());
        boolean hover = Hover.is(chipX, chipY, chipW, chipH, (int) mouseX, (int) mouseY);
        UiChrome.keyChip(chipX, chipY, chipW, chipH, keyName, active, hover);

        if (screen.consumePressInBounds(chipX, chipY, chipW, chipH, 0) != null) {
            MainPanel.bindLock = active ? "" : lockId();
        }
        this.height = 19f;
    }

    @Override
    public void keyTyped(char typedChar, int keyCode) {
        if (!MainPanel.bindLock.equals(lockId())) {
            return;
        }
        if (keyCode == Keyboard.KEY_ESCAPE) {
            MainPanel.bindLock = "";
            return;
        }
        if (keyCode == Keyboard.KEY_BACK || keyCode == Keyboard.KEY_DELETE) {
            binding.set(0);
        } else if (keyCode != Keyboard.KEY_NONE) {
            binding.set(keyCode);
        }
        MainPanel.bindLock = "";
    }

    private String lockId() {
        return "bind:" + mod.name + "." + setting.name;
    }
}

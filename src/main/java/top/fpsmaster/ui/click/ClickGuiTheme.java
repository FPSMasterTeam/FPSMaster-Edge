package top.fpsmaster.ui.click;

import top.fpsmaster.features.impl.interfaces.ClientSettings;

import java.awt.*;

public class ClickGuiTheme {
    public static boolean isLight() {
        return ClientSettings.theme.getValue() == 1;
    }

    // ClickGuiTheme author:Ukiyograin
    public static Color textPrimary() {
        return isLight() ? new Color(30, 30, 30) : new Color(234, 234, 234);
    }

    public static Color textSecondary() {
        return isLight() ? new Color(100, 100, 100) : new Color(162, 162, 162);
    }

    public static Color textDescription() {
        return isLight() ? new Color(70, 70, 70) : new Color(162, 162, 162);
    }

    public static Color textDisabled() {
        return isLight() ? new Color(140, 140, 140) : new Color(156, 156, 156);
    }

    public static Color categoryTextSelected() {
        return isLight() ? new Color(255, 255, 255) : new Color(0, 0, 0);
    }

    public static Color categoryTextUnselected() {
        return isLight() ? new Color(30, 30, 30) : new Color(255, 255, 255);
    }

    public static Color categoryHover() {
        return isLight() ? new Color(200, 200, 200) : new Color(70, 70, 70);
    }

    public static Color categoryBg() {
        return isLight() ? new Color(255, 255, 255, 200) : new Color(0, 0, 0, 200);
    }

    public static Color panelBg() {
        return isLight() ? new Color(235, 238, 248, 180) : new Color(0, 0, 0, 0);
    }

    public static Color moduleHeaderBg() {
        return isLight() ? new Color(0, 0, 0, 0) : new Color(0, 0, 0, 0);
    }

    public static Color categorySelection() {
        return isLight() ? new Color(40, 40, 40) : new Color(255, 255, 255);
    }

    public static Color settingsBg() {
        return isLight() ? new Color(195, 200, 220, 160) : new Color(100, 100, 100, 60);
    }

    public static Color moduleContentEnabled() {
        return isLight() ? new Color(40, 80, 220) : new Color(255, 255, 255);
    }

    public static Color moduleContentDisabled() {
        return isLight() ? new Color(0, 0, 0) : new Color(156, 156, 156);
    }

    public static Color toggleEnabled() {
        return new Color(89, 101, 241);
    }

    public static Color toggleDisabled() {
        return isLight() ? new Color(40, 40, 40) : new Color(255, 255, 255);
    }

    public static Color inputBg() {
        return isLight() ? new Color(200, 200, 200, 80) : new Color(0, 0, 0, 80);
    }

    public static Color sliderFill() {
        return isLight() ? new Color(89, 101, 241) : new Color(255, 255, 255);
    }

    public static Color modeBg() {
        return isLight() ? new Color(220, 220, 220) : new Color(52, 52, 52);
    }

    public static Color modeBorder() {
        return isLight() ? new Color(0, 0, 0, 50) : new Color(255, 255, 255, 50);
    }

    public static Color modeText() {
        return isLight() ? new Color(30, 30, 30) : new Color(234, 234, 234);
    }

    public static Color scrollbar() {
        return isLight() ? new Color(0, 0, 0, 100) : new Color(255, 255, 255, 100);
    }

    public static Color mask(int alpha) {
        return new Color(0, 0, 0, alpha);
    }

    public static Color pickerBg() {
        return isLight() ? new Color(180, 180, 180) : new Color(39, 39, 39);
    }

    public static Color hexText() {
        return isLight() ? new Color(30, 30, 30) : new Color(234, 234, 234);
    }

    public static Color bindBgActive() {
        return isLight() ? new Color(180, 180, 180, 80) : new Color(255, 255, 255, 80);
    }

    public static Color bindBgInactive() {
        return isLight() ? new Color(200, 200, 200, 80) : new Color(0, 0, 0, 80);
    }

    public static Color textFieldBg() {
        return isLight() ? new Color(200, 200, 200) : new Color(58, 58, 58);
    }

    public static Color textFieldText() {
        return isLight() ? new Color(30, 30, 30) : new Color(234, 234, 234);
    }

    public static Color itemBg() {
        return isLight() ? new Color(180, 180, 180, 120) : new Color(50, 50, 50, 120);
    }

    public static Color itemContainerBg() {
        return isLight() ? new Color(160, 160, 160, 160) : new Color(80, 80, 80, 160);
    }

    public static Color buttonBg() {
        return isLight() ? new Color(160, 160, 160, 140) : new Color(70, 70, 70, 140);
    }

    public static Color buttonHoverBg() {
        return isLight() ? new Color(130, 130, 130, 140) : new Color(120, 120, 120, 140);
    }

    public static Color themeBtnBg() {
        return isLight() ? new Color(255, 255, 255, 180) : new Color(0, 0, 0, 150);
    }

    public static Color themeBtnText() {
        return isLight() ? new Color(30, 30, 30) : new Color(200, 200, 200);
    }

    public static Color modeSelectBg() {
        return isLight() ? new Color(40, 40, 40) : new Color(52, 52, 52);
    }
}

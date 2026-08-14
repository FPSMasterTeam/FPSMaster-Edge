package top.fpsmaster.ui.click;

import top.fpsmaster.features.impl.interfaces.ClientSettings;

import java.awt.Color;

public class ClickGuiTheme {
    public static boolean isLight() {
        return ClientSettings.theme.getValue() == 1;
    }

    public static Color textPrimary() {
        return isLight() ? new Color(30, 30, 30) : new Color(242, 242, 242);
    }

    public static Color textSecondary() {
        return isLight() ? new Color(100, 100, 100) : new Color(154, 154, 154);
    }

    public static Color textDescription() {
        return isLight() ? new Color(120, 120, 120) : new Color(92, 92, 92);
    }

    public static Color textDisabled() {
        return isLight() ? new Color(140, 140, 140) : new Color(92, 92, 92);
    }

    public static Color categoryTextSelected() {
        return Color.WHITE;
    }

    public static Color categoryTextUnselected() {
        return isLight() ? new Color(70, 70, 70) : new Color(154, 154, 154);
    }

    public static Color categoryHover() {
        return layerHover();
    }

    public static Color categoryBg() {
        return isLight() ? new Color(255, 255, 255, 140) : new Color(0, 0, 0, 36);
    }

    public static Color panelBg() {
        return glass();
    }

    /**
     * Primary panel fill. With backdrop blur on this can stay translucent (edge-ui.css --glass);
     * with blur off it falls back to the nearly-opaque --glass-solid so text stays readable over
     * a bright world.
     */
    public static Color glass() {
        if (isLight()) {
            return new Color(246, 246, 246, 235);
        }
        return top.fpsmaster.features.impl.interfaces.ClientSettings.blur.getValue()
                ? new Color(18, 18, 18, 209)
                : new Color(14, 14, 14, 240);
    }

    /** Screen-dimming layer behind the world on full-bleed screens (edge-ui.css --veil). */
    public static Color veil() {
        return new Color(0, 0, 0, 148);
    }

    public static Color stroke() {
        return isLight() ? new Color(0, 0, 0, 26) : new Color(255, 255, 255, 20);
    }

    public static Color strokeStrong() {
        return isLight() ? new Color(0, 0, 0, 48) : new Color(255, 255, 255, 41);
    }

    public static Color layer() {
        return isLight() ? new Color(0, 0, 0, 10) : new Color(255, 255, 255, 11);
    }

    public static Color layerHover() {
        return isLight() ? new Color(0, 0, 0, 16) : new Color(255, 255, 255, 20);
    }

    public static Color layerActive() {
        return isLight() ? new Color(0, 0, 0, 28) : new Color(255, 255, 255, 31);
    }

    public static Color moduleHeaderBg() {
        return new Color(0, 0, 0, 0);
    }

    public static Color categorySelection() {
        return accent();
    }

    public static Color settingsBg() {
        return isLight() ? new Color(0, 0, 0, 12) : new Color(0, 0, 0, 46);
    }

    public static Color moduleContentEnabled() {
        return textPrimary();
    }

    public static Color moduleContentDisabled() {
        return isLight() ? new Color(90, 90, 90) : new Color(156, 156, 156);
    }

    public static Color toggleEnabled() {
        return accent();
    }

    public static Color toggleDisabled() {
        return layerActive();
    }

    public static Color inputBg() {
        return isLight() ? new Color(0, 0, 0, 18) : new Color(0, 0, 0, 64);
    }

    public static Color sliderFill() {
        return accent();
    }

    public static Color modeBg() {
        return isLight() ? new Color(220, 220, 220) : new Color(0, 0, 0, 64);
    }

    public static Color modeBorder() {
        return stroke();
    }

    public static Color modeText() {
        return textPrimary();
    }

    public static Color scrollbar() {
        return isLight() ? new Color(0, 0, 0, 80) : new Color(255, 255, 255, 36);
    }

    public static Color mask(int alpha) {
        return new Color(0, 0, 0, alpha);
    }

    public static Color pickerBg() {
        return isLight() ? new Color(180, 180, 180) : new Color(28, 28, 28);
    }

    public static Color hexText() {
        return textPrimary();
    }

    public static Color bindBgActive() {
        return accentSoft();
    }

    public static Color bindBgInactive() {
        return inputBg();
    }

    public static Color textFieldBg() {
        return inputBg();
    }

    public static Color textFieldText() {
        return textPrimary();
    }

    public static Color itemBg() {
        return layer();
    }

    public static Color itemContainerBg() {
        return isLight() ? new Color(0, 0, 0, 16) : new Color(0, 0, 0, 50);
    }

    public static Color buttonBg() {
        return layer();
    }

    public static Color buttonHoverBg() {
        return layerHover();
    }

    public static Color themeBtnBg() {
        return new Color(0, 0, 0, 0);
    }

    public static Color themeBtnText() {
        return textSecondary();
    }

    public static Color modeSelectBg() {
        return layerActive();
    }

    public static Color accent() {
        return new Color(89, 101, 241);
    }

    public static Color accentHover() {
        return new Color(107, 118, 255);
    }

    public static Color accentSoft() {
        return new Color(89, 101, 241, 41);
    }

    public static Color accentText() {
        return new Color(170, 178, 255);
    }

    public static Color danger() {
        return isLight() ? new Color(214, 69, 69) : new Color(240, 80, 110);
    }

    public static Color dangerSoft() {
        return new Color(240, 80, 110, 31);
    }

    public static Color ok() {
        return new Color(62, 207, 142);
    }

    /** Border of a selected/accented card (rgba(89,101,241,0.4) in the prototypes). */
    public static Color accentBorder() {
        return new Color(89, 101, 241, 102);
    }

    public static Color sideBtnHoverBg() {
        return layerHover();
    }

    public static Color cardBg() {
        // A touch above --layer: with the blur-off panel fallback being darker than the
        // prototype's blurred glass, the row would otherwise disappear into the panel.
        return isLight() ? new Color(0, 0, 0, 10) : new Color(255, 255, 255, 16);
    }

    public static Color cardHoverBg() {
        return isLight() ? new Color(0, 0, 0, 18) : new Color(255, 255, 255, 26);
    }

    public static Color cardExpanded() {
        return isLight() ? new Color(0, 0, 0, 18) : new Color(0, 0, 0, 46);
    }

    public static Color divider() {
        return stroke();
    }
}

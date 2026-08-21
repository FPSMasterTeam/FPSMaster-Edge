package top.fpsmaster.ui.click;

import org.lwjgl.input.Keyboard;
import top.fpsmaster.FPSMaster;
import top.fpsmaster.font.impl.UFontRenderer;
import top.fpsmaster.ui.kit.EdgeUi;
import top.fpsmaster.utils.render.draw.Hover;
import top.fpsmaster.utils.render.draw.Icons;
import top.fpsmaster.utils.render.gui.ScaledGuiScreen;
import top.fpsmaster.uikit.theme.Metrics;
import top.fpsmaster.uikit.widget.Chrome;
import top.fpsmaster.uikit.widget.UiFrame;

import java.awt.Color;

/**
 * Shared chrome for the Edge UI — façade over {@code fpsmaster-ui} {@link Chrome}.
 *
 * <p>Sizing convention: 1 GUI unit = 1 prototype CSS px / 2. All widgets are draw-only;
 * hit-testing stays with the caller via {@link ScaledGuiScreen#consumePressInBounds}.
 */
public final class UiChrome {
    public static final int PANEL_RADIUS = (int) Metrics.PANEL_RADIUS;
    public static final int CARD_RADIUS = (int) Metrics.CARD_RADIUS;
    public static final int CTL_RADIUS = (int) Metrics.CTL_RADIUS;

    public static final float BTN_H = Metrics.BTN_H;
    public static final float BTN_PAD_X = Metrics.BTN_PAD_X;
    public static final float SWITCH_W = Metrics.SWITCH_W;
    public static final float SWITCH_H = Metrics.SWITCH_H;
    public static final float SWITCH_SM_W = Metrics.SWITCH_SM_W;
    public static final float SWITCH_SM_H = Metrics.SWITCH_SM_H;
    public static final float SLIDER_W = Metrics.SLIDER_W;
    public static final float SLIDER_H = Metrics.SLIDER_H;
    public static final float INPUT_H = Metrics.INPUT_H;
    public static final float SEARCH_H = Metrics.SEARCH_H;
    public static final float KEYCHIP_H = Metrics.KEYCHIP_H;
    public static final float NAV_ITEM = Metrics.NAV_ITEM;
    public static final float MODULE_ROW = Metrics.MODULE_ROW;
    public static final float SIDEBAR = Metrics.SIDEBAR;

    private UiChrome() {
    }

    private static UiFrame ui() {
        return EdgeUi.frame();
    }

    public static void panel(float x, float y, float width, float height) {
        Chrome.panel(ui(), x, y, width, height);
    }

    public static void panel(float x, float y, float width, float height, int radius) {
        Chrome.panel(ui(), x, y, width, height, radius);
    }

    public static void veil(float guiWidth, float guiHeight, float alpha) {
        Chrome.veil(ui(), alpha);
    }

    public static void card(float x, float y, float width, float height, boolean hover, boolean expanded) {
        Chrome.card(ui(), x, y, width, height, hover, expanded);
    }

    public static void selectedCard(float x, float y, float width, float height) {
        Chrome.selectedCard(ui(), x, y, width, height);
    }

    public static void selectedSurface(float x, float y, float width, float height, int radius) {
        Chrome.selectedSurface(ui(), x, y, width, height, radius);
    }

    public static void hairlineH(float x, float y, float width) {
        Chrome.hairlineH(ui(), x, y, width);
    }

    public static void hairlineV(float x, float y, float height) {
        Chrome.hairlineV(ui(), x, y, height);
    }

    public static void accentMark(float x, float y, float height) {
        Chrome.accentMark(ui(), x, y, height);
    }

    public static void fillButton(float x, float y, float width, float height, boolean hover, boolean danger) {
        Chrome.fillButton(ui(), x, y, width, height, hover, danger);
    }

    public static void button(float x, float y, float width, float height, boolean hover) {
        Chrome.button(ui(), x, y, width, height, hover);
    }

    public static void dangerButton(float x, float y, float width, float height, boolean hover) {
        Chrome.dangerButton(ui(), x, y, width, height, hover);
    }

    public static void ghostButton(float x, float y, float width, float height, boolean hover) {
        Chrome.ghostButton(ui(), x, y, width, height, hover);
    }

    public static void iconButton(float x, float y, float size, boolean hover) {
        button(x, y, size, size, hover);
    }

    public static void pillIconButton(float x, float y, float size, boolean hover) {
        Chrome.button(ui(), x, y, size, size, hover);
    }

    public static boolean buttonClicked(ScaledGuiScreen screen, float x, float y, float width, float height,
                                        String icon, String label, Style style, int mouseX, int mouseY) {
        boolean hover = Hover.is(x, y, width, height, mouseX, mouseY);
        int textColor;
        switch (style) {
            case PRIMARY:
                fillButton(x, y, width, height, hover, false);
                textColor = 0xFFFFFFFF;
                break;
            case DANGER:
                dangerButton(x, y, width, height, hover);
                textColor = ClickGuiTheme.danger().getRGB();
                break;
            case DANGER_FILL:
                fillButton(x, y, width, height, hover, true);
                textColor = 0xFFFFFFFF;
                break;
            case GHOST:
                ghostButton(x, y, width, height, hover);
                textColor = (hover ? ClickGuiTheme.textPrimary() : ClickGuiTheme.textSecondary()).getRGB();
                break;
            default:
                button(x, y, width, height, hover);
                textColor = ClickGuiTheme.textPrimary().getRGB();
                break;
        }
        UFontRenderer font = FPSMaster.fontManager.s14;
        float iconSize = 7.5f;
        float labelW = label == null || label.isEmpty() ? 0f : font.getStringWidth(label);
        float contentW = labelW + (icon != null ? iconSize + (labelW > 0 ? 4f : 0f) : 0f);
        float cx = x + (width - contentW) / 2f;
        if (icon != null) {
            Icons.draw(icon, cx, y + (height - iconSize) / 2f, iconSize, textColor);
            cx += iconSize + (labelW > 0 ? 4f : 0f);
        }
        if (labelW > 0) {
            font.drawString(label, cx, y + height / 2f - 3.5f, textColor);
        }
        return screen.consumePressInBounds(x, y, width, height, 0) != null;
    }

    public enum Style {
        DEFAULT, PRIMARY, DANGER, DANGER_FILL, GHOST
    }

    public static void drawSwitch(float x, float y, boolean on, float knobT) {
        Chrome.drawSwitch(ui(), x, y, on, knobT);
    }

    public static void drawSwitchSm(float x, float y, boolean on, float knobT) {
        Chrome.drawSwitchSm(ui(), x, y, on, knobT);
    }

    public static void slider(float x, float y, float width, float t, boolean showThumb) {
        Chrome.slider(ui(), x, y, width, t, showThumb);
    }

    public static void seg(float x, float y, float width, float height) {
        Chrome.inputBox(ui(), x, y, width, height, false);
    }

    public static void segOption(float x, float y, float width, float height, String label,
                                 boolean selected, boolean hover) {
        if (selected) {
            ui().canvas().fillRoundRect(x, y, width, height, 3f, EdgeUi.theme().accent());
        }
        int color = selected
                ? 0xFFFFFFFF
                : (hover ? ClickGuiTheme.textPrimary() : ClickGuiTheme.textSecondary()).getRGB();
        FPSMaster.fontManager.getFont(12).drawCenteredString(label, x + width / 2f, y + height / 2f - 3f, color);
    }

    public static void inputBox(float x, float y, float width, float height, boolean focused) {
        Chrome.inputBox(ui(), x, y, width, height, focused);
    }

    public static void searchBox(float x, float y, float width, float height, boolean focused) {
        Chrome.searchBox(ui(), x, y, width, height, focused);
    }

    public static String keyName(int key) {
        if (key <= 0) {
            return FPSMaster.i18n.get("clickgui.bind.none");
        }
        String name = Keyboard.getKeyName(key);
        if (name == null || name.isEmpty() || "NONE".equalsIgnoreCase(name)) {
            return FPSMaster.i18n.get("clickgui.bind.none");
        }
        return name;
    }

    public static float keyChipWidth(String text) {
        return Math.max(19f, FPSMaster.fontManager.getFont(11).getStringWidth(text) + 8f);
    }

    public static void keyChip(float x, float y, float width, float height, String text, boolean active, boolean hover) {
        Chrome.keyChip(ui(), x, y, width, height, text, active, hover);
    }

    public static void navItem(float x, float y, float width, float height, boolean selected, boolean hover) {
        Chrome.navItem(ui(), x, y, width, height, selected, hover);
    }

    public static float badge(float x, float y, String text) {
        return Chrome.badge(ui(), x, y, text);
    }

    public static void pingBars(float x, float baselineY, int level, Color litColor) {
        for (int i = 0; i < 4; i++) {
            float h = 2f + i;
            int c = i < level ? litColor.getRGB() : ClickGuiTheme.layerActive().getRGB();
            ui().canvas().fillRoundRect(x + i * 2.25f, baselineY - h, 1.5f, h, 1f, c);
        }
    }

    public static int pingLevel(long pingMs) {
        if (pingMs < 0) {
            return 0;
        }
        if (pingMs < 80) {
            return 4;
        }
        if (pingMs < 150) {
            return 3;
        }
        if (pingMs < 300) {
            return 2;
        }
        return 1;
    }

    public static Color pingColor(long pingMs) {
        if (pingMs < 0) {
            return ClickGuiTheme.layerActive();
        }
        if (pingMs < 150) {
            return ClickGuiTheme.ok();
        }
        if (pingMs < 300) {
            return new Color(226, 185, 61);
        }
        return ClickGuiTheme.danger();
    }

    public static void boldString(UFontRenderer font, String text, float x, float y, int color) {
        font.drawString(text, x, y, color);
        font.drawString(text, x + 0.4f, y, color);
    }

    public static void boldCentered(UFontRenderer font, String text, float x, float y, int color) {
        float w = font.getStringWidth(text);
        boldString(font, text, x - w / 2f, y, color);
    }
}

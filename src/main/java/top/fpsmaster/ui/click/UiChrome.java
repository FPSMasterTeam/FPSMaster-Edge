package top.fpsmaster.ui.click;

import org.lwjgl.input.Keyboard;
import top.fpsmaster.FPSMaster;
import top.fpsmaster.features.impl.interfaces.ClientSettings;
import top.fpsmaster.font.impl.UFontRenderer;
import top.fpsmaster.utils.render.draw.Hover;
import top.fpsmaster.utils.render.draw.Icons;
import top.fpsmaster.utils.render.draw.Rects;
import top.fpsmaster.utils.render.effects.Blur;
import top.fpsmaster.utils.render.gui.ScaledGuiScreen;

import java.awt.Color;

/**
 * Shared chrome for the Edge UI — the runtime twin of {@code docs/prototypes/edge-ui.css}.
 *
 * <p>Sizing convention: 1 GUI unit = 1 prototype CSS px / 2 (see the header comment of
 * edge-ui.css). Text drawn with {@code fontManager.sN} renders N/2 units tall, so an HTML
 * font-size of 13.5px maps to {@code s14}, 12px to {@code getFont(12)}, and so on. At the default
 * ui scale of 2 this reproduces the prototype pixel-for-pixel.
 *
 * <p>All widgets are draw-only; hit-testing stays with the caller via
 * {@link ScaledGuiScreen#consumePressInBounds}. The few {@code *Clicked} helpers bundle
 * draw + hover + consume for the common button case.
 */
public final class UiChrome {
    // --- radius tokens (edge-ui.css: 18 / 12 / 10 px) ---
    public static final int PANEL_RADIUS = 9;
    public static final int CARD_RADIUS = 6;
    public static final int CTL_RADIUS = 5;

    // --- control metrics (HTML px / 2) ---
    public static final float BTN_H = 18f;           // .btn height 36px
    public static final float BTN_PAD_X = 9f;        // .btn padding 0 18px
    public static final float SWITCH_W = 22f;        // .switch 44x24
    public static final float SWITCH_H = 12f;
    public static final float SWITCH_SM_W = 18f;     // .switch.sm 36x20
    public static final float SWITCH_SM_H = 10f;
    public static final float SLIDER_W = 80f;        // .slider width 160px
    public static final float SLIDER_H = 12f;
    public static final float INPUT_H = 17f;         // .input height 34px
    public static final float SEARCH_H = 17f;        // .search height 34px
    public static final float KEYCHIP_H = 13f;       // .keybind height 26px
    public static final float NAV_ITEM = 19f;        // .nav .item height 38px
    public static final float MODULE_ROW = 22f;      // clickgui .module .head min-height 44px
    public static final float SIDEBAR = 92f;         // clickgui .side width 184px

    private UiChrome() {
    }

    // ------------------------------------------------------------------
    // Panels & layers
    // ------------------------------------------------------------------

    /** Glass panel: optional Kawase blur behind, translucent fill, hairline border. */
    public static void panel(float x, float y, float width, float height) {
        panel(x, y, width, height, PANEL_RADIUS);
    }

    public static void panel(float x, float y, float width, float height, int radius) {
        if (ClientSettings.blur.getValue()) {
            Blur.area(x, y, width, height, radius, new Color(255, 255, 255, 255), 3, 3);
        }
        Rects.rounded(x - 0.5f, y - 0.5f, width + 1f, height + 1f, radius + 1,
                ClickGuiTheme.stroke().getRGB(), false);
        Rects.rounded(x, y, width, height, radius, ClickGuiTheme.glass().getRGB(), false);
    }

    /** Full-screen dimming veil drawn between the world and a floating panel. */
    public static void veil(float guiWidth, float guiHeight, float alpha) {
        Color veil = ClickGuiTheme.veil();
        int a = Math.max(0, Math.min(255, (int) (veil.getAlpha() * alpha)));
        Rects.fill(0f, 0f, guiWidth, guiHeight, new Color(0, 0, 0, a).getRGB());
    }

    /** Secondary surface: row / card resting on a panel. */
    public static void card(float x, float y, float width, float height, boolean hover, boolean expanded) {
        Color fill = expanded ? ClickGuiTheme.cardExpanded() : (hover ? ClickGuiTheme.cardHoverBg() : ClickGuiTheme.cardBg());
        if (expanded) {
            Rects.rounded(x - 0.5f, y - 0.5f, width + 1f, height + 1f, CARD_RADIUS + 1,
                    ClickGuiTheme.stroke().getRGB(), false);
        }
        Rects.rounded(x, y, width, height, CARD_RADIUS, fill.getRGB(), false);
    }

    /** Selected list row: accent-soft fill + accent border + left accent mark. */
    public static void selectedCard(float x, float y, float width, float height) {
        selectedSurface(x, y, width, height, CARD_RADIUS);
        accentMark(x, y + height * 0.22f, height * 0.56f);
    }

    /**
     * Accent-selected surface. The border underlay is a full rect, so the translucent
     * accent-soft fill must sit on an opaque base or the border colour bleeds through
     * the whole card.
     */
    public static void selectedSurface(float x, float y, float width, float height, int radius) {
        Rects.rounded(x - 0.5f, y - 0.5f, width + 1f, height + 1f, radius + 1,
                ClickGuiTheme.accentBorder().getRGB(), false);
        Rects.rounded(x, y, width, height, radius, new Color(14, 14, 14, 255).getRGB(), false);
        Rects.rounded(x, y, width, height, radius, ClickGuiTheme.accentSoft().getRGB(), false);
    }

    public static void hairlineH(float x, float y, float width) {
        Rects.fill(x, y, width, 0.5f, ClickGuiTheme.divider());
    }

    public static void hairlineV(float x, float y, float height) {
        Rects.fill(x, y, 0.5f, height, ClickGuiTheme.divider());
    }

    /** 1.5-unit (3px) accent bar used as the "selected / hovered" signature mark. */
    public static void accentMark(float x, float y, float height) {
        Rects.rounded(x, y, 1.5f, height, 1, ClickGuiTheme.accent().getRGB(), false);
    }

    // ------------------------------------------------------------------
    // Buttons
    // ------------------------------------------------------------------

    public static void fillButton(float x, float y, float width, float height, boolean hover, boolean danger) {
        Color fill;
        if (danger) {
            fill = hover ? new Color(214, 60, 90) : ClickGuiTheme.danger();
        } else {
            fill = hover ? ClickGuiTheme.accentHover() : ClickGuiTheme.accent();
        }
        Rects.rounded(x, y, width, height, CTL_RADIUS, fill.getRGB(), false);
    }

    /** Default surface button (.btn): layer fill + hairline border. */
    public static void button(float x, float y, float width, float height, boolean hover) {
        Rects.rounded(x - 0.5f, y - 0.5f, width + 1f, height + 1f, CTL_RADIUS + 1,
                (hover ? ClickGuiTheme.strokeStrong() : ClickGuiTheme.stroke()).getRGB(), false);
        Rects.rounded(x, y, width, height, CTL_RADIUS,
                (hover ? ClickGuiTheme.layerHover() : ClickGuiTheme.layer()).getRGB(), false);
    }

    /** .btn.danger: layer surface, red content; hover warms the surface. */
    public static void dangerButton(float x, float y, float width, float height, boolean hover) {
        if (hover) {
            Rects.rounded(x - 0.5f, y - 0.5f, width + 1f, height + 1f, CTL_RADIUS + 1,
                    new Color(240, 80, 110, 77).getRGB(), false);
            Rects.rounded(x, y, width, height, CTL_RADIUS, ClickGuiTheme.dangerSoft().getRGB(), false);
        } else {
            button(x, y, width, height, false);
        }
    }

    /** .btn.ghost: transparent at rest, layer on hover. */
    public static void ghostButton(float x, float y, float width, float height, boolean hover) {
        if (hover) {
            Rects.rounded(x, y, width, height, CTL_RADIUS, ClickGuiTheme.layerHover().getRGB(), false);
        }
    }

    public static void iconButton(float x, float y, float size, boolean hover) {
        button(x, y, size, size, hover);
    }

    /** Round pill icon button (main-menu .top-actions). */
    public static void pillIconButton(float x, float y, float size, boolean hover) {
        Rects.rounded(x - 0.5f, y - 0.5f, size + 1f, size + 1f, (int) (size / 2f) + 1,
                (hover ? ClickGuiTheme.strokeStrong() : ClickGuiTheme.stroke()).getRGB(), false);
        Rects.rounded(x, y, size, size, (int) (size / 2f),
                (hover ? ClickGuiTheme.layerHover() : ClickGuiTheme.layer()).getRGB(), false);
    }

    /** Draw + hit in one call: a labeled button with optional icon; returns true when clicked. */
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

    // ------------------------------------------------------------------
    // Form controls
    // ------------------------------------------------------------------

    /** iOS switch, default size (.switch 44x24). knobT: 0..1 animated position. */
    public static void drawSwitch(float x, float y, boolean on, float knobT) {
        drawSwitchSized(x, y, SWITCH_W, SWITCH_H, on, knobT);
    }

    /** Compact switch (.switch.sm 36x20) used in dense rows. */
    public static void drawSwitchSm(float x, float y, boolean on, float knobT) {
        drawSwitchSized(x, y, SWITCH_SM_W, SWITCH_SM_H, on, knobT);
    }

    private static void drawSwitchSized(float x, float y, float w, float h, boolean on, float knobT) {
        float t = Math.max(0f, Math.min(1f, knobT));
        Color track = blend(ClickGuiTheme.toggleDisabled(), ClickGuiTheme.accent(), t);
        Rects.rounded(x, y, w, h, (int) (h / 2f), track.getRGB(), false);
        float knob = h - 3f;
        float kx = x + 1.5f + (w - knob - 3f) * t;
        int knobAlpha = 217 + (int) (38 * t);
        Rects.rounded(kx, y + 1.5f, knob, knob, (int) (knob / 2f),
                new Color(255, 255, 255, knobAlpha).getRGB(), false);
    }

    /**
     * Slider track + fill + thumb. {@code t} is the filled fraction. Number readout is the
     * caller's job (layouts differ).
     */
    public static void slider(float x, float y, float width, float t, boolean showThumb) {
        float clamped = Math.max(0f, Math.min(1f, t));
        float trackY = y + SLIDER_H / 2f - 1.25f;
        Rects.rounded(x, trackY, width, 2.5f, 1, ClickGuiTheme.layerActive().getRGB(), false);
        if (clamped > 0f) {
            Rects.rounded(x, trackY, Math.max(2.5f, width * clamped), 2.5f, 1,
                    ClickGuiTheme.accent().getRGB(), false);
        }
        if (showThumb) {
            float thumb = 7f;
            Rects.rounded(x + width * clamped - thumb / 2f, y + SLIDER_H / 2f - thumb / 2f,
                    thumb, thumb, (int) (thumb / 2f), Color.WHITE.getRGB(), false);
        }
    }

    /** Segmented control container (.seg). Returns nothing; draw options with {@link #segOption}. */
    public static void seg(float x, float y, float width, float height) {
        Rects.rounded(x - 0.5f, y - 0.5f, width + 1f, height + 1f, CTL_RADIUS + 1,
                ClickGuiTheme.stroke().getRGB(), false);
        Rects.rounded(x, y, width, height, CTL_RADIUS, ClickGuiTheme.mask(64).getRGB(), false);
    }

    public static void segOption(float x, float y, float width, float height, String label,
                                 boolean selected, boolean hover) {
        if (selected) {
            Rects.rounded(x, y, width, height, 3, ClickGuiTheme.accent().getRGB(), false);
        }
        int color = selected
                ? 0xFFFFFFFF
                : (hover ? ClickGuiTheme.textPrimary() : ClickGuiTheme.textSecondary()).getRGB();
        FPSMaster.fontManager.getFont(12).drawCenteredString(label, x + width / 2f, y + height / 2f - 3f, color);
    }

    /** Text input surface (.input); text/caret drawing stays with TextField. */
    public static void inputBox(float x, float y, float width, float height, boolean focused) {
        Rects.rounded(x - 0.5f, y - 0.5f, width + 1f, height + 1f, CTL_RADIUS + 1,
                (focused ? ClickGuiTheme.accent() : ClickGuiTheme.stroke()).getRGB(), false);
        Rects.rounded(x, y, width, height, CTL_RADIUS, ClickGuiTheme.mask(64).getRGB(), false);
    }

    /** Pill search field surface (.search). */
    public static void searchBox(float x, float y, float width, float height, boolean focused) {
        int r = (int) (height / 2f);
        Rects.rounded(x - 0.5f, y - 0.5f, width + 1f, height + 1f, r + 1,
                (focused ? ClickGuiTheme.accent() : ClickGuiTheme.stroke()).getRGB(), false);
        Rects.rounded(x, y, width, height, r, ClickGuiTheme.mask(64).getRGB(), false);
    }

    // ------------------------------------------------------------------
    // Keybind chip
    // ------------------------------------------------------------------

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
        Color border = active ? ClickGuiTheme.accent() : ClickGuiTheme.strokeStrong();
        Rects.rounded(x - 0.5f, y - 0.5f, width + 1f, height + 1f, 4,
                (hover && !active ? ClickGuiTheme.strokeStrong() : border).getRGB(), false);
        Rects.rounded(x, y, width, height, 3, ClickGuiTheme.mask(77).getRGB(), false);
        int color = active ? ClickGuiTheme.accentText().getRGB() : ClickGuiTheme.textSecondary().getRGB();
        FPSMaster.fontManager.getFont(11).drawCenteredString(text, x + width / 2f, y + height / 2f - 2.5f, color);
    }

    // ------------------------------------------------------------------
    // Navigation / misc
    // ------------------------------------------------------------------

    /** Pill nav item background (.nav .item). Content is drawn by the caller. */
    public static void navItem(float x, float y, float width, float height, boolean selected, boolean hover) {
        if (selected) {
            Rects.rounded(x, y, width, height, (int) (height / 2f), ClickGuiTheme.accent().getRGB(), false);
        } else if (hover) {
            Rects.rounded(x, y, width, height, (int) (height / 2f), ClickGuiTheme.layerHover().getRGB(), false);
        }
    }

    /** Small accent-soft badge (.badge). Returns its width. */
    public static float badge(float x, float y, String text) {
        UFontRenderer font = FPSMaster.fontManager.getFont(11);
        float w = font.getStringWidth(text) + 8f;
        float h = 10f;
        Rects.rounded(x, y, w, h, (int) (h / 2f), ClickGuiTheme.accentSoft().getRGB(), false);
        font.drawCenteredString(text, x + w / 2f, y + h / 2f - 2.5f, ClickGuiTheme.accentText().getRGB());
        return w;
    }

    /**
     * 4-bar ping indicator (.ping). Bars are 1.5 wide, 0.75 apart, heights 2/3/4/5.
     * {@code level}: 0 = offline (all dim), 1..4 = lit bar count. Total width 8.25.
     */
    public static void pingBars(float x, float baselineY, int level, Color litColor) {
        for (int i = 0; i < 4; i++) {
            float h = 2f + i;
            Color c = i < level ? litColor : ClickGuiTheme.layerActive();
            Rects.rounded(x + i * 2.25f, baselineY - h, 1.5f, h, 1, c.getRGB(), false);
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

    /** Fake-bold for headings: the single bundled face has no bold cut, so double-strike. */
    public static void boldString(UFontRenderer font, String text, float x, float y, int color) {
        font.drawString(text, x, y, color);
        font.drawString(text, x + 0.4f, y, color);
    }

    public static void boldCentered(UFontRenderer font, String text, float x, float y, int color) {
        float w = font.getStringWidth(text);
        boldString(font, text, x - w / 2f, y, color);
    }

    private static Color blend(Color from, Color to, float t) {
        float clamped = Math.max(0f, Math.min(1f, t));
        return new Color(
                (int) (from.getRed() + (to.getRed() - from.getRed()) * clamped),
                (int) (from.getGreen() + (to.getGreen() - from.getGreen()) * clamped),
                (int) (from.getBlue() + (to.getBlue() - from.getBlue()) * clamped),
                (int) (from.getAlpha() + (to.getAlpha() - from.getAlpha()) * clamped));
    }
}

package top.fpsmaster.ui.screens.replay;

import top.fpsmaster.FPSMaster;
import top.fpsmaster.font.impl.UFontRenderer;
import top.fpsmaster.ui.click.ClickGuiTheme;
import top.fpsmaster.ui.click.UiChrome;
import top.fpsmaster.utils.render.draw.Hover;
import top.fpsmaster.utils.render.draw.Icons;
import top.fpsmaster.utils.render.draw.Rects;
import top.fpsmaster.utils.render.gui.ScaledGuiScreen;

import java.awt.Color;

/**
 * Immediate-mode widgets for the director timeline.
 *
 * <p>Edge UI is not a retained widget tree. {@link UiChrome} paints; the caller hit-tests the same
 * frame via {@link ScaledGuiScreen#consumePressInBounds}. This class is that same contract, sized
 * for a dense NLE rather than Click GUI cards. No layout objects survive the frame.
 *
 * <p>Popups must claim clicks <em>before</em> the timeline, then paint again on top: first-come
 * hit-testing otherwise lets the clip under a menu eat the press.
 */
public final class DirectorUi {

    public static final float TOOL = 16f;
    public static final float TOOL_H = 14f;
    public static final float TOOLBAR_H = 22f;
    public static final float INSPECT_H = 16f;
    public static final float HEADER_W = 64f;
    public static final float RULER_H = 12f;
    public static final float CLIP_H = 20f;
    public static final float CLIP_CURVE_H = 36f;
    public static final float PROP_ROW = 13f;
    public static final float GAP = 3f;
    public static final float TRIM = 6f;
    public static final float SCROLL_H = 5f;
    public static final int KEY_FILL = 0xFFF2D27A;
    static final float ICON = 7f;

    private static String tipText;
    private static float tipX;
    private static float tipY;

    private DirectorUi() {
    }

    public static void beginFrame() {
        tipText = null;
    }

    public static void endFrame(float guiWidth, float guiHeight) {
        if (tipText == null || tipText.isEmpty()) {
            return;
        }
        UFontRenderer font = FPSMaster.fontManager.getFont(11);
        float tw = font.getStringWidth(tipText) + 10f;
        float th = 13f;
        float x = tipX - tw / 2f;
        float y = tipY - th - 5f;
        if (x < 4f) {
            x = 4f;
        }
        if (x + tw > guiWidth - 4f) {
            x = guiWidth - 4f - tw;
        }
        if (y < 4f) {
            y = tipY + TOOL_H + 5f;
        }
        Rects.rounded(x, y, tw, th, 3, new Color(18, 18, 20, 242).getRGB(), false);
        font.drawCenteredString(tipText, x + tw / 2f, y + 3f, ClickGuiTheme.textPrimary().getRGB());
    }

    public static void tip(String text, float x, float y) {
        if (text == null || text.isEmpty()) {
            return;
        }
        tipText = text;
        tipX = x;
        tipY = y;
    }

    public static boolean click(ScaledGuiScreen screen, float x, float y, float w, float h, int button) {
        return screen != null && screen.consumePressInBounds(x, y, w, h, button) != null;
    }

    public static boolean outside(ScaledGuiScreen screen, float x, float y, float w, float h) {
        return screen != null && screen.consumePressOutside(x, y, w, h) != null;
    }

    public static boolean tool(ScaledGuiScreen screen, float x, float y, String icon, String tip,
                               boolean on, boolean enabled, int mouseX, int mouseY) {
        boolean hover = enabled && Hover.is(x, y, TOOL, TOOL_H, mouseX, mouseY);
        if (on) {
            Rects.rounded(x, y, TOOL, TOOL_H, 3, ClickGuiTheme.accent().getRGB(), false);
        } else if (hover) {
            Rects.rounded(x, y, TOOL, TOOL_H, 3, ClickGuiTheme.layerHover().getRGB(), false);
        } else {
            Rects.rounded(x, y, TOOL, TOOL_H, 3, ClickGuiTheme.layer().getRGB(), false);
        }
        int color;
        if (!enabled) {
            color = ClickGuiTheme.textDisabled().getRGB();
        } else if (on) {
            color = 0xFFFFFFFF;
        } else {
            color = (hover ? ClickGuiTheme.textPrimary() : ClickGuiTheme.textSecondary()).getRGB();
        }
        Icons.draw(icon, x + (TOOL - ICON) / 2f, y + (TOOL_H - ICON) / 2f, ICON, color);
        if (hover) {
            tip(tip, x + TOOL / 2f, y);
        }
        return enabled && click(screen, x, y, TOOL, TOOL_H, 0);
    }

    public static float textWidth(String label) {
        return FPSMaster.fontManager.getFont(11).getStringWidth(label) + 10f;
    }

    public static boolean textTool(ScaledGuiScreen screen, float x, float y, String label, String tip,
                                   boolean on, boolean enabled, int mouseX, int mouseY) {
        float w = textWidth(label);
        boolean hover = enabled && Hover.is(x, y, w, TOOL_H, mouseX, mouseY);
        if (on) {
            Rects.rounded(x, y, w, TOOL_H, 3, ClickGuiTheme.accent().getRGB(), false);
        } else if (hover) {
            UiChrome.button(x, y, w, TOOL_H, true);
        } else {
            UiChrome.button(x, y, w, TOOL_H, false);
        }
        int color = !enabled
                ? ClickGuiTheme.textDisabled().getRGB()
                : (on ? 0xFFFFFFFF : ClickGuiTheme.textPrimary().getRGB());
        FPSMaster.fontManager.getFont(11).drawCenteredString(label, x + w / 2f, y + 3.5f, color);
        if (hover) {
            tip(tip == null ? label : tip, x + w / 2f, y);
        }
        return enabled && click(screen, x, y, w, TOOL_H, 0);
    }

    public static boolean primary(ScaledGuiScreen screen, float x, float y, float w, String label,
                                  boolean enabled, int mouseX, int mouseY) {
        boolean hover = enabled && Hover.is(x, y, w, TOOL_H, mouseX, mouseY);
        UiChrome.fillButton(x, y, w, TOOL_H, hover, false);
        FPSMaster.fontManager.getFont(11).drawCenteredString(label, x + w / 2f, y + 3.5f, 0xFFFFFFFF);
        return enabled && click(screen, x, y, w, TOOL_H, 0);
    }

    public static float enumSeg(ScaledGuiScreen screen, float x, float y, String label,
                                String[] options, int current, int mouseX, int mouseY, EnumPick pick) {
        FPSMaster.fontManager.getFont(11).drawString(label, x, y + 3.5f, ClickGuiTheme.textDisabled().getRGB());
        float cursor = x + FPSMaster.fontManager.getFont(11).getStringWidth(label) + 4f;
        float segH = 13f;
        float[] widths = new float[options.length];
        float total = 3f;
        for (int idx = 0; idx < options.length; idx++) {
            widths[idx] = FPSMaster.fontManager.getFont(11).getStringWidth(options[idx]) + 8f;
            total += widths[idx] + (idx > 0 ? 1f : 0f);
        }
        UiChrome.seg(cursor, y, total, segH);
        float ox = cursor + 1.5f;
        for (int idx = 0; idx < options.length; idx++) {
            boolean on = idx == current;
            boolean hover = Hover.is(ox, y + 1.5f, widths[idx], segH - 3f, mouseX, mouseY);
            UiChrome.segOption(ox, y + 1.5f, widths[idx], segH - 3f, options[idx], on, hover);
            if (click(screen, ox, y, widths[idx], segH, 0)) {
                pick.pick(idx);
            }
            ox += widths[idx] + 1f;
        }
        return cursor + total;
    }

    public static boolean menuRow(ScaledGuiScreen screen, float x, float y, float w, String label,
                                  boolean on, int mouseX, int mouseY) {
        boolean hover = Hover.is(x + 4f, y, w - 8f, 13f, mouseX, mouseY);
        if (on || hover) {
            Rects.rounded(x + 4f, y, w - 8f, 12f, 3,
                    (on ? ClickGuiTheme.accent() : ClickGuiTheme.layerHover()).getRGB(), false);
        }
        FPSMaster.fontManager.getFont(12).drawString(label, x + 8f, y + 2.5f,
                on ? 0xFFFFFFFF : ClickGuiTheme.textPrimary().getRGB());
        return click(screen, x + 4f, y, w - 8f, 13f, 0);
    }

    public static void fillDiamond(float cx, float cy, float r, int color) {
        float s = Math.max(3f, r * 2f);
        Rects.rounded(cx - s / 2f, cy - s / 2f, s, s, 1, color, false);
    }

    public static void strokeDiamond(float cx, float cy, float r, int color) {
        float s = Math.max(3f, r * 2f);
        Rects.rounded(cx - s / 2f - 0.6f, cy - s / 2f - 0.6f, s + 1.2f, s + 1.2f, 1, color, false);
        Rects.rounded(cx - s / 2f, cy - s / 2f, s, s, 1, new Color(16, 16, 18, 240).getRGB(), false);
    }

    public static void line(float x1, float y1, float x2, float y2, int color) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        if (Math.abs(dx) < 0.2f && Math.abs(dy) < 0.2f) {
            Rects.fill(x1, y1, 1f, 1f, color);
            return;
        }
        Rects.fill(Math.min(x1, x2), Math.min(y1, y2), Math.max(1f, Math.abs(dx)), Math.max(1f, Math.abs(dy)), color);
    }

    public static void playhead(float x, float y, float h, boolean scrubbing) {
        int color = scrubbing ? ClickGuiTheme.accent().getRGB() : 0xFFFFFFFF;
        Rects.fill(x, y, 1.25f, h, color);
        Rects.rounded(x - 3.5f, y, 8f, 6f, 1, color, false);
    }

    public interface EnumPick {
        void pick(int index);
    }

    /**
     * Left-to-right / right-to-left cursor for a toolbar row. Same idea as ImGui {@code SameLine}:
     * no widget objects, just an advancing x.
     */
    public static final class Bar {
        public final ScaledGuiScreen screen;
        public final int mouseX;
        public final int mouseY;
        public final float y;
        public final float btnY;
        public float left;
        public float right;

        public Bar(ScaledGuiScreen screen, float x, float y, float w, float h, int mouseX, int mouseY) {
            this.screen = screen;
            this.mouseX = mouseX;
            this.mouseY = mouseY;
            this.y = y;
            this.btnY = y + (h - TOOL_H) / 2f;
            this.left = x;
            this.right = x + w;
        }

        public boolean iconLeft(String icon, String tip, boolean on, boolean enabled) {
            boolean clicked = tool(screen, left, btnY, icon, tip, on, enabled, mouseX, mouseY);
            left += TOOL + GAP;
            return clicked;
        }

        public boolean iconRight(String icon, String tip, boolean on, boolean enabled) {
            right -= TOOL;
            boolean clicked = tool(screen, right, btnY, icon, tip, on, enabled, mouseX, mouseY);
            right -= GAP;
            return clicked;
        }

        public boolean textLeft(String label, String tip, boolean on, boolean enabled) {
            float w = textWidth(label);
            boolean clicked = textTool(screen, left, btnY, label, tip, on, enabled, mouseX, mouseY);
            left += w + GAP;
            return clicked;
        }

        public boolean textRight(String label, String tip, boolean on, boolean enabled) {
            float w = textWidth(label);
            right -= w;
            boolean clicked = textTool(screen, right, btnY, label, tip, on, enabled, mouseX, mouseY);
            right -= GAP;
            return clicked;
        }

        public void gapLeft() {
            left += 5f;
        }

        public void ruleLeft() {
            left += 4f;
            Rects.fill(left, btnY + 2f, 0.5f, TOOL_H - 4f, ClickGuiTheme.strokeStrong().getRGB());
            left += 5f;
        }

        public void labelLeft(String text, int color) {
            UFontRenderer font = FPSMaster.fontManager.getFont(11);
            font.drawString(text, left, btnY + 3.5f, color);
            left += font.getStringWidth(text) + 8f;
        }
    }
}

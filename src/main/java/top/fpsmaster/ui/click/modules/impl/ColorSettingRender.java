package top.fpsmaster.ui.click.modules.impl;

import net.minecraft.util.ResourceLocation;
import top.fpsmaster.FPSMaster;
import top.fpsmaster.features.manager.Module;
import top.fpsmaster.features.settings.impl.ColorSetting;
import top.fpsmaster.features.settings.impl.utils.CustomColor;
import top.fpsmaster.ui.click.ClickGuiTheme;
import top.fpsmaster.ui.click.UiChrome;
import top.fpsmaster.ui.click.modules.SettingRender;
import top.fpsmaster.ui.common.binding.ColorSettingBinding;
import top.fpsmaster.utils.math.anim.AnimMath;
import top.fpsmaster.utils.render.draw.Gradients;
import top.fpsmaster.utils.render.draw.Hover;
import top.fpsmaster.utils.render.draw.Images;
import top.fpsmaster.utils.render.draw.Rects;
import top.fpsmaster.utils.render.gui.ScaledGuiScreen;
import top.fpsmaster.utils.render.gui.UiScale;
import top.fpsmaster.utils.render.shader.GradientUtils;
import top.fpsmaster.utils.system.OSUtil;

import java.awt.Color;
import java.util.Locale;

import static java.lang.Math.max;
import static java.lang.Math.min;

/**
 * Color row after the prototype: collapsed shows "#RRGGBB" + a color dot on the right; clicking
 * them unfolds an editor card — a segmented mode selector, an HSBA palette with hue / alpha rails
 * for the static-ish modes, and sliders styled like every other slider in the GUI for the rest.
 */
public class ColorSettingRender extends SettingRender<ColorSetting> {
    private static final float ROW_H = 19f;
    private static final float DOT = 9f;
    private static final float PICKER_W = 64f;
    private static final float PICKER_H = 44f;
    private static final float RAIL_W = 6f;
    private static final float RAIL_GAP = 5f;
    private static final float PAD = 6f;
    private static final float SEG_PAD = 1.5f;
    private static final float SEG_H = 16f;
    private static final float TRACK_H = 2.5f;
    private static final float THUMB = 5.5f;
    private static final float SLIDER_ROW_H = 13f;
    private static final ResourceLocation CURSOR = new ResourceLocation("client/gui/settings/values/color.png");
    private static final ResourceLocation ALPHA_CHECKER = new ResourceLocation("client/gui/settings/values/alpha.png");

    private float expandedHeight = 0f;
    /** Screenshot-pipeline hook: -Dedge.uishot.expandcolors renders every color row unfolded. */
    private boolean expand = Boolean.getBoolean("edge.uishot.expandcolors");
    private final ColorSettingBinding binding;
    private final String paletteCaptureId;
    private final String hueCaptureId;
    private final String alphaCaptureId;
    private final String saturationCaptureId;
    private final String brightnessCaptureId;
    private final String speedCaptureId;

    public ColorSettingRender(Module mod, ColorSetting setting) {
        super(setting);
        this.mod = mod;
        this.binding = new ColorSettingBinding(setting);
        String capturePrefix = mod.name + ":" + setting.name + ":color:";
        this.paletteCaptureId = capturePrefix + "palette";
        this.hueCaptureId = capturePrefix + "hue";
        this.alphaCaptureId = capturePrefix + "alpha";
        this.saturationCaptureId = capturePrefix + "saturation";
        this.brightnessCaptureId = capturePrefix + "brightness";
        this.speedCaptureId = capturePrefix + "speed";
    }

    @Override
    public boolean isWide() {
        return true;
    }

    @Override
    public void render(ScaledGuiScreen screen, float x, float y, float width, float height, float mouseX, float mouseY, boolean custom) {
        String labelKey = (mod.name + "." + setting.name).toLowerCase(Locale.getDefault());
        FPSMaster.fontManager.getFont(13).drawString(
                FPSMaster.i18n.get(labelKey), x + 5, y + 6, ClickGuiTheme.textPrimary().getRGB());

        CustomColor customColor = binding.get();
        Color previewColor = setting.getColor();

        float dotX = x + width - 5 - DOT;
        float dotY = y + (ROW_H - DOT) / 2f;
        String hex = "#" + String.format("%06X", previewColor.getRGB() & 0xFFFFFF);
        float hexW = FPSMaster.fontManager.getFont(11).getStringWidth(hex);
        FPSMaster.fontManager.getFont(11).drawString(hex, dotX - 4 - hexW, y + 6.5f,
                ClickGuiTheme.textDisabled().getRGB());
        Rects.rounded(dotX - 1f, dotY - 1f, DOT + 2f, DOT + 2f, (int) ((DOT + 2f) / 2f),
                new Color(255, 255, 255, 64).getRGB(), false);
        Rects.rounded(dotX, dotY, DOT, DOT, (int) (DOT / 2f), previewColor.getRGB(), false);

        ColorSetting.ColorType[] types = setting.getAvailableTypes();
        boolean showModes = types.length > 1;
        ColorSetting.ColorType type = setting.getColorType();
        boolean showPalette = type == ColorSetting.ColorType.STATIC
                || type == ColorSetting.ColorType.WAVE
                || type == ColorSetting.ColorType.WAVE_BRIGHTNESS;
        boolean showSpeed = type != ColorSetting.ColorType.STATIC;

        // --- card metrics: the card hugs its content instead of floating in the row ---
        float labelColW = sliderLabelWidth();
        float valueColW = sliderValueWidth();
        float trackW = 78f;
        float sliderRowW = labelColW + 5f + trackW + 5f + valueColW;
        float paletteBlockW = PICKER_W + RAIL_GAP + RAIL_W + RAIL_GAP + RAIL_W;
        float editorW = showPalette
                ? max(paletteBlockW, showSpeed ? sliderRowW : 0f)
                : sliderRowW;
        float segOptW = 0f;
        if (showModes) {
            for (ColorSetting.ColorType t : types) {
                segOptW = max(segOptW, FPSMaster.fontManager.getFont(11).getStringWidth(FPSMaster.i18n.get(t.i18nKey)) + 12f);
            }
        }
        float segW = showModes ? types.length * segOptW + SEG_PAD * 2f : 0f;
        float contentW = max(editorW, segW);
        float cardW = min(width - 10f, contentW + PAD * 2f);
        contentW = cardW - PAD * 2f;
        trackW = max(40f, min(trackW, contentW - labelColW - valueColW - 10f));

        float editorH = showPalette
                ? PICKER_H + (showSpeed ? SLIDER_ROW_H + 3f : 0f)
                : SLIDER_ROW_H * 3f;
        float cardH = PAD + (showModes ? SEG_H + 5f : 0f) + editorH + PAD;

        float targetHeight = expand ? cardH + 4f : 0f;
        expandedHeight = (float) AnimMath.base(expandedHeight, targetHeight, 0.2);

        if (expandedHeight > 1f) {
            float cardX = x + 5f;
            float cardY = y + ROW_H + 2f;
            Rects.rounded(cardX - 0.5f, cardY - 0.5f, cardW + 1f, cardH + 1f, 7,
                    ClickGuiTheme.stroke().getRGB(), false);
            Rects.rounded(cardX, cardY, cardW, cardH, 6, ClickGuiTheme.layer().getRGB(), false);

            float cx = cardX + PAD;
            float cy = cardY + PAD;
            if (showModes) {
                drawModeSegments(screen, types, type, cx, cy, contentW, segOptW, mouseX, mouseY);
                cy += SEG_H + 5f;
            }
            if (showPalette) {
                renderPalette(screen, cx, cy, mouseX, mouseY, customColor);
                if (showSpeed) {
                    renderSpeedSlider(screen, cx, cy + PICKER_H + 3f, labelColW, trackW, valueColW, mouseX);
                }
            } else {
                renderChromaSliders(screen, cx, cy, labelColW, trackW, valueColW, mouseX, customColor);
            }
        }

        if (screen.consumePressInBounds(dotX - 8 - hexW, y, hexW + 8 + DOT + 5, ROW_H, 0) != null) {
            expand = !expand;
        }

        this.height = expandedHeight + ROW_H;
    }

    private void drawModeSegments(ScaledGuiScreen screen, ColorSetting.ColorType[] types,
                                  ColorSetting.ColorType current, float x, float y, float contentW,
                                  float optW, float mouseX, float mouseY) {
        float segW = min(contentW, types.length * optW + SEG_PAD * 2f);
        optW = (segW - SEG_PAD * 2f) / types.length;
        UiChrome.seg(x, y, segW, SEG_H);
        for (int i = 0; i < types.length; i++) {
            float ox = x + SEG_PAD + i * optW;
            boolean selected = types[i] == current;
            boolean hover = Hover.is(ox, y + SEG_PAD, optW, SEG_H - SEG_PAD * 2f, (int) mouseX, (int) mouseY);
            UiChrome.segOption(ox, y + SEG_PAD, optW, SEG_H - SEG_PAD * 2f,
                    FPSMaster.i18n.get(types[i].i18nKey), selected, hover);
            if (screen.consumePressInBounds(ox, y, optW, SEG_H) != null) {
                setting.setColorType(types[i]);
            }
        }
    }

    private void renderPalette(ScaledGuiScreen screen, float pickerX, float pickerY, float mouseX, float mouseY, CustomColor customColor) {
        Rects.rounded(pickerX - 0.5f, pickerY - 0.5f, PICKER_W + 1f, PICKER_H + 1f, 4,
                ClickGuiTheme.stroke().getRGB(), false);
        if (OSUtil.supportShader()) {
            GradientUtils.applyGradient(
                    UiScale.toPixel(pickerX),
                    UiScale.toPixel(pickerY),
                    UiScale.toPixel(PICKER_W),
                    UiScale.toPixel(PICKER_H),
                    1f,
                    Color.getHSBColor(customColor.hue, 0.0f, 0f),
                    Color.getHSBColor(customColor.hue, 0f, 1f),
                    Color.getHSBColor(customColor.hue, 1f, 0f),
                    Color.getHSBColor(customColor.hue, 1f, 1f),
                    1f,
                    () -> Rects.roundedImage(Math.round(pickerX), Math.round(pickerY), Math.round(PICKER_W), Math.round(PICKER_H), 3, Color.WHITE)
            );
        } else {
            for (int i = 0; i < PICKER_H; i++) {
                for (int j = 0; j < PICKER_W; j++) {
                    float brightness = 1 - i / PICKER_H;
                    float saturation = j / PICKER_W;
                    Rects.fill(pickerX + j, pickerY + i, 1, 1, Color.getHSBColor(customColor.hue, saturation, brightness).getRGB());
                }
            }
        }

        float saturation = customColor.saturation;
        float brightness = customColor.brightness;
        screen.beginPointerCapture(paletteCaptureId, 0, pickerX, pickerY, PICKER_W, PICKER_H);
        if (screen.isPointerCapturedBy(paletteCaptureId, 0)) {
            saturation = max(min((mouseX - pickerX) / PICKER_W, 1f), 0f);
            brightness = max(min(1f - (mouseY - pickerY) / PICKER_H, 1f), 0f);
        }
        float cursorX = pickerX + saturation * PICKER_W;
        float cursorY = pickerY + (1 - brightness) * PICKER_H;
        Images.draw(CURSOR, cursorX - 2.5f, cursorY - 2.5f, 5f, 5f, -1);

        float hueX = pickerX + PICKER_W + RAIL_GAP;
        float hue = customColor.hue;
        Rects.rounded(hueX - 0.5f, pickerY - 0.5f, RAIL_W + 1f, PICKER_H + 1f, 3,
                ClickGuiTheme.stroke().getRGB(), false);
        Gradients.hue(hueX, pickerY, (int) RAIL_W, PICKER_H);
        Images.draw(CURSOR, hueX + RAIL_W / 2f - 2.5f, pickerY + PICKER_H * customColor.hue - 2.5f, 5f, 5f, -1);
        screen.beginPointerCapture(hueCaptureId, 0, hueX, pickerY, RAIL_W, PICKER_H);
        if (screen.isPointerCapturedBy(hueCaptureId, 0)) {
            hue = max(min((mouseY - pickerY) / PICKER_H, 1f), 0f);
        }

        float alphaX = hueX + RAIL_W + RAIL_GAP;
        float alpha = customColor.alpha;
        Rects.rounded(alphaX - 0.5f, pickerY - 0.5f, RAIL_W + 1f, PICKER_H + 1f, 3,
                ClickGuiTheme.stroke().getRGB(), false);
        Images.draw(ALPHA_CHECKER, alphaX, pickerY, RAIL_W, PICKER_H, -1);
        if (OSUtil.supportShader()) {
            // The rail fades the setting's own color out, not white: what the knob picks is what
            // the rail shows at that height.
            Color base = new Color(Color.HSBtoRGB(customColor.hue, customColor.saturation, customColor.brightness));
            GradientUtils.drawGradientVertical(alphaX, pickerY, RAIL_W, PICKER_H,
                    base, new Color(base.getRed(), base.getGreen(), base.getBlue(), 0));
        }
        Images.draw(CURSOR, alphaX + RAIL_W / 2f - 2.5f, pickerY + PICKER_H * (1 - alpha) - 2.5f, 5f, 5f, -1);
        screen.beginPointerCapture(alphaCaptureId, 0, alphaX, pickerY, RAIL_W, PICKER_H);
        if (screen.isPointerCapturedBy(alphaCaptureId, 0)) {
            alpha = max(min(1f - (mouseY - pickerY) / PICKER_H, 1f), 0f);
        }

        if (hue != customColor.hue || saturation != customColor.saturation || brightness != customColor.brightness || alpha != customColor.alpha) {
            binding.setHsba(hue, saturation, brightness, alpha);
        }
    }

    private void renderChromaSliders(ScaledGuiScreen screen, float x, float y, float labelW, float trackW, float valueW, float mouseX, CustomColor customColor) {
        float saturation = drawSlider(screen, saturationCaptureId,
                FPSMaster.i18n.get("colorsetting.saturation"), x, y, labelW, trackW, valueW,
                customColor.saturation, Math.round(customColor.saturation * 100f) + "%", mouseX);
        float brightness = drawSlider(screen, brightnessCaptureId,
                FPSMaster.i18n.get("colorsetting.brightness"), x, y + SLIDER_ROW_H, labelW, trackW, valueW,
                customColor.brightness, Math.round(customColor.brightness * 100f) + "%", mouseX);
        if (saturation != customColor.saturation || brightness != customColor.brightness) {
            binding.setHsba(customColor.hue, saturation, brightness, customColor.alpha);
        }
        renderSpeedSlider(screen, x, y + SLIDER_ROW_H * 2f, labelW, trackW, valueW, mouseX);
    }

    private void renderSpeedSlider(ScaledGuiScreen screen, float x, float y, float labelW, float trackW, float valueW, float mouseX) {
        float speed = setting.getSpeed();
        float value01 = (speed - 0.1f) / 9.9f;
        float dragged = drawSlider(screen, speedCaptureId,
                FPSMaster.i18n.get("colorsetting.speed"), x, y, labelW, trackW, valueW,
                value01, String.format(Locale.getDefault(), "%.1f", speed), mouseX);
        if (dragged != value01) {
            setting.setSpeed(0.1f + dragged * 9.9f);
        }
    }

    /**
     * One slider row in the shared GUI vocabulary (track + fill + round thumb + fixed-width
     * right-aligned value). Returns the possibly-dragged 0..1 value.
     */
    private float drawSlider(ScaledGuiScreen screen, String captureId, String label,
                             float x, float y, float labelW, float trackW, float valueW,
                             float value01, String valueText, float mouseX) {
        FPSMaster.fontManager.getFont(11).drawString(label, x, y + 1f, ClickGuiTheme.textSecondary().getRGB());
        float trackX = x + labelW + 5f;
        float trackY = y + 3f;
        Rects.rounded(trackX, trackY, trackW, TRACK_H, 1, ClickGuiTheme.layerActive().getRGB(), false);
        float fillW = trackW * max(0f, min(1f, value01));
        if (fillW > 0.5f) {
            Rects.rounded(trackX, trackY, fillW, TRACK_H, 1, ClickGuiTheme.sliderFill().getRGB(), false);
        }
        float thumbX = trackX + max(0f, min(trackW - THUMB, fillW - THUMB / 2f));
        Rects.rounded(thumbX, trackY + (TRACK_H - THUMB) / 2f, THUMB, THUMB, (int) (THUMB / 2f),
                0xFFFFFFFF, false);
        float textW = FPSMaster.fontManager.getFont(11).getStringWidth(valueText);
        FPSMaster.fontManager.getFont(11).drawString(valueText,
                trackX + trackW + 5f + (valueW - textW), y + 1f, ClickGuiTheme.textSecondary().getRGB());

        screen.beginPointerCapture(captureId, 0, trackX, trackY - 4f, trackW, TRACK_H + 8f);
        if (screen.isPointerCapturedBy(captureId, 0)) {
            return max(0f, min(1f, (mouseX - trackX) / trackW));
        }
        return value01;
    }

    /** Widest slider label of the editor, so all three rows share one label column. */
    private float sliderLabelWidth() {
        float w = 0f;
        w = max(w, FPSMaster.fontManager.getFont(11).getStringWidth(FPSMaster.i18n.get("colorsetting.saturation")));
        w = max(w, FPSMaster.fontManager.getFont(11).getStringWidth(FPSMaster.i18n.get("colorsetting.brightness")));
        w = max(w, FPSMaster.fontManager.getFont(11).getStringWidth(FPSMaster.i18n.get("colorsetting.speed")));
        return min(w, 34f);
    }

    /** Fixed value column ("100%" / "10.0" are the widest), so tracks never shift while dragging. */
    private float sliderValueWidth() {
        return max(FPSMaster.fontManager.getFont(11).getStringWidth("100%"),
                FPSMaster.fontManager.getFont(11).getStringWidth("88.8"));
    }
}

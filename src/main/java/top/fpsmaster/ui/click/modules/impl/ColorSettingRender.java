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
 * them unfolds a compact HSBA editor (mode chip, palette, hue / alpha rails, optional speed).
 */
public class ColorSettingRender extends SettingRender<ColorSetting> {
    private static final float ROW_H = 19f;
    private static final float DOT = 9f;
    private static final float PICKER_W = 56f;
    private static final float PICKER_H = 40f;
    private static final float RAIL_W = 5f;
    private static final ResourceLocation CURSOR = new ResourceLocation("client/gui/settings/values/color.png");

    private float expandedHeight = 0f;
    private boolean expand = false;
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

        boolean showPalette = setting.getColorType() == ColorSetting.ColorType.STATIC
                || setting.getColorType() == ColorSetting.ColorType.WAVE
                || setting.getColorType() == ColorSetting.ColorType.WAVE_BRIGHTNESS;
        boolean showSpeed = setting.getColorType() != ColorSetting.ColorType.STATIC;
        float targetHeight = expand ? (showPalette ? (showSpeed ? PICKER_H + 14f : PICKER_H + 4f) : 40f) : 0f;
        expandedHeight = (float) AnimMath.base(expandedHeight, targetHeight, 0.2);

        if (expandedHeight > 1f) {
            float editorY = y + ROW_H + 2f;
            float modeW = 40f;
            boolean modeHover = Hover.is(x + 5, editorY, modeW, 11f, (int) mouseX, (int) mouseY);
            Rects.rounded(x + 5, editorY, modeW, 11f, 4,
                    (modeHover ? ClickGuiTheme.layerHover() : ClickGuiTheme.layer()).getRGB(), false);
            FPSMaster.fontManager.getFont(11).drawCenteredString(
                    FPSMaster.i18n.get(setting.getColorType().i18nKey),
                    x + 5 + modeW / 2f, editorY + 3f, ClickGuiTheme.modeText().getRGB());
            if (showPalette) {
                renderStaticOrWaveEditor(screen, x + 5 + modeW + 6f, editorY, mouseX, mouseY, customColor, showSpeed);
            } else {
                renderDynamicEditor(screen, x + 5 + modeW + 6f, editorY, mouseX, mouseY, customColor);
            }
            if (screen.consumePressInBounds(x + 5, editorY, modeW, 11f, 0) != null) {
                setting.cycleColorType();
            }
        }

        if (screen.consumePressInBounds(dotX - 8 - hexW, y, hexW + 8 + DOT + 5, ROW_H, 0) != null) {
            expand = !expand;
        }

        this.height = expandedHeight + ROW_H;
    }

    private void renderStaticOrWaveEditor(ScaledGuiScreen screen, float pickerX, float pickerY, float mouseX, float mouseY, CustomColor customColor, boolean showSpeed) {
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

        float cursorX = saturation * PICKER_W;
        float cursorY = (1 - brightness) * PICKER_H;
        Images.draw(CURSOR, pickerX + cursorX - 2f, pickerY + cursorY - 2f, 4f, 4f, -1);

        float hueX = pickerX + PICKER_W + 4f;
        float hue = customColor.hue;
        Gradients.hue(hueX, pickerY, (int) RAIL_W, PICKER_H);
        Images.draw(CURSOR, hueX + RAIL_W / 2f - 2f, pickerY + PICKER_H * customColor.hue - 2f, 4f, 4f, -1);
        screen.beginPointerCapture(hueCaptureId, 0, hueX, pickerY, RAIL_W, PICKER_H);
        if (screen.isPointerCapturedBy(hueCaptureId, 0)) {
            hue = max(min((mouseY - pickerY) / PICKER_H, 1f), 0f);
        }

        float alphaX = hueX + RAIL_W + 4f;
        float alpha = customColor.alpha;
        Images.draw(new ResourceLocation("client/gui/settings/values/alpha.png"), alphaX, pickerY, RAIL_W, PICKER_H, -1);
        if (OSUtil.supportShader()) {
            GradientUtils.drawGradientVertical(alphaX, pickerY, RAIL_W, PICKER_H, new Color(255, 255, 255), new Color(255, 255, 255, 0));
        }
        Images.draw(CURSOR, alphaX + RAIL_W / 2f - 2f, pickerY + PICKER_H * (1 - alpha) - 2f, 4f, 4f, -1);
        screen.beginPointerCapture(alphaCaptureId, 0, alphaX, pickerY, RAIL_W, PICKER_H);
        if (screen.isPointerCapturedBy(alphaCaptureId, 0)) {
            alpha = max(min(1f - (mouseY - pickerY) / PICKER_H, 1f), 0f);
        }

        if (hue != customColor.hue || saturation != customColor.saturation || brightness != customColor.brightness || alpha != customColor.alpha) {
            binding.setHsba(hue, saturation, brightness, alpha);
        }

        if (showSpeed) {
            renderSpeedSlider(screen, pickerX, pickerY + PICKER_H + 4f, PICKER_W, mouseX, mouseY);
        }
    }

    private void renderSpeedSlider(ScaledGuiScreen screen, float sliderX, float sliderY, float sliderW, float mouseX, float mouseY) {
        float speed = setting.getSpeed();
        Rects.rounded(sliderX, sliderY, sliderW, 3f, 1, ClickGuiTheme.layerActive().getRGB(), false);
        Rects.rounded(sliderX, sliderY, sliderW * (speed - 0.1f) / 9.9f, 3f, 1, ClickGuiTheme.accent().getRGB(), false);
        FPSMaster.fontManager.getFont(11).drawString("T", sliderX - 6f, sliderY - 1.5f, ClickGuiTheme.textSecondary().getRGB());
        FPSMaster.fontManager.getFont(11).drawString(String.format(Locale.getDefault(), "%.1f", speed), sliderX + sliderW + 3f, sliderY - 1.5f, ClickGuiTheme.textSecondary().getRGB());

        float newSpeed = speed;
        screen.beginPointerCapture(speedCaptureId, 0, sliderX, sliderY - 2, sliderW, 7f);
        if (screen.isPointerCapturedBy(speedCaptureId, 0)) {
            newSpeed = 0.1f + max(min((mouseX - sliderX) / sliderW, 1f), 0f) * 9.9f;
        }
        if (Math.abs(newSpeed - speed) > 0.01f) {
            setting.setSpeed(newSpeed);
        }
    }

    private void renderDynamicEditor(ScaledGuiScreen screen, float sliderX, float editorY, float mouseX, float mouseY, CustomColor customColor) {
        float satY = editorY + 2f;
        float brightY = editorY + 12f;
        float sliderW = 56f;

        FPSMaster.fontManager.getFont(11).drawString("S", sliderX - 6f, satY - 1.5f, ClickGuiTheme.textSecondary().getRGB());
        FPSMaster.fontManager.getFont(11).drawString("B", sliderX - 6f, brightY - 1.5f, ClickGuiTheme.textSecondary().getRGB());

        Rects.rounded(sliderX, satY, sliderW, 3f, 1, ClickGuiTheme.layerActive().getRGB(), false);
        Rects.rounded(sliderX, brightY, sliderW, 3f, 1, ClickGuiTheme.layerActive().getRGB(), false);
        Rects.rounded(sliderX, satY, sliderW * customColor.saturation, 3f, 1, new Color(114, 173, 255).getRGB(), false);
        Rects.rounded(sliderX, brightY, sliderW * customColor.brightness, 3f, 1, new Color(255, 223, 114).getRGB(), false);

        float saturation = customColor.saturation;
        float brightness = customColor.brightness;
        screen.beginPointerCapture(saturationCaptureId, 0, sliderX, satY - 2, sliderW, 7f);
        if (screen.isPointerCapturedBy(saturationCaptureId, 0)) {
            saturation = max(min((mouseX - sliderX) / sliderW, 1f), 0f);
        }
        screen.beginPointerCapture(brightnessCaptureId, 0, sliderX, brightY - 2, sliderW, 7f);
        if (screen.isPointerCapturedBy(brightnessCaptureId, 0)) {
            brightness = max(min((mouseX - sliderX) / sliderW, 1f), 0f);
        }

        if (saturation != customColor.saturation || brightness != customColor.brightness) {
            binding.setHsba(customColor.hue, saturation, brightness, customColor.alpha);
        }

        renderSpeedSlider(screen, sliderX, editorY + 24f, sliderW, mouseX, mouseY);
    }
}

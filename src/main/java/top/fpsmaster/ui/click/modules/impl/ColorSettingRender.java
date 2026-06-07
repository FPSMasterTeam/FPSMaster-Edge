package top.fpsmaster.ui.click.modules.impl;

import net.minecraft.util.ResourceLocation;
import top.fpsmaster.FPSMaster;
import top.fpsmaster.features.manager.Module;
import top.fpsmaster.features.settings.impl.ColorSetting;
import top.fpsmaster.features.settings.impl.utils.CustomColor;
import top.fpsmaster.ui.click.modules.SettingRender;
import top.fpsmaster.ui.common.binding.ColorSettingBinding;
import top.fpsmaster.utils.math.anim.AnimMath;
import top.fpsmaster.ui.click.ClickGuiTheme;
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

public class ColorSettingRender extends SettingRender<ColorSetting> {
    private static final float COLOR_BOX_W = 80f;
    private static final float COLOR_BOX_H = 14f;
    private static final float MODE_BOX_W = 48f;
    private static final float PICKER_W = 80f;

    private float expandedHeight = 0f;
    private boolean expand = false;
    private final ColorSettingBinding binding;
    private final String paletteCaptureId;
    private final String hueCaptureId;
    private final String alphaCaptureId;
    private final String saturationCaptureId;
    private final String brightnessCaptureId;

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
    }

    @Override
    public void render(ScaledGuiScreen screen, float x, float y, float width, float height, float mouseX, float mouseY, boolean custom) {
        String labelKey = (mod.name + "." + setting.name).toLowerCase(Locale.getDefault());
        float labelW = FPSMaster.fontManager.s16.drawString(
                FPSMaster.i18n.get(labelKey),
                x + 10,
                y + 3,
                ClickGuiTheme.textSecondary().getRGB()
        );

        float colorBoxX = x + labelW + 26;
        float modeBoxX = colorBoxX + COLOR_BOX_W + 4f;

        CustomColor customColor = binding.get();
        Color previewColor = setting.getColor();

        Rects.rounded(Math.round(colorBoxX), Math.round(y + 1), Math.round(COLOR_BOX_W), Math.round(COLOR_BOX_H), 3, ClickGuiTheme.pickerBg().getRGB());
        Rects.rounded(Math.round(colorBoxX + 1), Math.round(y + 2), 12, 12, 3, previewColor.getRGB());
        FPSMaster.fontManager.s16.drawString(
                "#" + Integer.toHexString(previewColor.getRGB()).toUpperCase(Locale.getDefault()),
                colorBoxX + 18,
                y + 2,
                ClickGuiTheme.hexText().getRGB()
        );

        boolean showPalette = setting.getColorType() == ColorSetting.ColorType.STATIC || setting.getColorType() == ColorSetting.ColorType.WAVE;
        float targetHeight = expand ? (showPalette ? 80f : 34f) : 0f;
        expandedHeight = (float) AnimMath.base(expandedHeight, targetHeight, 0.2);

        if (expandedHeight > 1f) {
            Rects.rounded(Math.round(modeBoxX), Math.round(y + 1), Math.round(MODE_BOX_W), Math.round(COLOR_BOX_H), 3, ClickGuiTheme.pickerBg().getRGB());
            FPSMaster.fontManager.s16.drawCenteredString(
                    FPSMaster.i18n.get(setting.getColorType().i18nKey),
                    modeBoxX + MODE_BOX_W / 2f,
                    y + 2,
                    ClickGuiTheme.modeText().getRGB()
            );
            if (showPalette) {
                renderStaticOrWaveEditor(screen, x, y, mouseX, mouseY, labelW, customColor, expandedHeight);
            } else {
                renderDynamicEditor(screen, x, y, mouseX, mouseY, labelW, customColor);
            }
        }

        ScaledGuiScreen.PointerEvent paletteClick = screen.consumePressInBounds(colorBoxX, y + 1, COLOR_BOX_W, COLOR_BOX_H, 0);
        if (paletteClick != null) {
            expand = !expand;
        }

        if (expandedHeight > 1f) {
            ScaledGuiScreen.PointerEvent modeClick = screen.consumePressInBounds(modeBoxX, y + 1, MODE_BOX_W, COLOR_BOX_H, 0);
            if (modeClick != null) {
                setting.cycleColorType();
            }
        }

        this.height = expandedHeight + 20f;
    }

    private void renderStaticOrWaveEditor(ScaledGuiScreen screen, float x, float y, float mouseX, float mouseY, float labelW, CustomColor customColor, float pickerHeight) {
        float pickerX = x + labelW + 26;
        float pickerY = y + 16;

        if (OSUtil.supportShader()) {
            GradientUtils.applyGradient(
                    UiScale.toPixel(pickerX),
                    UiScale.toPixel(pickerY),
                    UiScale.toPixel(PICKER_W),
                    UiScale.toPixel(pickerHeight),
                    1f,
                    Color.getHSBColor(customColor.hue, 0.0f, 0f),
                    Color.getHSBColor(customColor.hue, 0f, 1f),
                    Color.getHSBColor(customColor.hue, 1f, 0f),
                    Color.getHSBColor(customColor.hue, 1f, 1f),
                    1f,
                    () -> Rects.roundedImage(Math.round(pickerX), Math.round(pickerY), Math.round(PICKER_W), Math.round(max(pickerHeight, 1f)), 4, Color.WHITE)
            );
        } else {
            for (int i = 0; i < pickerHeight; i++) {
                for (int j = 0; j < PICKER_W; j++) {
                    float brightness = 1 - i / pickerHeight;
                    float saturation = j / PICKER_W;
                    Rects.fill(pickerX + j, pickerY + i, 1, 1, Color.getHSBColor(customColor.hue, saturation, brightness).getRGB());
                }
            }
        }

        float saturation = customColor.saturation;
        float brightness = customColor.brightness;
        screen.beginPointerCapture(paletteCaptureId, 0, pickerX, pickerY, PICKER_W, pickerHeight);
        if (screen.isPointerCapturedBy(paletteCaptureId, 0)) {
            saturation = max(min((mouseX - pickerX) / PICKER_W, 1f), 0f);
            brightness = max(min(1f - (mouseY - pickerY) / pickerHeight, 1f), 0f);
        }

        float cursorX = saturation * PICKER_W;
        float cursorY = (1 - brightness) * pickerHeight;
        Images.draw(new ResourceLocation("client/gui/settings/values/color.png"), pickerX + cursorX - 2.5f, pickerY + cursorY - 2.5f, 5f, 5f, -1);

        float hue = customColor.hue;
        Gradients.hue(x + labelW + 110, pickerY, 10, pickerHeight);
        Images.draw(new ResourceLocation("client/gui/settings/values/color.png"), x + labelW + 112.5f, pickerY + pickerHeight * customColor.hue - 2.5f, 5f, 5f, -1);
        screen.beginPointerCapture(hueCaptureId, 0, x + labelW + 110, pickerY, 10f, pickerHeight);
        if (screen.isPointerCapturedBy(hueCaptureId, 0)) {
            hue = max(min((mouseY - pickerY) / pickerHeight, 1f), 0f);
        }

        float alpha = customColor.alpha;
        Images.draw(new ResourceLocation("client/gui/settings/values/alpha.png"), x + labelW + 122, pickerY, 10f, pickerHeight, -1);
        if (OSUtil.supportShader()) {
            GradientUtils.drawGradientVertical(x + labelW + 122, pickerY, 10f, pickerHeight, new Color(255, 255, 255), new Color(255, 255, 255, 0));
        }
        Images.draw(new ResourceLocation("client/gui/settings/values/color.png"), x + labelW + 124.5f, pickerY + pickerHeight * (1 - alpha) - 2.5f, 5f, 5f, -1);
        screen.beginPointerCapture(alphaCaptureId, 0, x + labelW + 122, pickerY, 10f, pickerHeight);
        if (screen.isPointerCapturedBy(alphaCaptureId, 0)) {
            alpha = max(min(1f - (mouseY - pickerY) / pickerHeight, 1f), 0f);
        }

        if (hue != customColor.hue || saturation != customColor.saturation || brightness != customColor.brightness || alpha != customColor.alpha) {
            binding.setHsba(hue, saturation, brightness, alpha);
        }
    }

    private void renderDynamicEditor(ScaledGuiScreen screen, float x, float y, float mouseX, float mouseY, float labelW, CustomColor customColor) {
        float sliderX = x + labelW + 26;
        float satY = y + 20;
        float brightY = y + 34;
        float sliderW = 106f;

        FPSMaster.fontManager.s14.drawString("S", sliderX - 8f, satY - 1f, ClickGuiTheme.textSecondary().getRGB());
        FPSMaster.fontManager.s14.drawString("B", sliderX - 8f, brightY - 1f, ClickGuiTheme.textSecondary().getRGB());

        Rects.rounded(Math.round(sliderX), Math.round(satY), Math.round(sliderW), 6, 3, ClickGuiTheme.inputBg().getRGB());
        Rects.rounded(Math.round(sliderX), Math.round(brightY), Math.round(sliderW), 6, 3, ClickGuiTheme.inputBg().getRGB());
        Rects.rounded(Math.round(sliderX), Math.round(satY), Math.round(sliderW * customColor.saturation), 6, 3, new Color(114, 173, 255));
        Rects.rounded(Math.round(sliderX), Math.round(brightY), Math.round(sliderW * customColor.brightness), 6, 3, new Color(255, 223, 114));

        float saturation = customColor.saturation;
        float brightness = customColor.brightness;
        screen.beginPointerCapture(saturationCaptureId, 0, sliderX, satY, sliderW, 6f);
        if (screen.isPointerCapturedBy(saturationCaptureId, 0)) {
            saturation = max(min((mouseX - sliderX) / sliderW, 1f), 0f);
        }
        screen.beginPointerCapture(brightnessCaptureId, 0, sliderX, brightY, sliderW, 6f);
        if (screen.isPointerCapturedBy(brightnessCaptureId, 0)) {
            brightness = max(min((mouseX - sliderX) / sliderW, 1f), 0f);
        }

        if (saturation != customColor.saturation || brightness != customColor.brightness) {
            binding.setHsba(customColor.hue, saturation, brightness, customColor.alpha);
        }
    }
}

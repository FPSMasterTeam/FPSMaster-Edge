package top.fpsmaster.ui.click.modules.impl;

import net.minecraft.util.ResourceLocation;
import org.lwjgl.input.Mouse;
import top.fpsmaster.FPSMaster;
import top.fpsmaster.features.manager.Module;
import top.fpsmaster.features.settings.impl.ColorSetting;
import top.fpsmaster.features.settings.impl.utils.CustomColor;
import top.fpsmaster.ui.click.MainPanel;
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

public class ColorSettingRender extends SettingRender<ColorSetting> {
    private static final float COLOR_BOX_W = 80f;
    private static final float COLOR_BOX_H = 14f;
    private static final float MODE_BOX_W = 48f;
    private static final float PICKER_W = 80f;

    private float expandedHeight = 0f;
    private boolean expand = false;
    private final ColorSettingBinding binding;

    public ColorSettingRender(Module mod, ColorSetting setting) {
        super(setting);
        this.mod = mod;
        this.binding = new ColorSettingBinding(setting);
    }

    @Override
    public void render(float x, float y, float width, float height, float mouseX, float mouseY, boolean custom) {
        String labelKey = (mod.name + "." + setting.name).toLowerCase(Locale.getDefault());
        float labelW = FPSMaster.fontManager.s16.drawString(
                FPSMaster.i18n.get(labelKey),
                x + 10,
                y + 3,
                new Color(162, 162, 162).getRGB()
        );

        float colorBoxX = x + labelW + 26;
        float modeBoxX = colorBoxX + COLOR_BOX_W + 4f;

        CustomColor customColor = binding.get();
        Color previewColor = setting.getColor();

        Rects.rounded(Math.round(colorBoxX), Math.round(y + 1), Math.round(COLOR_BOX_W), Math.round(COLOR_BOX_H), 3, new Color(39, 39, 39));
        Rects.rounded(Math.round(colorBoxX + 1), Math.round(y + 2), 12, 12, 3, previewColor.getRGB());
        FPSMaster.fontManager.s16.drawString(
                "#" + Integer.toHexString(previewColor.getRGB()).toUpperCase(Locale.getDefault()),
                colorBoxX + 18,
                y + 2,
                new Color(234, 234, 234).getRGB()
        );

        boolean showPalette = setting.getColorType() == ColorSetting.ColorType.STATIC || setting.getColorType() == ColorSetting.ColorType.WAVE;
        float targetHeight = expand ? (showPalette ? 80f : 34f) : 0f;
        expandedHeight = (float) AnimMath.base(expandedHeight, targetHeight, 0.2);

        if (expandedHeight > 1f) {
            Rects.rounded(Math.round(modeBoxX), Math.round(y + 1), Math.round(MODE_BOX_W), Math.round(COLOR_BOX_H), 3, new Color(39, 39, 39));
            FPSMaster.fontManager.s16.drawCenteredString(
                    FPSMaster.i18n.get(setting.getColorType().i18nKey),
                    modeBoxX + MODE_BOX_W / 2f,
                    y + 2,
                    new Color(214, 214, 214).getRGB()
            );
            if (showPalette) {
                renderStaticOrWaveEditor(x, y, mouseX, mouseY, labelW, customColor, expandedHeight);
            } else {
                renderDynamicEditor(x, y, mouseX, mouseY, labelW, customColor);
            }
        }

        ScaledGuiScreen screen = ScaledGuiScreen.getActiveScreen();
        if (screen != null) {
            ScaledGuiScreen.ConsumedClick paletteClick = screen.consumeClickInBounds(colorBoxX, y + 1, COLOR_BOX_W, COLOR_BOX_H);
            if (paletteClick != null && paletteClick.button == 0) {
                expand = !expand;
            }

            if (expandedHeight > 1f) {
                ScaledGuiScreen.ConsumedClick modeClick = screen.consumeClickInBounds(modeBoxX, y + 1, MODE_BOX_W, COLOR_BOX_H);
                if (modeClick != null && modeClick.button == 0) {
                    setting.cycleColorType();
                }
            }
        }

        this.height = expandedHeight + 20f;
    }

    private void renderStaticOrWaveEditor(float x, float y, float mouseX, float mouseY, float labelW, CustomColor customColor, float pickerHeight) {
        float pickerX = x + labelW + 26;
        float pickerY = y + 16;

        if (OSUtil.supportShader()) {
            GradientUtils.applyGradient(
                    pickerX,
                    pickerY,
                    PICKER_W,
                    pickerHeight,
                    1f,
                    Color.getHSBColor(customColor.hue, 0.0f, 0f),
                    Color.getHSBColor(customColor.hue, 0f, 1f),
                    Color.getHSBColor(customColor.hue, 1f, 0f),
                    Color.getHSBColor(customColor.hue, 1f, 1f),
                    UiScale.getScale(),
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
        if ((Hover.is(pickerX, pickerY, PICKER_W, pickerHeight, (int) mouseX, (int) mouseY) && Mouse.isButtonDown(0)) || MainPanel.dragLock.equals(mod.name + setting.name + 1)) {
            if (MainPanel.dragLock.equals("null") && Mouse.isButtonDown(0)) {
                MainPanel.dragLock = mod.name + setting.name + 1;
            }
            if (MainPanel.dragLock.equals(mod.name + setting.name + 1)) {
                saturation = max(min((mouseX - pickerX) / PICKER_W, 1f), 0f);
                brightness = max(min(1f - (mouseY - (y + 15)) / pickerHeight, 1f), 0f);
            }
        }

        float cursorX = saturation * PICKER_W;
        float cursorY = (1 - brightness) * pickerHeight;
        Images.draw(new ResourceLocation("client/gui/settings/values/color.png"), pickerX + cursorX - 2.5f, y + 15 + cursorY - 2.5f, 5f, 5f, -1);

        float hue = customColor.hue;
        Gradients.hue(x + labelW + 110, y + 16, 10, pickerHeight);
        Images.draw(new ResourceLocation("client/gui/settings/values/color.png"), x + labelW + 112.5f, y + 14 + pickerHeight * customColor.hue, 5f, 5f, -1);
        if ((Hover.is(x + labelW + 110, y + 16, 10f, pickerHeight, (int) mouseX, (int) mouseY) && Mouse.isButtonDown(0)) || MainPanel.dragLock.equals(mod.name + setting.name + 2)) {
            if (MainPanel.dragLock.equals("null")) {
                MainPanel.dragLock = mod.name + setting.name + 2;
            }
            if (MainPanel.dragLock.equals(mod.name + setting.name + 2)) {
                hue = max(min((mouseY - (y + 15)) / pickerHeight, 1f), 0f);
            }
        }

        float alpha = customColor.alpha;
        Images.draw(new ResourceLocation("client/gui/settings/values/alpha.png"), x + labelW + 122, y + 16, 10f, pickerHeight, -1);
        if (OSUtil.supportShader()) {
            GradientUtils.drawGradientVertical(x + labelW + 122, y + 16, 10f, pickerHeight, new Color(255, 255, 255), new Color(255, 255, 255, 0));
        }
        Images.draw(new ResourceLocation("client/gui/settings/values/color.png"), x + labelW + 124.5f, y + 13.5f + pickerHeight * (1 - alpha), 5f, 5f, -1);
        if ((Hover.is(x + labelW + 122, y + 16, 10f, pickerHeight, (int) mouseX, (int) mouseY) && Mouse.isButtonDown(0)) || MainPanel.dragLock.equals(mod.name + setting.name + 3)) {
            if (MainPanel.dragLock.equals("null")) {
                MainPanel.dragLock = mod.name + setting.name + 3;
            }
            if (MainPanel.dragLock.equals(mod.name + setting.name + 3)) {
                alpha = max(min(1f - (mouseY - (y + 15)) / pickerHeight, 1f), 0f);
            }
        }

        if (!Mouse.isButtonDown(0)) {
            MainPanel.dragLock = "null";
        }

        if (hue != customColor.hue || saturation != customColor.saturation || brightness != customColor.brightness || alpha != customColor.alpha) {
            binding.setHsba(hue, saturation, brightness, alpha);
        }
    }

    private void renderDynamicEditor(float x, float y, float mouseX, float mouseY, float labelW, CustomColor customColor) {
        float sliderX = x + labelW + 26;
        float satY = y + 20;
        float brightY = y + 34;
        float sliderW = 106f;

        FPSMaster.fontManager.s14.drawString("S", sliderX - 8f, satY - 1f, new Color(200, 200, 200).getRGB());
        FPSMaster.fontManager.s14.drawString("B", sliderX - 8f, brightY - 1f, new Color(200, 200, 200).getRGB());

        Rects.rounded(Math.round(sliderX), Math.round(satY), Math.round(sliderW), 6, 3, new Color(48, 48, 48));
        Rects.rounded(Math.round(sliderX), Math.round(brightY), Math.round(sliderW), 6, 3, new Color(48, 48, 48));
        Rects.rounded(Math.round(sliderX), Math.round(satY), Math.round(sliderW * customColor.saturation), 6, 3, new Color(114, 173, 255));
        Rects.rounded(Math.round(sliderX), Math.round(brightY), Math.round(sliderW * customColor.brightness), 6, 3, new Color(255, 223, 114));

        float saturation = customColor.saturation;
        float brightness = customColor.brightness;
        if ((Hover.is(sliderX, satY, sliderW, 6f, (int) mouseX, (int) mouseY) && Mouse.isButtonDown(0)) || MainPanel.dragLock.equals(mod.name + setting.name + 4)) {
            if (MainPanel.dragLock.equals("null") && Mouse.isButtonDown(0)) {
                MainPanel.dragLock = mod.name + setting.name + 4;
            }
            if (MainPanel.dragLock.equals(mod.name + setting.name + 4)) {
                saturation = max(min((mouseX - sliderX) / sliderW, 1f), 0f);
            }
        }
        if ((Hover.is(sliderX, brightY, sliderW, 6f, (int) mouseX, (int) mouseY) && Mouse.isButtonDown(0)) || MainPanel.dragLock.equals(mod.name + setting.name + 5)) {
            if (MainPanel.dragLock.equals("null") && Mouse.isButtonDown(0)) {
                MainPanel.dragLock = mod.name + setting.name + 5;
            }
            if (MainPanel.dragLock.equals(mod.name + setting.name + 5)) {
                brightness = max(min((mouseX - sliderX) / sliderW, 1f), 0f);
            }
        }

        if (!Mouse.isButtonDown(0)) {
            MainPanel.dragLock = "null";
        }

        if (saturation != customColor.saturation || brightness != customColor.brightness) {
            binding.setHsba(customColor.hue, saturation, brightness, customColor.alpha);
        }
    }
}

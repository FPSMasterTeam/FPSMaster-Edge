package top.fpsmaster.ui.click.modules.impl;

import net.minecraft.util.ResourceLocation;
import org.lwjgl.input.Mouse;
import top.fpsmaster.FPSMaster;
import top.fpsmaster.features.manager.Module;
import top.fpsmaster.features.settings.impl.ColorSetting;
import top.fpsmaster.features.settings.impl.utils.CustomColor;
import top.fpsmaster.ui.click.MainPanel;
import top.fpsmaster.ui.click.modules.SettingRender;
import top.fpsmaster.utils.math.animation.AnimationUtils;
import top.fpsmaster.utils.os.OSUtil;
import top.fpsmaster.utils.render.shader.GradientUtils;
import top.fpsmaster.utils.render.Render2DUtils;

import java.awt.Color;
import java.util.Locale;

import static java.lang.Math.max;
import static java.lang.Math.min;

public class ColorSettingRender extends SettingRender<ColorSetting> {

    private float aHeight = 0f;
    private boolean expand = false;

    public ColorSettingRender(Module mod, ColorSetting setting) {
        super(setting);
        this.mod = mod;
    }

    @Override
    public void render(
            float x, float y, float width, float height, float mouseX, float mouseY, boolean custom
    ) {
        float tW = FPSMaster.fontManager.s16.drawString(
                FPSMaster.i18n.get((mod.name + "." + setting.name).toLowerCase(Locale.getDefault())),
                x + 10, y + 3, FPSMaster.theme.getTextColorDescription().getRGB()
        );
        Render2DUtils.drawOptimizedRoundedRect(x + tW + 26, y + 1, 80f, 14f, FPSMaster.theme.getBackground());

        CustomColor customColor = setting.getValue();
        Render2DUtils.drawOptimizedRoundedRect(x + tW + 27, y + 2, 12f, 12f, customColor.getRGB());

        FPSMaster.fontManager.s16.drawString(
                "#" + Integer.toHexString(setting.getRGB()).toUpperCase(Locale.getDefault()),
                x + tW + 44, y + 2, FPSMaster.theme.getTextColorTitle().getRGB()
        );

        if (aHeight > 1) {
            if (OSUtil.supportShader()) {
                GradientUtils.applyGradient(
                        x + tW + 26, y + 15, 80f, aHeight, 1f,
                        Color.getHSBColor(customColor.hue, 0.0f, 0f),
                        Color.getHSBColor(customColor.hue, 0f, 1f),
                        Color.getHSBColor(customColor.hue, 1f, 0f),
                        Color.getHSBColor(customColor.hue, 1f, 1f),
                        Render2DUtils.getFixedScale(),
                        () -> Render2DUtils.drawRoundedRectImage(
                                x + tW + 26, y + 16, 80f, max(aHeight, 1f), 4,
                                new Color(255, 255, 255)
                        )
                );
            }

            float saturation = customColor.saturation;
            float brightness = customColor.brightness;

            if (Render2DUtils.isHovered(
                    x + tW + 26, y + 16, 80f, 80f, (int) mouseX, (int) mouseY
            ) && Mouse.isButtonDown(0) || MainPanel.dragLock.equals(mod.name + setting.name + 1)) {
                if (MainPanel.dragLock.equals("null") && Mouse.isButtonDown(0)) {
                    MainPanel.dragLock = mod.name + setting.name + 1;
                }
                if (MainPanel.dragLock.equals(mod.name + setting.name + 1)) {
                    saturation = max(min((mouseX - (x + tW + 26)) / 80, 1f), 0f);
                    brightness = max(min(1 - (mouseY - (y + 15)) / 80, 1f), 0f);
                }
            }

            // Draw the selected position circle
            float cX = saturation * 80;
            float cY = (1 - brightness) * aHeight;
            Render2DUtils.drawImage(
                    new ResourceLocation("client/gui/settings/values/color.png"),
                    x + tW + 26 + cX - 2.5f, y + 15 + cY - 2.5f, 5f, 5f, -1
            );

            // Hue adjustment
            float hue = customColor.hue;
            Render2DUtils.drawHue(x + tW + 110, y + 16, 10, aHeight);
            Render2DUtils.drawImage(
                    new ResourceLocation("client/gui/settings/values/color.png"),
                    x + tW + 112.5f, y + 14 + aHeight * customColor.hue,
                    5f, 5f, -1
            );

            if (Render2DUtils.isHovered(
                    x + tW + 110, y + 16, 10f, aHeight, (int) mouseX, (int) mouseY
            ) && Mouse.isButtonDown(0) || MainPanel.dragLock.equals(mod.name + setting.name + 2)) {
                if (MainPanel.dragLock.equals("null")) {
                    MainPanel.dragLock = mod.name + setting.name + 2;
                }
                if (MainPanel.dragLock.equals(mod.name + setting.name + 2)) {
                    hue = max(min((mouseY - (y + 15)) / aHeight, 1f), 0f);
                }
            }

            // Alpha adjustment
            float alpha = customColor.alpha;
            Render2DUtils.drawImage(
                    new ResourceLocation("client/gui/settings/values/alpha.png"),
                    x + tW + 122, y + 16, 10f, aHeight, -1
            );
            if (OSUtil.supportShader()) {
                GradientUtils.drawGradientVertical(
                        x + tW + 122, y + 16, 10f, aHeight, new Color(255, 255, 255), new Color(255, 255, 255, 0)
                );
            }

            Render2DUtils.drawImage(
                    new ResourceLocation("client/gui/settings/values/color.png"),
                    x + tW + 124.5f, y + 13.5f + aHeight * (1 - alpha), 5f, 5f, -1
            );

            if (Render2DUtils.isHovered(
                    x + tW + 122, y + 16, 10f, aHeight, (int) mouseX, (int) mouseY
            ) && Mouse.isButtonDown(0) || MainPanel.dragLock.equals(mod.name + setting.name + 3)) {
                if (MainPanel.dragLock.equals("null")) {
                    MainPanel.dragLock = mod.name + setting.name + 3;
                }
                if (MainPanel.dragLock.equals(mod.name + setting.name + 3)) {
                    alpha = max(min(1 - (mouseY - (y + 15)) / aHeight, 1f), 0f);
                }
            }

            if (!Mouse.isButtonDown(0)) {
                MainPanel.dragLock = "null";
            }

            // Apply the modified color
            customColor.setColor(hue, saturation, brightness, alpha);
        } else {
            customColor.setColor(customColor.hue, customColor.saturation,
                    customColor.brightness, customColor.alpha);
        }

        aHeight = expand ? (float) AnimationUtils.base(aHeight, 80.0, 0.2) : (float) AnimationUtils.base(aHeight, 0.0, 0.2);
        this.height = aHeight + 20;
    }

    @Override
    public void mouseClick(
            float x, float y, float width, float height, float mouseX, float mouseY, int btn
    ) {
        float tW = FPSMaster.fontManager.s16.drawString(
                FPSMaster.i18n.get((mod.name + "." + setting.name).toLowerCase(Locale.getDefault())),
                x + 10, y + 2, FPSMaster.theme.getTextColorDescription().getRGB()
        );
        if (Render2DUtils.isHovered(x + tW + 26, y + 1, 80f, 14f, (int) mouseX, (int) mouseY) && btn == 0) {
            expand = !expand;
        }
    }
}

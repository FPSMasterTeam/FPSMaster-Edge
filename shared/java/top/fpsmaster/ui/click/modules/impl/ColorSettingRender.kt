package top.fpsmaster.ui.click.modules.impl

import net.minecraft.util.ResourceLocation
import org.lwjgl.input.Mouse
import top.fpsmaster.FPSMaster
import top.fpsmaster.features.manager.Module
import top.fpsmaster.features.settings.impl.ColorSetting
import top.fpsmaster.ui.click.MainPanel
import top.fpsmaster.ui.click.modules.SettingRender
import top.fpsmaster.utils.math.animation.AnimationUtils.base
import top.fpsmaster.utils.render.shader.GradientUtils
import top.fpsmaster.utils.render.Render2DUtils
import java.awt.Color
import java.util.*
import kotlin.math.max
import kotlin.math.min

class ColorSettingRender(mod: Module, setting: ColorSetting) : SettingRender<ColorSetting>(setting) {
    private var aHeight = 0f
    private var expand = false

    init {
        this.mod = mod
    }

    override fun render(
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        mouseX: Float,
        mouseY: Float,
        custom: Boolean
    ) {
        val tW = FPSMaster.fontManager.s16.drawString(
            FPSMaster.i18n[(mod.name + "." + setting.name).lowercase(
                Locale.getDefault()
            )], x + 10, y + 3, FPSMaster.theme.textColorDescription.rgb
        ).toFloat()
        Render2DUtils.drawOptimizedRoundedRect(x + tW + 26, y + 1, 80f, 14f, FPSMaster.theme.background)
        Render2DUtils.drawOptimizedRoundedRect(x + tW + 27, y + 2, 12f, 12f, setting.value.rGB)
        FPSMaster.fontManager.s16.drawString(
            "#" + Integer.toHexString(setting.rGB).uppercase(Locale.getDefault()),
            x + tW + 44,
            y + 2,
            FPSMaster.theme.textColorTitle.rgb
        )
        if (aHeight > 1) {
            // 饱和度和亮度调节部分
            GradientUtils.applyGradient(
                x + tW + 26, y + 15, 80f, aHeight, 1f,
                Color.getHSBColor(setting.value.hue, 0.0f, 0f),
                Color.getHSBColor(setting.value.hue, 0f, 1f),
                Color.getHSBColor(setting.value.hue, 1f, 0f),
                Color.getHSBColor(setting.value.hue, 1f, 1f),
                Render2DUtils.getFixedScale()
            ) {
                Render2DUtils.drawRoundedRectImage(
                    x + tW + 26,
                    y + 16,
                    80f,
                    max(aHeight, 1f),
                    3,
                    Color.getHSBColor(setting.value.hue, setting.value.saturation, setting.value.brightness)
                )
            }
            var saturation = setting.value.saturation
            var brightness = setting.value.brightness
            if (Render2DUtils.isHovered(
                    x + tW + 26,
                    y + 16,
                    80f,
                    80f,
                    mouseX.toInt(),
                    mouseY.toInt()
                ) && Mouse.isButtonDown(0) || MainPanel.dragLock == mod.name + setting.name + 1
            ) {
                if (MainPanel.dragLock == "null" && Mouse.isButtonDown(0)) {
                    MainPanel.dragLock = mod.name + setting.name + 1
                }
                if (MainPanel.dragLock == mod.name + setting.name + 1) {
                    saturation = max(min((mouseX - (x + tW + 26)) / 80, 1f), 0f)
                    brightness = max(min(1 - (mouseY - (y + 15)) / 80, 1f), 0f)
                }
            }

            // 绘制选中位置的圆形
            val cX = saturation * 80
            val cY = (1 - brightness) * aHeight
            Render2DUtils.drawImage(
                ResourceLocation("client/gui/settings/values/color.png"),
                x + tW + 26 + cX - 2.5f,
                y + 15 + cY - 2.5f,
                5f,
                5f,
                -1
            )

            // 色度调节部分
            var hue = setting.value.hue
            Render2DUtils.drawHue(x + tW + 110, y + 16, 10, aHeight)
            Render2DUtils.drawImage(
                ResourceLocation("client/gui/settings/values/color.png"),
                x + tW + 112.5f, y + 14 + aHeight * setting.value.hue,
                5f,
                5f,
                -1
            )
//            Render2DUtils.drawRect(x + tW + 110, y + 16 + aHeight * setting.value.hue, 10f, 1f, -1)
            if (Render2DUtils.isHovered(
                    x + tW + 110,
                    y + 16,
                    10f,
                    aHeight,
                    mouseX.toInt(),
                    mouseY.toInt()
                ) && Mouse.isButtonDown(0) || MainPanel.dragLock == mod.name + setting.name + 2
            ) {
                if (MainPanel.dragLock == "null") {
                    MainPanel.dragLock = mod.name + setting.name + 2
                }
                if (MainPanel.dragLock == mod.name + setting.name + 2) {
                    hue = max(min((mouseY - (y + 15)) / aHeight, 1f), 0f)
                }
            }

            //透明度调节部分
            var alpha = setting.value.alpha
            Render2DUtils.drawImage(
                ResourceLocation("client/gui/settings/values/alpha.png"),
                x + tW + 122,
                y + 16,
                10f,
                aHeight,
                -1
            )
            GradientUtils.drawGradientVertical(
                x + tW + 122,
                y + 16,
                10f,
                aHeight,
                Color(255, 255, 255),
                Color(255, 255, 255, 0)
            )

            Render2DUtils.drawImage(
                ResourceLocation("client/gui/settings/values/color.png"),
                x + tW + 124.5f,
                y + 13.5f + aHeight * (1 - alpha),
                5f,
                5f,
                -1
            )
            if (Render2DUtils.isHovered(
                    x + tW + 122,
                    y + 16,
                    10f,
                    aHeight,
                    mouseX.toInt(),
                    mouseY.toInt()
                ) && Mouse.isButtonDown(0) || MainPanel.dragLock == mod.name + setting.name + 3
            ) {
                if (MainPanel.dragLock == "null") {
                    MainPanel.dragLock = mod.name + setting.name + 3
                }
                if (MainPanel.dragLock == mod.name + setting.name + 3) {
                    alpha = max(min(1 - (mouseY - (y + 15)) / aHeight, 1f), 0f)
                }
            }
            if (!Mouse.isButtonDown(0)) {
                MainPanel.dragLock = "null"
            }

            // 应用修改后的颜色
            setting.value.setColor(hue, saturation, brightness, alpha)
        } else {
            setting.value.setColor(
                setting.value.hue,
                setting.value.saturation,
                setting.value.brightness,
                setting.value.alpha
            )
        }
        aHeight = if (expand) {
            base(aHeight.toDouble(), 80.0, 0.2).toFloat()
        } else {
            base(aHeight.toDouble(), 0.0, 0.2).toFloat()
        }
        this.height = aHeight + 20
    }

    override fun mouseClick(x: Float, y: Float, width: Float, height: Float, mouseX: Float, mouseY: Float, btn: Int) {
        val tW = FPSMaster.fontManager.s16.drawString(
            FPSMaster.i18n[(mod.name + "." + setting.name).lowercase(
                Locale.getDefault()
            )], x + 10, y + 2, FPSMaster.theme.textColorDescription.rgb
        ).toFloat()
        if (Render2DUtils.isHovered(x + tW + 26, y + 1, 80f, 14f, mouseX.toInt(), mouseY.toInt()) && btn == 0) {
            expand = !expand
        }
    }
}

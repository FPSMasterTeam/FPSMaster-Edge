package top.fpsmaster.ui.click.modules

import net.minecraft.util.ResourceLocation
import top.fpsmaster.FPSMaster
import top.fpsmaster.features.manager.Category
import top.fpsmaster.features.manager.Module
import top.fpsmaster.features.settings.Setting
import top.fpsmaster.features.settings.impl.*
import top.fpsmaster.modules.lua.LuaModule
import top.fpsmaster.ui.click.MainPanel
import top.fpsmaster.ui.click.modules.impl.*
import top.fpsmaster.utils.Utility
import top.fpsmaster.utils.math.animation.AnimationUtils.base
import top.fpsmaster.utils.math.animation.ColorAnimation
import top.fpsmaster.utils.math.animation.Type
import top.fpsmaster.utils.render.Render2DUtils
import java.util.*
import java.util.function.Consumer

class ModuleRenderer(override var mod: Module) : ValueRender() {
    private var settingsRenderers = ArrayList<SettingRender<*>>()
    private var settingHeight = 0f
    private var border = 0f
    private var expand = false
    private var background = ColorAnimation()
    var content = ColorAnimation(FPSMaster.theme.moduleDisabled)

    init {
        content =
            ColorAnimation(if (mod.isEnabled) FPSMaster.theme.moduleEnabled else FPSMaster.theme.moduleDisabled)
        mod.settings.forEach(Consumer { setting: Setting<*>? ->
            when (setting) {
                is BooleanSetting -> {
                    settingsRenderers.add(BooleanSettingRender(mod, (setting as BooleanSetting?)!!))
                }

                is ModeSetting -> {
                    settingsRenderers.add(ModeSettingRender(mod, (setting as ModeSetting?)!!))
                }

                is TextSetting -> {
                    settingsRenderers.add(TextSettingRender(mod, (setting as TextSetting?)!!))
                }

                is NumberSetting -> {
                    settingsRenderers.add(NumberSettingRender(mod, (setting as NumberSetting?)!!))
                }

                is ColorSetting -> {
                    settingsRenderers.add(ColorSettingRender(mod, (setting as ColorSetting?)!!))
                }

                is BindSetting -> {
                    settingsRenderers.add(BindSettingRender(mod, (setting as BindSetting?)!!))
                }
            }
        })
    }

    override fun render(
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        mouseX: Float,
        mouseY: Float,
        current: Boolean
    ) {
        content.update()
        background.update()
        border = if (Render2DUtils.isHovered(x + 5, y, width - 10, height, mouseX.toInt(), mouseY.toInt())) {
            base(border.toDouble(), 200.0, 0.3).toFloat()
        } else {
            base(border.toDouble(), 30.0, 0.3).toFloat()
        }
        if (mod.isEnabled) {
            content.start(content.color, FPSMaster.theme.moduleTextEnabled, 0.2f, Type.EASE_IN_OUT_QUAD)
            background.start(background.color, FPSMaster.theme.moduleEnabled, 0.2f, Type.EASE_IN_OUT_QUAD)
        } else {
            content.start(content.color, FPSMaster.theme.moduleTextDisabled, 0.2f, Type.EASE_IN_OUT_QUAD)
            background.start(background.color, FPSMaster.theme.moduleDisabled, 0.2f, Type.EASE_IN_OUT_QUAD)
        }
        Render2DUtils.drawOptimizedRoundedRect(
            x + 4.5f,
            y - 0.5f,
            width - 9,
            settingHeight + 38f,
            FPSMaster.theme.moduleEnabled
        )
        Render2DUtils.drawOptimizedRoundedRect(
            x + 5,
            y,
            width - 10,
            settingHeight + 37f,
            FPSMaster.theme.moduleDisabled.rgb
        )
        Render2DUtils.drawOptimizedRoundedBorderRect(
            x + 5, y, width - 10, 37f, 0.5f, background.color, Render2DUtils.reAlpha(
                FPSMaster.theme.moduleBorder, border.toInt()
            )
        )
        if (mod.category === Category.Interface) {
            Render2DUtils.drawImage(
                ResourceLocation("client/textures/modules/interface.png"),
                x + 14,
                y + 10,
                14f,
                14f,
                content.color.rgb
            )
        } else if (mod is LuaModule) {
            Render2DUtils.drawImage(
                ResourceLocation("client/textures/modules/lua.png"),
                x + 14,
                y + 10,
                14f,
                14f,
                content.color.rgb
            )
        } else {
            Render2DUtils.drawImage(
                ResourceLocation("client/textures/modules/" + mod.name.lowercase(Locale.getDefault()) + ".png"),
                x + 14,
                y + 10,
                14f,
                14f,
                content.color.rgb
            )
        }
        FPSMaster.fontManager.s18.drawString(
            FPSMaster.i18n[mod.name.lowercase(Locale.getDefault())],
            x + 40,
            y + 9,
            content.color.rgb
        )
        FPSMaster.fontManager.s16.drawString(
            FPSMaster.i18n[mod.name.lowercase(Locale.getDefault()) + ".desc"],
            x + 40,
            y + 20,
            FPSMaster.theme.textColorDescription.rgb
        )
        var settingsHeight = 0f
        if (expand) {
            for (settingsRenderer in settingsRenderers) {
                if (settingsRenderer.setting.getVisible()) {
                    settingsRenderer.render(
                        x + 5,
                        y + 40 + settingsHeight,
                        width - 10,
                        12f,
                        mouseX,
                        mouseY,
                        MainPanel.curModule === mod
                    )
                    settingsHeight += settingsRenderer.height + 6
                }
            }
        }
        settingHeight = base(settingHeight.toDouble(), settingsHeight.toDouble(), 0.2).toFloat()
        this.height = settingHeight
    }

    override fun mouseClick(x: Float, y: Float, width: Float, height: Float, mouseX: Float, mouseY: Float, btn: Int) {
        var settingsHeight = 0f
        if (expand) {
            for (settingsRenderer in settingsRenderers) {
                if (settingsRenderer.setting.getVisible()) {
                    settingsRenderer.mouseClick(x + 5, y + 40 + settingsHeight, width - 10, 12f, mouseX, mouseY, btn)
                    settingsHeight += settingsRenderer.height + 6
                }
            }
        }
        if (Render2DUtils.isHovered(x + 5, y, width - 10, 40f, mouseX.toInt(), mouseY.toInt())) {
            if (btn == 0) {
                mod.toggle()
            } else if (btn == 1) {
                expand = !expand
                MainPanel.curModule = null
            }
        }
    }

    override fun keyTyped(typedChar: Char, keyCode: Int) {
        if (expand) {
            for (settingsRenderer in settingsRenderers) {
                settingsRenderer.keyTyped(typedChar, keyCode)
            }
        }
    }
}

package top.fpsmaster.ui.custom

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiChat
import net.minecraft.client.gui.ScaledResolution
import org.lwjgl.input.Mouse
import org.lwjgl.opengl.GL11
import top.fpsmaster.FPSMaster
import top.fpsmaster.font.impl.UFontRenderer
import top.fpsmaster.features.impl.InterfaceModule
import top.fpsmaster.features.impl.interfaces.ClientSettings
import top.fpsmaster.ui.click.MainPanel
import top.fpsmaster.utils.Utility
import top.fpsmaster.utils.math.animation.AnimationUtils.base
import top.fpsmaster.utils.render.Render2DUtils
import top.fpsmaster.interfaces.ProviderManager
import top.fpsmaster.utils.render.shader.BlurBuffer
import java.awt.Color
import kotlin.math.max
import kotlin.math.min

open class Component(clazz: Class<*>?) {

    private var dragX = 0f
    private var dragY = 0f

    @JvmField
    var mod: InterfaceModule = FPSMaster.moduleManager.getModule(clazz!!) as InterfaceModule
    var position = Position.LT

    @JvmField
    var x = 0f

    @JvmField
    var y = 0f

    @JvmField
    var width = 0f

    @JvmField
    var height = 0f

    @JvmField
    var scale = 1f

    var allowScale = false


    open fun draw(x: Float, y: Float) {}
    var alpha = 0f

    fun shouldDisplay(): Boolean {
        return mod.isEnabled
    }

    fun getRealPosition(): FloatArray {
        val sr = ScaledResolution(Minecraft.getMinecraft())
        var rX = 0f
        var rY = 0f
        x = max(0f, min(1f, x))
        y = max(0f, min(1f, y))
        var scaleFactor: Int = if (ClientSettings.Companion.fixedScale.value) {
            sr.scaleFactor;
        } else {
            2;
        }
        val guiWidth = sr.scaledWidth / 2f * scaleFactor
        val guiHeight = sr.scaledHeight / 2f * scaleFactor

        when (position) {
            Position.LT -> {
                rX = x * guiWidth / 2f
                rY = y * guiHeight / 2f
            }

            Position.RT -> {
                rX = guiWidth - (x * guiWidth / 2f + width)
                rY = y * guiHeight / 2f
            }

            Position.LB -> {
                rX = x * guiWidth / 2f
                rY = guiHeight - (y * guiHeight / 2f + height)
            }

            Position.RB -> {
                rX = guiWidth - (x * guiWidth / 2f + width)
                rY = guiHeight - (y * guiHeight / 2f + height)
            }

            Position.CT -> {

            }
        }
        return floatArrayOf(rX, rY)
    }

    fun display(mouseX: Int, mouseY: Int) {
        val rX = getRealPosition()[0]
        val rY = getRealPosition()[1]
        draw(rX.toInt().toFloat(), rY.toInt().toFloat())
        if (Utility.mc.currentScreen !is GuiChat && Utility.mc.currentScreen !is MainPanel
        ) return
        var width = width * scale
        var height = height * scale
        val drag = FPSMaster.componentsManager.dragLock == mod.name
        alpha = if (Render2DUtils.isHovered(rX, rY, width, height, mouseX, mouseY) || drag) {
            if ((base(alpha.toDouble(), 50.0, 0.1).toFloat()).isNaN())
                0f else base(alpha.toDouble(), 50.0, 0.1).toFloat()
        } else {
            if ((base(alpha.toDouble(), 0.0, 0.1).toFloat()).isNaN())
                0f else base(alpha.toDouble(), 0.0, 0.1).toFloat()
        }
        Render2DUtils.drawOptimizedRoundedRect(rX - 2, rY - 2, width + 4, height + 4, Color(0, 0, 0, alpha.toInt()))
        if (!Mouse.isButtonDown(0)) {
            FPSMaster.componentsManager.dragLock = ""
        }
        if (Render2DUtils.isHovered(rX, rY, width, height, mouseX, mouseY) || drag) {
            if (allowScale) {
                val dWheel = Mouse.getDWheel()
                if (dWheel > 0) {
                    this.scaleUp()
                } else if (dWheel < 0) {
                    this.scaleDown()
                }
            }
            FPSMaster.fontManager.s14.drawString(
                FPSMaster.i18n[mod.name.lowercase()] + " " + (scale * 10).toInt() / 10f + "x",
                rX,
                rY - 10,
                -1
            )
            if (!Mouse.isButtonDown(0)) return
            if (!drag && FPSMaster.componentsManager.dragLock.isEmpty()) {
                dragX = mouseX - rX
                dragY = mouseY - rY
                FPSMaster.componentsManager.dragLock = mod.name
            }
            if (FPSMaster.componentsManager.dragLock == mod.name) {
                move(mouseX.toFloat(), mouseY.toFloat())
                FPSMaster.componentsManager.dragLock = mod.name
            }

        }


    }

    fun scaleUp() {
        if (scale < 2.5f)
            scale += 0.1f
    }

    fun scaleDown() {
        if (scale > 0.5f)
            scale -= 0.1f
    }

    private fun move(x: Float, y: Float) {
        val sr = ScaledResolution(Utility.mc)
        var scaleFactor: Int = if (ClientSettings.Companion.fixedScale.value) {
            sr.scaleFactor;
        } else {
            2;
        }
        val guiWidth = sr.scaledWidth / 2f * scaleFactor
        val guiHeight = sr.scaledHeight / 2f * scaleFactor
        var changeX = 0f
        var changeY = 0f
        if (x > guiWidth / 2f) {
            if (y >= guiHeight / 2f) position = Position.RB else if (y < guiHeight / 2f) position =
                Position.RT
        } else {
            if (y >= guiHeight / 2f) position = Position.LB else if (y < guiHeight / 2f) position =
                Position.LT
        }
        when (position) {
            Position.LT -> {
                changeX = x - dragX
                changeY = y - dragY
            }

            Position.RT -> {
                changeX = guiWidth - x - width + dragX
                changeY = y - dragY
            }

            Position.LB -> {
                changeX = x - dragX
                changeY = guiHeight - y - height + dragY
            }

            Position.RB -> {
                changeX = guiWidth - x - width + dragX
                changeY = guiHeight - y - height + dragY
            }

            Position.CT -> {

            }
        }
        this.x = changeX / guiWidth * 2f
        this.y = changeY / guiHeight * 2f
    }

    fun drawRect(x: Float, y: Float, width: Float, height: Float, color: Color?) {
        var width = width * scale
        var height = height * scale
        BlurBuffer.blurArea(x, y, width, height, true)
        if (mod.bg.value)
            if (mod.rounded.value) {
                Render2DUtils.drawOptimizedRoundedRect(x, y, width, height, mod.roundRadius.value.toInt(), color!!.rgb)
            } else {
                Render2DUtils.drawRect(x, y, width, height, color)
            }
    }

    fun drawString(fontSize: Int, text: String, x: Float, y: Float, color: Int) {
        drawString(fontSize, false, text, x, y, color)
    }

    fun drawString(fontSize: Int, bold: Boolean, text: String, x: Float, y: Float, color: Int) {
        val fontSize = (fontSize * scale).toInt()
        val font = FPSMaster.fontManager.getFont(fontSize)
        if (mod.betterFont.value) {
            if (mod.fontShadow.value) font.drawStringWithShadow(text, x, y, color) else font.drawString(
                text,
                x,
                y,
                color
            )
        } else {
            GL11.glPushMatrix()
            GL11.glTranslated(x.toDouble(), y.toDouble(), 0.0)
            GL11.glScaled(scale.toDouble(), scale.toDouble(), 1.0)
            if (mod.fontShadow.value) ProviderManager.mcProvider.getFontRenderer()
                .drawStringWithShadow(text, 0f, 0f, color) else ProviderManager.mcProvider.drawString(text, 0f, 0f, color)

            GL11.glPopMatrix()
        }
    }

    protected fun getStringWidth(fontSize: Int, name: String): Float {
        val font = FPSMaster.fontManager.getFont(fontSize)

        return if (mod.betterFont.value) {
            font.getStringWidth(name).toFloat()
        } else {
            ProviderManager.mcProvider.getFontRenderer().getStringWidth(name).toFloat()
        }
    }
}

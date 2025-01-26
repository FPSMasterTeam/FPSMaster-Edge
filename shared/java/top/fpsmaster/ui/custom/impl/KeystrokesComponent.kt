package top.fpsmaster.ui.custom.impl

import org.lwjgl.input.Keyboard
import org.lwjgl.input.Mouse
import top.fpsmaster.FPSMaster
import top.fpsmaster.features.impl.interfaces.Keystrokes
import top.fpsmaster.ui.custom.Component
import top.fpsmaster.utils.math.animation.ColorAnimation
import java.awt.Color

class KeystrokesComponent : Component(Keystrokes::class.java) {
    private var keys = ArrayList<Key>()

    init {
        keys.add(Key("W", Keyboard.KEY_W, 20, 0))
        keys.add(Key("A", Keyboard.KEY_A, 0, 20))
        keys.add(Key("S", Keyboard.KEY_S, 20, 20))
        keys.add(Key("D", Keyboard.KEY_D, 40, 20))
        keys.add(Key("LMB", -1, 0, 40))
        keys.add(Key("RMB", -2, 40, 40))
    }

    override fun draw(x: Float, y: Float) {
        super.draw(x, y)
        for (key in keys) {
            key.render(x, y, 0f, mod.backgroundColor.color, Keystrokes.pressedColor.color)
        }
        width = 60f
        height = 60f
    }

    inner class Key(var name: String, var keyCode: Int, private var xOffset: Int, private var yOffset: Int) {
        var color = ColorAnimation()
        fun render(x: Float, y: Float, speed: Float, color: Color?, color1: Color?) {
            val pressed: Boolean
            val s16b = FPSMaster.fontManager.s16
            when (keyCode) {
                -1 -> {
                    pressed = Mouse.isButtonDown(0)
                    drawRect(x + xOffset, y + yOffset, 28f, 18f, this.color.color)
                    drawRect(x + xOffset, y + yOffset, 28f, 18f, this.color.color)
                    drawString(16, name, x + xOffset + 7, y + yOffset + 4, -1)
                }
                -2 -> {
                    pressed = Mouse.isButtonDown(1)
                    drawRect(x + xOffset - 10, y + yOffset, 28f, 18f, this.color.color)
                    drawRect(x + xOffset - 10, y + yOffset, 28f, 18f, this.color.color)
                    drawString(16, name, x + xOffset - 4, y + yOffset + 4, -1)
                }
                else -> {
                    pressed = Keyboard.isKeyDown(keyCode)
                    drawRect(x + xOffset, y + yOffset, 18f, 18f, this.color.color)
                    drawRect(x + xOffset, y + yOffset, 18f, 18f, this.color.color)
                    drawString(
                        16,
                        name,
                        x.toInt() + xOffset + 9 - getStringWidth(16, name) / 2f,
                        (y.toInt() + yOffset + 4).toFloat(),
                        -1
                    )
                }
            }
            this.color.base((if (pressed) color1 else color)!!)
        }
    }
}

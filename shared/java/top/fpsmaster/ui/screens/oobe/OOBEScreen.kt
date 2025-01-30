package top.fpsmaster.ui.screens.oobe

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiScreen
import net.minecraft.client.gui.ScaledResolution
import org.lwjgl.opengl.GL11
import top.fpsmaster.ui.screens.oobe.impls.Login
import top.fpsmaster.utils.math.animation.Animation
import top.fpsmaster.utils.math.animation.Type
import top.fpsmaster.utils.render.Render2DUtils
import java.awt.Color
import java.io.IOException

class OOBEScreen : GuiScreen() {
    override fun initGui() {
        super.initGui()
        if (scenes.isEmpty()) {
            scenes.add(Login(true))
            currentScene = scenes[0]
            switchAnimation.start(0.0, 100.0, 0.3f, Type.EASE_IN_OUT_QUAD)
        }
    }

    override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
        super.drawScreen(mouseX, mouseY, partialTicks)
        val sr = ScaledResolution(Minecraft.getMinecraft())
        switchAnimation.update()
        if (switchAnimation.value < 99.9 && currentSceneIndex > 0) {
            val previousScene = scenes[currentSceneIndex - 1]
            previousScene.drawScreen(mouseX, mouseY, partialTicks)
            Render2DUtils.drawRect(
                0f, 0f, sr.scaledWidth.toFloat(), sr.scaledHeight.toFloat(), Color(
                    0, 0, 0, Render2DUtils.limit(
                        (2 * switchAnimation.value).toInt().toDouble()
                    )
                )
            )
            GL11.glTranslatef(sr.scaledWidth * (1 - switchAnimation.value / 100f).toFloat(), 0f, 0f)
            currentScene!!.drawScreen(mouseX, mouseY, partialTicks)
            GL11.glTranslatef(-sr.scaledWidth * (1 - switchAnimation.value / 100f).toFloat(), 0f, 0f)
        } else {
            currentScene!!.drawScreen(mouseX, mouseY, partialTicks)
        }
    }

    @Throws(IOException::class)
    override fun mouseClicked(mouseX: Int, mouseY: Int, mouseButton: Int) {
        super.mouseClicked(mouseX, mouseY, mouseButton)
        if (switchAnimation.value > 99.9) {
            currentScene!!.mouseClick(mouseX, mouseY, mouseButton)
        }
    }

    @Throws(IOException::class)
    override fun keyTyped(typedChar: Char, keyCode: Int) {
        if (switchAnimation.value > 99.9) {
            currentScene!!.keyTyped(typedChar, keyCode)
        }
        super.keyTyped(typedChar, keyCode)
    }

    fun nextScene() {
        if (currentSceneIndex < scenes.size - 1) {
            currentSceneIndex++
            currentScene = scenes[currentSceneIndex]
            switchAnimation.reset()
            switchAnimation.start(0.0, 100.0, 0.5f, Type.EASE_IN_OUT_QUAD)
        } else {
            currentSceneIndex = 0
            currentScene = scenes[currentSceneIndex]
        }
    }

    companion object {
        private val scenes = ArrayList<Scene>()
        private var currentScene: Scene? = null
        private var currentSceneIndex = 0
        var switchAnimation = Animation()
    }
}

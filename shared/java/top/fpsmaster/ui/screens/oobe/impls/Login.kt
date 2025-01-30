package top.fpsmaster.ui.screens.oobe.impls

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.ScaledResolution
import top.fpsmaster.FPSMaster
import top.fpsmaster.modules.account.AccountManager.login
import top.fpsmaster.ui.common.TextField
import top.fpsmaster.ui.screens.oobe.Scene
import top.fpsmaster.ui.common.GuiButton
import top.fpsmaster.ui.screens.mainmenu.MainMenu
import top.fpsmaster.utils.math.animation.ColorAnimation
import top.fpsmaster.utils.math.animation.Type
import top.fpsmaster.utils.os.FileUtils.saveTempValue
import top.fpsmaster.utils.render.Render2DUtils
import java.awt.Color
import java.awt.Desktop
import java.net.URI

class Login(isOOBE: Boolean) : Scene() {
    private var msg: String? = null
    private var msgbox = false
    var btn: GuiButton
    private var btn2: GuiButton
    private var username: TextField =
        TextField(
            FPSMaster.fontManager.s18,
            false,
            FPSMaster.i18n["oobe.login.username"],
            -1,
            Color(200, 200, 200).rgb,
            32
        )
    private var password: TextField =
        TextField(
            FPSMaster.fontManager.s18,
            true,
            FPSMaster.i18n["oobe.login.password"],
            -1,
            Color(200, 200, 200).rgb,
            32
        )
    private var msgBoxAnimation = ColorAnimation(Color(0, 0, 0, 0))

    init {
        username.text = FPSMaster.configManager.configure.getOrCreate("username", "")
        btn = GuiButton(FPSMaster.i18n["oobe.login.login"]) {
            try {
                val login = login(username.text, password.text)
                if (login["code"].asInt == 200) {
                    if (FPSMaster.accountManager != null) {
                        FPSMaster.accountManager.username = username.text
                        FPSMaster.accountManager.token = login["msg"].asString
                    }
                    saveTempValue("token", FPSMaster.accountManager.token)
                    FPSMaster.INSTANCE.loggedIn = true
                    if (isOOBE) {
                        FPSMaster.oobeScreen.nextScene()
                    } else {
                        Minecraft.getMinecraft().displayGuiScreen(MainMenu())
                    }
                } else {
                    msg = login["msg"].asString
                    msgbox = true
                }
            } catch (e: Exception) {
                e.printStackTrace()
                msg = "未知错误: " + e.message
                msgbox = true
            }
        }
        btn2 = GuiButton(FPSMaster.i18n["oobe.login.skip"]) {
            if (isOOBE) {
                FPSMaster.oobeScreen.nextScene()
            } else {
                Minecraft.getMinecraft().displayGuiScreen(MainMenu())
            }
            FPSMaster.configManager.configure["username"] = "offline"
        }
    }

    override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
        super.drawScreen(mouseX, mouseY, partialTicks)
        val sr = ScaledResolution(Minecraft.getMinecraft())
//        GradientUtils.drawGradient(
//            0f,
//            0f,
//            sr.scaledWidth.toFloat(),
//            sr.scaledHeight.toFloat(),
//            1f,
//            Color(255, 255, 255),
//            Color(235, 242, 255),
//            Color(255, 255, 255),
//            Color(235, 242, 255)
//        )
        Render2DUtils.drawRect(
            0f,
            0f,
            sr.scaledWidth.toFloat(),
            sr.scaledHeight.toFloat(),
            Color(235, 242, 255).rgb
        )
//        GlStateManager.pushMatrix()
//        GlStateManager.enableBlend()
        FPSMaster.fontManager.s24.drawCenteredString(
            FPSMaster.i18n["oobe.login.desc"],
            sr.scaledWidth / 2f,
            sr.scaledHeight / 2f - 90,
            FPSMaster.theme.textColorDescription.rgb
        )
        FPSMaster.fontManager.s18.drawString(
            FPSMaster.i18n["oobe.login.register"],
            sr.scaledWidth / 2f - 90,
            sr.scaledHeight / 2f + 15,
            FPSMaster.theme.primary.rgb
        )
        FPSMaster.fontManager.s40.drawCenteredString(
            FPSMaster.i18n["oobe.login.title"],
            sr.scaledWidth / 2f,
            sr.scaledHeight / 2f - 75,
            FPSMaster.theme.primary.rgb
        )
        btn.render(sr.scaledWidth / 2f - 70, sr.scaledHeight / 2f + 40, 60f, 24f, mouseX.toFloat(), mouseY.toFloat())
        btn2.render(sr.scaledWidth / 2f + 5, sr.scaledHeight / 2f + 40, 60f, 24f, mouseX.toFloat(), mouseY.toFloat())
        username.drawTextBox(sr.scaledWidth / 2f - 90, sr.scaledHeight / 2f - 40, 180f, 20f)
        password.drawTextBox(sr.scaledWidth / 2f - 90, sr.scaledHeight / 2f - 10, 180f, 20f)
        msgBoxAnimation.update()
        if (msgbox) {
            msgBoxAnimation.start(Color(0, 0, 0, 0), Color(0, 0, 0, 100), 0.6f, Type.EASE_IN_OUT_QUAD)
            Render2DUtils.drawRect(0f, 0f, sr.scaledWidth.toFloat(), sr.scaledHeight.toFloat(), msgBoxAnimation.color)
            Render2DUtils.drawOptimizedRoundedRect(
                sr.scaledWidth / 2f - 100,
                sr.scaledHeight / 2f - 50,
                200f,
                100f,
                Color(255, 255, 255)
            )
            Render2DUtils.drawOptimizedRoundedRect(
                sr.scaledWidth / 2f - 100,
                sr.scaledHeight / 2f - 50,
                200f,
                20f,
                Color(113, 127, 254)
            )
            FPSMaster.fontManager.s18.drawString(
                FPSMaster.i18n["oobe.login.info"],
                sr.scaledWidth / 2f - 90,
                sr.scaledHeight / 2f - 45,
                -1
            )
            FPSMaster.fontManager.s18.drawString(
                msg,
                sr.scaledWidth / 2f - 90,
                sr.scaledHeight / 2f - 20,
                Color(60, 60, 60).rgb
            )
        } else {
            msgBoxAnimation.start(Color(0, 0, 0, 100), Color(0, 0, 0, 0), 0.6f, Type.EASE_IN_OUT_QUAD)
        }
//        GlStateManager.disableBlend()
//        GlStateManager.popMatrix()
    }

    override fun mouseClick(mouseX: Int, mouseY: Int, mouseButton: Int) {
        super.mouseClick(mouseX, mouseY, mouseButton)
        btn.mouseClick(mouseX.toFloat(), mouseY.toFloat(), mouseButton)
        btn2.mouseClick(mouseX.toFloat(), mouseY.toFloat(), mouseButton)
        username.mouseClicked(mouseX, mouseY, mouseButton)
        password.mouseClicked(mouseX, mouseY, mouseButton)
        val sr = ScaledResolution(Minecraft.getMinecraft())
        if (Render2DUtils.isHovered(
                sr.scaledWidth / 2f - 90,
                sr.scaledHeight / 2f + 15,
                100f,
                10f,
                mouseX,
                mouseY
            ) && mouseButton == 0
        ) {
            val url = "https://fpsmaster.top/register"
            if (Desktop.isDesktopSupported()) {
                val desktop = Desktop.getDesktop()
                try {
                    desktop.browse(URI(url))
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        if (msgbox && msgBoxAnimation.color.alpha > 50) msgbox = false
    }

    override fun keyTyped(typedChar: Char, keyCode: Int) {
        super.keyTyped(typedChar, keyCode)
        username.textboxKeyTyped(typedChar, keyCode)
        FPSMaster.configManager.configure["username"] = username.text
        password.textboxKeyTyped(typedChar, keyCode)
    }
}

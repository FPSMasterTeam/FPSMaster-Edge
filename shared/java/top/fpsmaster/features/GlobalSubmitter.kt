package top.fpsmaster.features

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.ScaledResolution
import org.lwjgl.input.Mouse
import top.fpsmaster.FPSMaster
import top.fpsmaster.event.EventDispatcher.registerListener
import top.fpsmaster.event.Subscribe
import top.fpsmaster.event.events.EventPacket
import top.fpsmaster.event.events.EventRender2D
import top.fpsmaster.event.events.EventSendChatMessage
import top.fpsmaster.event.events.EventTick
import top.fpsmaster.features.impl.utility.Translator
import top.fpsmaster.interfaces.ProviderManager
import top.fpsmaster.modules.music.MusicPlayer
import top.fpsmaster.ui.notification.NotificationManager
import top.fpsmaster.ui.screens.mainmenu.MainMenu
import top.fpsmaster.utils.Utility
import top.fpsmaster.utils.math.MathTimer
import top.fpsmaster.utils.thirdparty.openai.OpenAi
import top.fpsmaster.utils.thirdparty.openai.requestClientAI
import top.fpsmaster.utils.render.shader.BlurBuffer
import top.fpsmaster.wrapper.TextFormattingProvider

class GlobalSubmitter {


    var time = MathTimer()
    private var musicSwitchTimer = MathTimer()

    fun init() {
        registerListener(this)
    }

    @Subscribe
    fun onChat(e: EventPacket) {
        if (Translator.using) {
            if (ProviderManager.packetChat.isPacket(e.packet)) {
                ProviderManager.packetChat.appendTranslation(e.packet)
            }
        }
    }

    @Subscribe
    fun onChatSend(e: EventSendChatMessage) {
        val msg = e.msg
        if (Translator.using) {
            if (msg.startsWith("#TRANSLATE")) {
                val message = msg.replace("#TRANSLATE", "")
                NotificationManager.addNotification(FPSMaster.i18n["translator"], message, 2f)
                FPSMaster.async.runnable {
                    val msgs = JsonArray()
                    val msgJson = JsonObject()
                    msgJson.addProperty("role", "user")
                    msgJson.addProperty("content", message)
                    msgs.add(msgJson)
                    val result = requestClientAI(
                        "You are a translator now, You need to translate following sentence to Chinese, you may use concepts in Minecraft",
                        "gpt-3.5-turbo",
                        msgs
                    )
                    if (result[0] == "200") {
                        Utility.sendClientMessage("${TextFormattingProvider.getGray()} ${result[1]}")
                    } else {
                        Utility.sendClientMessage("${TextFormattingProvider.getRed()} ${result[0]}: ${result[1]}")
                    }
                }
                e.cancel()
                return
            }
            if (msg.startsWith("#t ")) {
                val lang = msg.substring(0, msg.indexOf(" ")).replace("#", "")
                val message = msg.substring(msg.indexOf(" "), msg.length)
                NotificationManager.addNotification(FPSMaster.i18n["translator"], "$lang: $message", 2f)
                FPSMaster.async.runnable {
                    val openAi = OpenAi(
                        "https://one.aiskt.com/v1",
                        "sk-CwOF5Xs25FbSWPkK361b3574382346Ee9069Ae978b26Ee38",
                        "gpt-3.5-turbo-0125",
                        "You are a translator now, You need to translate following sentence to $lang. Please translate to $lang, Just answer the translation, dont add any extra things!"
                    )
                    val requestNewAnswer = openAi.requestNewAnswer(message)
                    if (ProviderManager.mcProvider.getPlayer() == null)
                        return@runnable
                    ProviderManager.mcProvider.getPlayer()?.sendChatMessage(requestNewAnswer.trim())
                }
                e.cancel()
            }
        }
    }

    @Subscribe
    fun onTick(e: EventTick) {
        if (musicSwitchTimer.delay(1000)) {
            if (MusicPlayer.isPlaying && MusicPlayer.getPlayProgress() > 0.999) {
                MusicPlayer.playList.next()
            }
        }
    }

    @Subscribe
    fun onRender(e: EventRender2D) {
        if (BlurBuffer.blurEnabled())
            BlurBuffer.updateBlurBuffer(true)

        val scaledResolution = ScaledResolution(Minecraft.getMinecraft())
        val mouseX = Mouse.getX().toFloat() / scaledResolution.scaleFactor
        val mouseY = scaledResolution.scaledHeight - Mouse.getY().toFloat() / scaledResolution.scaleFactor
        FPSMaster.componentsManager.draw(mouseX.toInt(), mouseY.toInt())
        NotificationManager.drawNotifications()
    }
}

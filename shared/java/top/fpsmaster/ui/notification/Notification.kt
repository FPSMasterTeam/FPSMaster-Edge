package top.fpsmaster.ui.notification

import net.minecraft.util.ResourceLocation
import top.fpsmaster.FPSMaster
import top.fpsmaster.utils.math.animation.Animation
import top.fpsmaster.utils.math.animation.AnimationUtils
import top.fpsmaster.utils.render.Render2DUtils
import java.awt.Color
import kotlin.math.max
import kotlin.math.min

class Notification(val title: String, val desc: String, val type: Type, val time: Float) {

    var anim: Animation = Animation()
    var y = 0f
    var width = 0f
    var height = 0f
    private var startTime = -1L

    init {
        anim.start(100.0, 0.0, 0.3f, top.fpsmaster.utils.math.animation.Type.EASE_IN_OUT_QUAD)
        val s16 = FPSMaster.fontManager.s16
        width = 30f + max(s16.getStringWidth(title), s16.getStringWidth(desc))
        height = 30f
    }

    fun draw(x: Float, y: Float) {
        // Draw the notification
        anim.update()
        if (startTime == -1L) {
            startTime = System.currentTimeMillis()
        }
        if (anim.value == 0.0) {
            if (System.currentTimeMillis() - startTime > time * 1000) {
                anim.start(0.0, 100.0, 0.3f, top.fpsmaster.utils.math.animation.Type.EASE_IN_OUT_QUAD)
            }
        }
        this.y = AnimationUtils.base(this.y.toDouble(), y.toDouble(), 0.2).toFloat()
        Render2DUtils.drawOptimizedRoundedRect(
            x - (width * anim.value / 100f).toFloat(),
            this.y,
            width,
            height,
            Color(0, 0, 0, 100)
        )
        Render2DUtils.drawOptimizedRoundedRect(
            x - (width * anim.value / 100f).toFloat(),
            this.y,
            width * (min(1f, (System.currentTimeMillis() - startTime) / 1000f / time)),
            height,
            Color(255, 255, 255, 100)
        )
        Render2DUtils.drawImage(
            ResourceLocation("client/textures/noti/" + type.name.lowercase() + ".png"),
            x - (width * anim.value / 100f).toFloat() + 4,
            this.y + 8,
            14f,
            14f,
            -1
        )
        FPSMaster.fontManager.s18.drawStringWithShadow(
            title,
            x - (width * anim.value / 100f).toFloat() + 20,
            this.y + 4,
            -1
        )
        FPSMaster.fontManager.s16.drawString(
            desc,
            x - (width * anim.value / 100f).toFloat() + 20,
            this.y + 15,
            Color(200, 200, 200).rgb
        )


    }
}

enum class Type {
    INFO,
    ERROR,
    WARNING
}
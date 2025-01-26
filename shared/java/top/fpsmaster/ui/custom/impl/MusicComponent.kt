package top.fpsmaster.ui.custom.impl

import net.minecraft.util.ResourceLocation
import top.fpsmaster.FPSMaster
import top.fpsmaster.features.impl.interfaces.MusicOverlay
import top.fpsmaster.modules.music.MusicPlayer
import top.fpsmaster.modules.music.MusicPlayer.playProgress
import top.fpsmaster.modules.music.netease.Music
import top.fpsmaster.ui.custom.Component
import top.fpsmaster.ui.custom.Position
import top.fpsmaster.utils.math.animation.AnimationUtils.base
import top.fpsmaster.utils.render.Render2DUtils
import kotlin.math.max

class MusicComponent : Component(MusicOverlay::class.java) {
    init {
        x = 0.01f
        y = 0.01f
        position = Position.RT

    }

    private fun drawSong(x: Float, y: Float, width: Float, height: Float) {
        val current = MusicPlayer.playList.getCurrent() as Music?
        drawRect(x, y, width, height, mod.backgroundColor.color)
        drawRect(x, y, songProgress, height, MusicOverlay.progressColor.color)
        songProgress = base(songProgress.toDouble(), (6 + (width - 6) * playProgress).toDouble(), 0.1).toFloat()
        Render2DUtils.drawImage(
            ResourceLocation("music/netease/" + current!!.id),
            x + 5,
            y + 5,
            height - 10,
            height - 10,
            -1
        )
        val s18 = FPSMaster.fontManager.s18
        val s16 = FPSMaster.fontManager.s16
        drawString(18, current.name, x + 40, y + 6, FPSMaster.theme.textColorTitle.rgb)
        drawString(16, current.author, x + 40, y + 18, FPSMaster.theme.textColorDescription.rgb)
    }

    override fun draw(x: Float, y: Float) {
        super.draw(x, y)
        if (!MusicPlayer.playList.musics.isEmpty()) {
            val width: Float = max(
                getStringWidth(18, MusicPlayer.playList.getCurrent()!!.name), getStringWidth(18,
                    MusicPlayer.playList.getCurrent()!!.author
                ).toFloat()
            )
            drawSong(x, y, width + 60, 40f)
            this.width = width + 60
            height = 40f
        }
    }

    companion object {
        private var songProgress = 0f
    }
}

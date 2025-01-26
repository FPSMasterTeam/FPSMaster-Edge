package top.fpsmaster.ui.custom.impl

import top.fpsmaster.FPSMaster
import top.fpsmaster.font.impl.UFontRenderer
import top.fpsmaster.features.impl.interfaces.LyricsDisplay
import top.fpsmaster.modules.music.JLayerHelper
import top.fpsmaster.modules.music.JLayerHelper.progress
import top.fpsmaster.modules.music.Line
import top.fpsmaster.modules.music.MusicPlayer
import top.fpsmaster.ui.custom.Component
import top.fpsmaster.ui.custom.Position
import top.fpsmaster.utils.math.animation.AnimationUtils.base
import top.fpsmaster.utils.render.Render2DUtils
import kotlin.math.abs

class LyricsComponent : Component(LyricsDisplay::class.java) {
    private var duration: Long = 0

    init {
        x = 0.5f
        y = 0.2f
        position = Position.CT
    }

    private fun fromTimeTick(timeTick: String?): Long {
        return timeTick!!.split(":".toRegex()).dropLastWhile { it.isEmpty() }
            .toTypedArray()[0].toLong() * 60 * 1000 + timeTick.split(":".toRegex()).dropLastWhile { it.isEmpty() }
            .toTypedArray()[1].split("\\.".toRegex())
            .dropLastWhile { it.isEmpty() }.toTypedArray()[0].toLong() * 1000 + timeTick.split(":".toRegex())
            .dropLastWhile { it.isEmpty() }
            .toTypedArray()[1].split("\\.".toRegex())
            .dropLastWhile { it.isEmpty() }.toTypedArray()[1].toLong()
    }

    override fun draw(x: Float, y: Float) {
        var y = y
        super.draw(x, y)
        width = 200f
        height = 70f
        drawRect(x, y, width, height, mod.backgroundColor.color)
        y += 10
        if (MusicPlayer.playList.getCurrent() != null) {
            val current = MusicPlayer.playList.getCurrent()
            if (current!!.lyrics == null) return
            var curLine = -1
            for (i in current.lyrics!!.lines.indices) {
                val line = current.lyrics!!.lines[i]
                if (MusicPlayer.isPlaying && JLayerHelper.clip != null) duration =
                    (JLayerHelper.duration * 60 * 1000 * progress).toLong()
                var time = line.time
                var nextTime = line.time + line.duration
                if (i < current.lyrics!!.lines.size - 1) {
                    nextTime = current.lyrics!!.lines[i + 1].time
                }
                if (line.type == 1) {
                    time = fromTimeTick(line.timeTick)
                    if (i < current.lyrics!!.lines.size - 1) {
                        nextTime = fromTimeTick(current.lyrics!!.lines[i + 1].timeTick)
                    }
                }
                if (duration in (time + 1)..<nextTime) {
                    curLine = i
                } else if (duration > time) {
                    curLine = i
                }
            }
            if (curLine != -1) {
                for (i in curLine - 2..curLine + 2) {
                    if (current.lyrics!!.lines.size > i && i >= 0) {
                        val line = current.lyrics!!.lines[i]
                        val content = line.content
                        val xOffset = x + (width - getStringWidth(20, content)) / 2
                        if (i == curLine) {
                            line.animation = base(line.animation.toDouble(), 0.0, 0.1).toFloat()
                            line.alpha = base(line.alpha.toDouble(), 1.0, 0.1).toFloat()
                        } else {
                            line.animation = base(line.animation.toDouble(), (i - curLine).toDouble(), 0.1).toFloat()
                            if (abs((i - curLine).toDouble()).toInt() == 1) {
                                line.alpha = base(line.alpha.toDouble(), 1.0, 0.1).toFloat()
                            } else {
                                line.alpha = base(line.alpha.toDouble(), 0.0, 0.1).toFloat()
                            }
                        }
                        if (abs((i - curLine).toDouble()) <= 1) {
//                            drawString(lfont,line.getContent(), xOffset, y + line.animation * 20 + 20,-1);
                            drawLine(
                                line,
                                xOffset,
                                y + line.animation * 20 + 20,
                                20,
                                i == curLine
                            )
                        }
                    }
                }
            }
        }

//        GL11.glDisable(GL11.GL_SCISSOR_TEST);
//        GL11.glPopMatrix();
    }

    private fun drawLine(line: Line, xOffset: Float, y: Float, lfont: Int, current: Boolean) {
        var xOffset = xOffset
        for (word in line.words) {
            xOffset += if (current) {
                if (duration >= word.time) {
                    // animation percent
                    val animation = 0.3f + (duration - word.time) / word.duration.toFloat()
                    val animation2 =(duration - word.time) / word.duration.toFloat()

                    drawString(
                        lfont,
                        word.content,
                        xOffset,
                        y + 7 - (animation2.coerceIn(0f,1f) * 3),
                        Render2DUtils.reAlpha(
                            LyricsDisplay.textColor.color,
                            Render2DUtils.limit((animation * 255).toDouble())
                        ).rgb
                    )
                    getStringWidth(lfont, word.content)
                } else {
                    drawString(
                        lfont,
                        word.content,
                        xOffset,
                        y + 7,
                        Render2DUtils.reAlpha(LyricsDisplay.textBG.color, (0.3 * 255).toInt()).rgb
                    )
                    getStringWidth(lfont, word.content)
                }
            } else {
                drawString(
                    lfont,
                    word.content,
                    xOffset,
                    y + 5,
                    Render2DUtils.reAlpha(
                        LyricsDisplay.textBG.color,
                        Render2DUtils.limit((line.alpha * 120).toDouble())
                    ).rgb
                )
                getStringWidth(lfont, word.content)
            }
        }
    }
}

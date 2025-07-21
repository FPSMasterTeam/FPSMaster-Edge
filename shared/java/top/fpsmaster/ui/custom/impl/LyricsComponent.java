package top.fpsmaster.ui.custom.impl;

import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.opengl.GL11;
import top.fpsmaster.features.impl.interfaces.LyricsDisplay;
import top.fpsmaster.modules.music.*;
import top.fpsmaster.ui.click.music.NewMusicPanel;
import top.fpsmaster.ui.custom.Component;
import top.fpsmaster.ui.custom.Position;
import top.fpsmaster.utils.math.animation.Animation;
import top.fpsmaster.utils.math.animation.AnimationUtils;
import top.fpsmaster.utils.math.animation.Type;
import top.fpsmaster.utils.render.Render2DUtils;

import java.util.List;

public class LyricsComponent extends Component {

    private long duration = 0;
    public LyricsComponent() {
        super(LyricsDisplay.class);
        x = 0.5f;
        y = 0.2f;
        width = 200f;
        height = 70f;
        position = Position.CT;
    }

    private long fromTimeTick(String timeTick) {
        String[] parts = timeTick.split(":");
        long minutes = Long.parseLong(parts[0]) * 60 * 1000;
        String[] subParts = parts[1].split("\\.");
        long seconds = Long.parseLong(subParts[0]) * 1000;
        long milliseconds = Long.parseLong(subParts[1]);
        return minutes + seconds + milliseconds;
    }

    @Override
    public void draw(float x, float y) {
        super.draw(x, y);
        drawRect(x, y, width, height, mod.backgroundColor.getColor());
        if (((LyricsDisplay) mod).scale.getValue()) {
            y += 10;
        }else{
            y += 5;
        }
        AbstractMusic current = NewMusicPanel.playing;
        if (current != null && current.lyrics != null) {
            int curLine = -1;
            List<Line> lines = current.lyrics.lines;
            for (int i = 0; i < lines.size(); i++) {
                Line line = lines.get(i);
                line.finished = false;
                if (MusicPlayer.isPlaying && JLayerHelper.clip != null) {
                    duration = (long) (JLayerHelper.getDuration() * 60 * 1000 * JLayerHelper.getProgress());
                }
                long time = line.time;
                long nextTime = line.time + line.duration;
                if (i < lines.size() - 1) {
                    nextTime = lines.get(i + 1).time;
                }
                if (line.type == 1) {
                    time = fromTimeTick(line.timeTick);
                    if (i < lines.size() - 1) {
                        nextTime = fromTimeTick(lines.get(i + 1).timeTick);
                    }
                }
                if ((duration >= time && duration < nextTime) || duration > time) {
                    curLine = i;
                    //get previous line and set finished
                    if(i != 0) lines.get(i - 1).finished = true;
                }
            }

            if (curLine != -1) {
                for (int j = curLine - 3; j <= curLine + 2; j++) {
                    if (j >= 0 && j < lines.size()) {
                        Line line = lines.get(j);
                        String content = line.getContent();
                        float stringWidth = getStringWidth(20, content);
                        float xOffset = x + (width - stringWidth) / 2;
                        if (this.width < stringWidth + 10) {
                            this.width = stringWidth + 10;
                        }
                        if (j == curLine) {
                            line.animation = (float) AnimationUtils.base(line.animation, 0.0, 0.1f);
                            line.alpha = (float) AnimationUtils.base(line.alpha, 1.0, 0.1f);
                        } else {
                            line.animation = (float) AnimationUtils.base(line.animation, j - curLine, 0.1f);
                            line.alpha = (float) (Math.abs(j - curLine) == 1 ?
                                    AnimationUtils.base(line.alpha, 1.0, 0.1f) :
                                    AnimationUtils.base(line.alpha, 0.0, 0.1f));
                        }
                        if (Math.abs(j - curLine) <= 1) {
                            drawLine(line, xOffset, y + line.animation * 20 + 20, 20,j == curLine);
                        }
                    }
                }
            }
        }
    }

    private void drawLine(Line line, float xOffset, float y, int font, boolean current) {
        LyricsDisplay lyrics = (LyricsDisplay) mod;
        //lyric line has been play finished or is playing
        if (lyrics.scale.getValue()) {
            //default scale ratio
            float scaleRatio = 1.0f;
            if(current) {
                line.scaleAnimation.start(1.0,1.3,0.3f,Type.LINEAR);
            }else{
                line.scaleAnimation.start(line.scaleAnimation.value,1.0,0.3f,Type.LINEAR);
            }
            line.scaleAnimation.update();
            scaleRatio = (float) line.scaleAnimation.value;
            Render2DUtils.scaleStart(xOffset + (getStringWidth(20, line.getContent()) / 2.0f), y + (getStringHeight(20) / 2.0f), scaleRatio);
            GL11.glTranslated(0, -8, 0);
        }
        for (Word word : line.words) {
            xOffset += current ? drawWord(word, xOffset, y, line) : drawWordBG(word, xOffset, y, line);
        }
        if (lyrics.scale.getValue()) {
            GL11.glTranslated(0, 8, 0);
            Render2DUtils.scaleEnd();
        }
    }


    private float drawWord(Word word, float xOffset, float y, Line line) {
        if (duration >= word.time) {
            float animation = 0.3f + (float) (duration - word.time) / word.duration;
            float animation2 = (float) (duration - word.time) / word.duration;
            drawString(20, word.content, xOffset, y + 7 - Math.min(animation2, 1f),
                    Render2DUtils.reAlpha(LyricsDisplay.textColor.getColor(), (int) Math.min(animation * 255, 255)).getRGB());
        }else {
            drawString(20, word.content, xOffset, y + 7,
                    Render2DUtils.reAlpha(LyricsDisplay.textColor.getColor(), (int) Math.min(line.alpha * 120, 255)).getRGB());
        }
        return getStringWidth(20, word.content);
    }

    private float drawWordBG(Word word, float xOffset, float y, Line line) {
        drawString(20, word.content, xOffset, y + 5,
                Render2DUtils.reAlpha(LyricsDisplay.textBG.getColor(), (int) Math.min(line.alpha * 120, 255)).getRGB());
        return getStringWidth(20, word.content);
    }
}

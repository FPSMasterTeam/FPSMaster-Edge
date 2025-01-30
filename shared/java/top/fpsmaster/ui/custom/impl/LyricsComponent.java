package top.fpsmaster.ui.custom.impl;

import org.jetbrains.annotations.Nullable;
import top.fpsmaster.FPSMaster;
import top.fpsmaster.font.impl.UFontRenderer;
import top.fpsmaster.features.impl.interfaces.LyricsDisplay;
import top.fpsmaster.modules.music.*;
import top.fpsmaster.ui.custom.Component;
import top.fpsmaster.ui.custom.Position;
import top.fpsmaster.utils.math.animation.AnimationUtils;
import top.fpsmaster.utils.render.Render2DUtils;

import java.awt.*;
import java.util.List;

public class LyricsComponent extends Component {

    private long duration = 0;

    public LyricsComponent() {
        super(LyricsDisplay.class);
        x = 0.5f;
        y = 0.2f;
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
        width = 200f;
        height = 70f;
        drawRect(x, y, width, height, mod.backgroundColor.getColor());
        y += 10;
        AbstractMusic current = MusicPlayer.playList.current();
        if (current != null && current.lyrics != null) {
            int curLine = -1;
            List<Line> lines = current.lyrics.lines;
            for (int i = 0; i < lines.size(); i++) {
                Line line = lines.get(i);
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
                }
            }

            if (curLine != -1) {
                for (int i = curLine - 2; i <= curLine + 2; i++) {
                    if (i >= 0 && i < lines.size()) {
                        Line line = lines.get(i);
                        String content = line.getContent();
                        float xOffset = x + (width - getStringWidth(20, content)) / 2;
                        if (i == curLine) {
                            line.animation = (float) AnimationUtils.base(line.animation, 0.0, 0.1f);
                            line.alpha = (float) AnimationUtils.base(line.alpha, 1.0, 0.1f);
                        } else {
                            line.animation = (float) AnimationUtils.base(line.animation, i - curLine, 0.1f);
                            line.alpha = (float) (Math.abs(i - curLine) == 1 ?
                                    AnimationUtils.base(line.alpha, 1.0, 0.1f) :
                                    AnimationUtils.base(line.alpha, 0.0, 0.1f));
                        }
                        if (Math.abs(i - curLine) <= 1) {
                            drawLine(line, xOffset, y + line.animation * 20 + 20, 20, i == curLine);
                        }
                    }
                }
            }
        }
    }

    private void drawLine(Line line, float xOffset, float y, int lfont, boolean current) {
        for (Word word : line.words) {
            xOffset += current ? drawWord(word, xOffset, y, line) : drawWordBG(word, xOffset, y, line);
        }
    }

    private float drawWord(Word word, float xOffset, float y, Line line) {
        if (duration >= word.time) {
            float animation = 0.3f + (float) (duration - word.time) / word.duration;
            float animation2 = (float) (duration - word.time) / word.duration;
            drawString(20, word.content, xOffset, y + 7 - Math.min(animation2, 1f) * 3,
                    Render2DUtils.reAlpha(LyricsDisplay.textColor.getColor(), (int) Math.min(animation * 255, 255)).getRGB());
            return getStringWidth(20, word.content);
        }
        return 0;
    }

    private float drawWordBG(Word word, float xOffset, float y, Line line) {
        drawString(20, word.content, xOffset, y + 5,
                Render2DUtils.reAlpha(LyricsDisplay.textBG.getColor(), (int) Math.min(line.alpha * 120, 255)).getRGB());
        return getStringWidth(20, word.content);
    }
}

package top.fpsmaster.modules.music;

import top.fpsmaster.utils.math.animation.Animation;

import java.util.ArrayList;

public class Line {
    public ArrayList<Word> words = new ArrayList<>();
    public int type = 0;
    public long time = 0;
    public float alpha = 0f;
    public long duration = 0;
    public float animation = 0f;
    public boolean finished = false;
    public String timeTick = null;
    public Animation scaleAnimation = new Animation();

    public void addWord(Word word) {
        words.add(word);
    }

    public String getContent() {
        StringBuilder stringBuilder = new StringBuilder();
        for (Word word : words) {
            stringBuilder.append(word.content);
        }
        return stringBuilder.toString();
    }

}

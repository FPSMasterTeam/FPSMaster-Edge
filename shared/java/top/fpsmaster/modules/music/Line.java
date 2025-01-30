package top.fpsmaster.modules.music;

import java.util.ArrayList;

public class Line {
    public ArrayList<Word> words = new ArrayList<>();
    public long time = 0;
    public long duration = 0;
    public String timeTick = null;
    public int type = 0;
    public float animation = 0f;
    public float alpha = 0f;

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

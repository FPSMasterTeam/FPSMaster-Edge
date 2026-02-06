package top.fpsmaster.modules.music;

import java.util.ArrayList;

public class Lyrics {
    public ArrayList<Line> lines = new ArrayList<>();

    public void addLine(Line line) {
        lines.add(line);
    }

    public ArrayList<Line> getLines() {
        return lines;
    }

    public void setLines(ArrayList<Line> lines) {
        this.lines = lines;
    }
}




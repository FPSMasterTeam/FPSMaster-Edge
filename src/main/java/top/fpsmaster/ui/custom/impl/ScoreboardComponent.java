package top.fpsmaster.ui.custom.impl;

import top.fpsmaster.features.impl.interfaces.Scoreboard;
import top.fpsmaster.ui.custom.Component;
import net.minecraft.client.Minecraft;
import net.minecraft.scoreboard.Score;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.ScorePlayerTeam;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ScoreboardComponent extends Component {

    /** Matches the vanilla font metrics this HUD was originally laid out against. */
    private static final int FONT_SIZE = 16;

    public ScoreboardComponent() {
        super(Scoreboard.class);
        allowScale = true;
    }

    @Override
    public void draw(float x, float y) {
        super.draw(x, y);
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.theWorld == null) {
            width = 0;
            height = 0;
            return;
        }
        net.minecraft.scoreboard.Scoreboard mcScoreboard = mc.theWorld.getScoreboard();
        ScoreObjective objective = mcScoreboard.getObjectiveInDisplaySlot(1);
        if (objective == null) {
            width = 0;
            height = 0;
            return;
        }

        Collection<Score> scores = mcScoreboard.getSortedScores(objective);
        List<Score> filtered = new ArrayList<>();
        for (Score score : scores) {
            if (score.getPlayerName() != null && !score.getPlayerName().startsWith("#")) {
                filtered.add(score);
            }
        }
        if (filtered.size() > 15) {
            filtered = filtered.subList(filtered.size() - 15, filtered.size());
        }

        // Measure through the base class so BetterFont is honoured; these are logical widths, and
        // drawRect/drawString apply scale themselves.
        String title = objective.getDisplayName();
        float maxWidth = getStringWidth(FONT_SIZE, title);
        boolean showScore = Scoreboard.score.getValue();
        List<String> lines = new ArrayList<>();
        for (Score score : filtered) {
            String name = ScorePlayerTeam.formatPlayerName(mcScoreboard.getPlayersTeam(score.getPlayerName()), score.getPlayerName());
            String line = showScore ? name + ": " + score.getScorePoints() : name;
            lines.add(line);
            maxWidth = Math.max(maxWidth, getStringWidth(FONT_SIZE, line));
        }

        float lineHeight = mc.fontRendererObj.FONT_HEIGHT + 1;
        width = maxWidth + 6;
        height = (lines.size() + 1) * lineHeight + 4;
        drawRect(x, y, width, height, mod.backgroundColor.getColor());
        drawString(FONT_SIZE, title, x + 3 * scale, y + 2 * scale, 0xFFFFFF);
        // Row offsets are positions, so they scale here.
        float offsetY = y + (2 + lineHeight) * scale;
        for (String line : lines) {
            drawString(FONT_SIZE, line, x + 3 * scale, offsetY, 0xFFFFFF);
            offsetY += lineHeight * scale;
        }
    }
}





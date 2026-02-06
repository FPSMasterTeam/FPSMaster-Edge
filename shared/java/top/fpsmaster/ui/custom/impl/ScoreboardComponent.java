package top.fpsmaster.ui.custom.impl;

import top.fpsmaster.features.impl.interfaces.Scoreboard;
import top.fpsmaster.ui.custom.Component;
import net.minecraft.client.Minecraft;
import net.minecraft.scoreboard.Score;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.scoreboard.Scoreboard;
import top.fpsmaster.utils.render.Render2DUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ScoreboardComponent extends Component {

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
        Scoreboard scoreboard = mc.theWorld.getScoreboard();
        ScoreObjective objective = scoreboard.getObjectiveInDisplaySlot(1);
        if (objective == null) {
            width = 0;
            height = 0;
            return;
        }

        Collection<Score> scores = scoreboard.getSortedScores(objective);
        List<Score> filtered = new ArrayList<>();
        for (Score score : scores) {
            if (score.getPlayerName() != null && !score.getPlayerName().startsWith("#")) {
                filtered.add(score);
            }
        }
        if (filtered.size() > 15) {
            filtered = filtered.subList(filtered.size() - 15, filtered.size());
        }

        int maxWidth = mc.fontRendererObj.getStringWidth(objective.getDisplayName());
        List<String> lines = new ArrayList<>();
        for (Score score : filtered) {
            String name = ScorePlayerTeam.formatPlayerName(scoreboard.getPlayersTeam(score.getPlayerName()), score.getPlayerName());
            String line = name + ": " + score.getScorePoints();
            lines.add(line);
            maxWidth = Math.max(maxWidth, mc.fontRendererObj.getStringWidth(line));
        }

        int lineHeight = mc.fontRendererObj.FONT_HEIGHT + 1;
        width = maxWidth + 6;
        height = (lines.size() + 1) * lineHeight + 4;
        Render2DUtils.drawRect(x, y, width, height, mod.backgroundColor.getColor());
        mc.fontRendererObj.drawStringWithShadow(objective.getDisplayName(), x + 3, y + 2, 0xFFFFFF);
        float offsetY = y + 2 + lineHeight;
        for (String line : lines) {
            mc.fontRendererObj.drawStringWithShadow(line, x + 3, offsetY, 0xFFFFFF);
            offsetY += lineHeight;
        }
    }
}

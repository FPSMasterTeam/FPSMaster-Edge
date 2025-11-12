package top.fpsmaster.wrapper.mods;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import net.minecraft.scoreboard.Score;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.ScorePlayerTeam;
import top.fpsmaster.FPSMaster;
import top.fpsmaster.font.impl.UFontRenderer;
import top.fpsmaster.features.impl.InterfaceModule;
import top.fpsmaster.features.impl.interfaces.Scoreboard;
import top.fpsmaster.ui.custom.impl.ScoreboardComponent;
import top.fpsmaster.api.Wrappers;
import top.fpsmaster.wrapper.TextFormattingProvider;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class WrapperScoreboard {
    public static float[] render(ScoreboardComponent scoreboardComponent, InterfaceModule mod, float x, float y) {
        top.fpsmaster.wrapper.scoreboard.WrapperScoreboard scoreboard = new top.fpsmaster.wrapper.scoreboard.WrapperScoreboard(Wrappers.world().getWorld().getScoreboard());
        ScoreObjective scoreobjective = null;
        ScorePlayerTeam scoreplayerteam = scoreboard.getPlayersTeam(Wrappers.minecraft().getPlayer().getName());

        if (scoreplayerteam != null) {
            int i1 = scoreboard.getPlayersTeamColorIndex(Wrappers.minecraft().getPlayer().getName());

            if (i1 >= 0) {
                scoreobjective = scoreboard.getObjectiveInDisplaySlot(3 + i1);
            }
        }

        ScoreObjective objective = scoreobjective != null ? scoreobjective : scoreboard.getObjectiveInDisplaySlot(1);

        if (objective != null) {
            Collection<Score> collection = scoreboard.getSortedScores(objective);

            List<Score> list = collection.stream().filter(score -> !score.getPlayerName().startsWith("#")).collect(Collectors.toList());

            if (list.size() > 15) {
                collection = Lists.newArrayList(Iterables.skip(list, collection.size() - 15));
            } else {
                collection = list;
            }

            int i = (int) scoreboardComponent.getStringWidth(16, objective.getDisplayName());


            for (Score score : collection) {
                ScorePlayerTeam scoreteam = scoreboard.getPlayersTeam(score.getPlayerName());
                String s = filterHypixelIllegalCharacters(ScorePlayerTeam.formatPlayerName(scoreteam, score.getPlayerName()) + ": " + TextFormattingProvider.getRed() + score.getScorePoints());
                i = (int) Math.max(i, scoreboardComponent.getStringWidth(16, s));
            }
            i += 6;

            int height1 = (int) scoreboardComponent.getStringHeight(16) + 2;
            int j = 0;
            float h = collection.size() * height1 + 10;
            scoreboardComponent.drawRect(x, y, i, h, mod.backgroundColor.getColor());

            for (Score score1 : collection) {
                ++j;
                ScorePlayerTeam scoreplayerteam1 = scoreboard.getPlayersTeam(score1.getPlayerName());
                String s1 = filterHypixelIllegalCharacters(ScorePlayerTeam.formatPlayerName(scoreplayerteam1, score1.getPlayerName()));
                float k = j * height1;

                // title
                if (j == collection.size()) {
                    String s3 = objective.getDisplayName();
                    scoreboardComponent.drawRect(x, y, i, height1 + 1, mod.backgroundColor.getColor());
                    scoreboardComponent.drawString(16, s3, (int) (x + 2 + ((float) i / 2 - scoreboardComponent.getStringWidth(16, s3) / 2f) * scoreboardComponent.scale), y, -1);
                }
                scoreboardComponent.drawString(16, s1, ((int) x) + 2, (int) (y + (h - k) * scoreboardComponent.scale), -1);
                // 红字
                if (Scoreboard.score.getValue()) {
                    String s2 = TextFormattingProvider.getRed() + String.valueOf(score1.getScorePoints());
                    scoreboardComponent.drawString(16, s2, x + (i - 2 - scoreboardComponent.getStringWidth(16, s2)) * scoreboardComponent.scale, y + k * scoreboardComponent.scale, -1);
                }
            }
            return new float[]{i, h};
        }
        return new float[]{100, 120};
    }


    public static String filterHypixelIllegalCharacters(String text) {
        boolean dangerous = false;
        StringBuilder stringBuilder = new StringBuilder();
        for (char c : text.toCharArray()) {
            if (c == '\ud83c' || c == '\ud83d') {
                dangerous = true;
                continue;
            }
            if (dangerous) {
                dangerous = false;
                continue;
            }
            if (c == '⚽') continue;
            stringBuilder.append(c);
        }
        return stringBuilder.toString();
    }
}

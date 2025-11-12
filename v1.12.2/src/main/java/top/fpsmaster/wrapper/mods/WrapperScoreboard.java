package top.fpsmaster.wrapper.mods;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import net.minecraft.scoreboard.Score;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.ScorePlayerTeam;
import top.fpsmaster.FPSMaster;
import top.fpsmaster.features.impl.InterfaceModule;
import top.fpsmaster.font.impl.UFontRenderer;
import top.fpsmaster.features.impl.interfaces.Scoreboard;
import top.fpsmaster.api.ProviderManager;
import top.fpsmaster.ui.custom.impl.ScoreboardComponent;
import top.fpsmaster.utils.render.Render2DUtils;
import top.fpsmaster.wrapper.TextFormattingProvider;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class WrapperScoreboard {
    public static float[] render(ScoreboardComponent scoreboardComponent, InterfaceModule mod, float x, float y) {
        top.fpsmaster.wrapper.scoreboard.WrapperScoreboard scoreboard = new top.fpsmaster.wrapper.scoreboard.WrapperScoreboard(ProviderManager.worldClientProvider.getWorld().getScoreboard());
        ScoreObjective scoreobjective = null;
        ScorePlayerTeam scoreplayerteam = scoreboard.getPlayersTeam(ProviderManager.mcProvider.getPlayer().getName());

        assert FPSMaster.fontManager != null;
        UFontRenderer s16 = FPSMaster.fontManager.s16;


        if (scoreplayerteam != null) {

            int i1 = scoreboard.getPlayersTeamColorIndex(ProviderManager.mcProvider.getPlayer().getName());

            if (i1 >= 0) {
                scoreobjective = scoreboard.getObjectiveInDisplaySlot(3 + i1);
            }

        }
        ScoreObjective objective = scoreobjective != null ? scoreobjective : scoreboard.getObjectiveInDisplaySlot(1);

        if (objective != null) {
            Collection<Score> collection = scoreboard.getSortedScores(objective);

            List<Score> list = collection.stream().filter(p_apply_1_ -> !p_apply_1_.getPlayerName().startsWith("#")).collect(Collectors.toList());

            if (list.size() > 15) {
                collection = Lists.newArrayList(Iterables.skip(list, collection.size() - 15));
            } else {
                collection = list;
            }

            int i;
            if (mod.betterFont.getValue()) {
                i = s16.getStringWidth(objective.getDisplayName());
            } else {
                i = ProviderManager.mcProvider.getFontRenderer().getStringWidth(objective.getDisplayName());
            }
            for (Score score : collection) {
                ScorePlayerTeam scoreteam = scoreboard.getPlayersTeam(score.getPlayerName());
                String s = ScorePlayerTeam.formatPlayerName(scoreteam, score.getPlayerName()) + ": " + TextFormattingProvider.getRed() + score.getScorePoints();
                if (mod.betterFont.getValue()) {
                    i = Math.max(i, s16.getStringWidth(s));
                } else {
                    i = Math.max(i, ProviderManager.mcProvider.getFontRenderer().getStringWidth(s));
                }
            }
            i += 6;

            int height1 = 10;
            float l1 = x;
            int j = 0;
            float h = collection.size() * height1 + 10;
            scoreboardComponent.drawRect(l1, y, i, h, mod.backgroundColor.getColor());

            for (Score score1 : collection) {
                ++j;
                ScorePlayerTeam scoreplayerteam1 = scoreboard.getPlayersTeam(score1.getPlayerName());
                String s1 = ScorePlayerTeam.formatPlayerName(scoreplayerteam1, score1.getPlayerName());
                float k = j * height1;

                // title
                if (j == collection.size()) {
                    String s3 = objective.getDisplayName();
                    if (mod.bg.getValue()) {
                        scoreboardComponent.drawRect(l1, y, i, height1 + 1, mod.backgroundColor.getColor());
                    }
                    if (mod.betterFont.getValue()) {
                        scoreboardComponent.drawString(s16, s3, (int) (l1 + 2 + (float) i / 2 - s16.getStringWidth(s3) / 2f), y, -1);
                    } else {
                        ProviderManager.mcProvider.getFontRenderer().drawStringWithShadow(s3, (int) (l1 + 2 + (float) i / 2 - ProviderManager.mcProvider.getFontRenderer().getStringWidth(s3) / 2f), y, -1);
                    }
                }

                if (mod.betterFont.getValue()) {
                    scoreboardComponent.drawString(s16, s1, ((int) l1) + 2, (int) (y + h - k), -1);
                } else {
                    ProviderManager.mcProvider.getFontRenderer().drawStringWithShadow(s1, ((int) l1) + 2, (int) (y + h - k), -1);
                }

                // 红字
                if (Scoreboard.score.getValue()) {
                    String s2 = TextFormattingProvider.getRed() + String.valueOf(score1.getScorePoints());
                    if (mod.betterFont.getValue()) {
                        scoreboardComponent.drawString(s16, s2, l1 + i - 2 - s16.getStringWidth(s2), y + k, -1);
                    } else {
                        ProviderManager.mcProvider.getFontRenderer().drawStringWithShadow(s2, l1 + i - 2 - ProviderManager.mcProvider.getFontRenderer().getStringWidth(s2), y + k, -1);
                    }
                }
            }
            return new float[]{i, h};
        }
        return new float[]{100, 140};
    }
}

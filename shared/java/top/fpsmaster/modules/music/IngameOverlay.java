package top.fpsmaster.modules.music;

import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.util.ResourceLocation;
import top.fpsmaster.FPSMaster;
import top.fpsmaster.features.impl.interfaces.MusicOverlay;
import top.fpsmaster.font.impl.UFontRenderer;
import top.fpsmaster.modules.music.netease.Music;
import top.fpsmaster.utils.Utility;
import top.fpsmaster.utils.math.animation.AnimationUtils;
import top.fpsmaster.utils.render.Render2DUtils;

import java.awt.*;

public class IngameOverlay {
    private static float songProgress = 0f;
    private static double[] smoothCurve = new double[0];

    public static void onRender() {
        if (MusicPlayer.isPlaying) {
            ScaledResolution sr = new ScaledResolution(Utility.mc);
            double[] curve = MusicPlayer.getCurve();
            if (curve.length != 0) {
                int numBars = 60;           // 你希望显示的频谱柱条数
                if (smoothCurve.length != numBars) {
                    smoothCurve = new double[numBars];
                }

                float width = (float) sr.getScaledWidth() / numBars;

                float screenWidth = sr.getScaledWidth();
                float screenHeight = sr.getScaledHeight();

                int binsPerBar = curve.length / numBars;

                for (int bar = 0; bar < numBars; bar++) {
                    double sum = 0.0;
                    for (int j = 0; j < binsPerBar; j++) {
                        int idx = bar * binsPerBar + j;
                        sum += Math.max(curve[idx], 0.0);
                    }
                    double averageMagnitude = sum / binsPerBar;
                    averageMagnitude = Math.min(averageMagnitude, 1.0);
                    averageMagnitude = Math.sqrt(averageMagnitude);
                    smoothCurve[bar] = AnimationUtils.base(smoothCurve[bar], averageMagnitude, 0.1);
                    float xPos = (float) bar / numBars * screenWidth;
                    float height = (float) (smoothCurve[bar] * 100f * MusicOverlay.amplitude.getValue().floatValue());

                    Render2DUtils.drawRect(
                            xPos,
                            screenHeight - height,
                            width,
                            height,
                            MusicOverlay.color.getColor()
                    );
                }
            }
        }
    }

    public static void drawSong(float x, float y, float width, float height) {
        ScaledResolution sr = new ScaledResolution(Utility.mc);
        Music current = (Music) MusicPlayer.playList.musics.get(MusicPlayer.playList.getCurrent());
        UFontRenderer s18 = FPSMaster.fontManager.s18;

        Render2DUtils.drawOptimizedRoundedRect(x, y, width, height, new Color(0, 0, 0, 180));
        Render2DUtils.drawOptimizedRoundedRect(x, y, songProgress, height, MusicOverlay.progressColor.getColor());

        songProgress = (float) AnimationUtils.base(songProgress, 6 + (width - 6) * MusicPlayer.curPlayProgress, 0.1);

        Render2DUtils.drawImage(
                new ResourceLocation("music/netease/" + current.id),
                x + 5,
                y + 5,
                height - 10,
                height - 10,
                -1
        );

        FPSMaster.fontManager.s18.drawString(
                current.name,
                x + 40,
                y + 6,
                new Color(234, 234, 234).getRGB()
        );

        FPSMaster.fontManager.s16.drawString(
                current.author,
                x + 40,
                y + 18,
                new Color(162, 162, 162).getRGB()
        );
    }
}

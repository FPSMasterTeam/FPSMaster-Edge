package top.fpsmaster.modules.music;

import top.fpsmaster.FPSMaster;

import javax.sound.sampled.LineUnavailableException;
import java.io.File;
import java.io.IOException;

import static java.lang.Math.min;

public class MusicPlayer {
    public static PlayList playList = new PlayList();
    public static int mode = 0;
    public static long startTime = 0;
    public static boolean isPlaying = false;
    public static float volume = 1f;
    public static float curPlayProgress = 0f;

    public static float getPlayProgress() {
        if (isPlaying && JLayerHelper.clip != null) {
            curPlayProgress = JLayerHelper.getProgress();
        }
        return min(curPlayProgress, 1f);
    }

    public static void play() {
        isPlaying = true;
        if (JLayerHelper.clip == null) return;
        JLayerHelper.start();
    }

    public static double[] getCurve() {
        return JLayerHelper.loudnessCurve;
    }

    public static void pause() {
        isPlaying = false;
        if (JLayerHelper.clip == null) return;
        JLayerHelper.stop();
    }

    public static void stop() {
        isPlaying = false;
        if (JLayerHelper.clip == null) return;
        JLayerHelper.stop();
    }

    public static void playFile(String path) {
        File file = new File(path);
        if (file.exists()) {
            JLayerHelper.convert(path, path.replace(".mp3", ".wav"));
            if (JLayerHelper.clip != null) {
                JLayerHelper.clip.stop();
                JLayerHelper.clip.close();
            }
            float v = Float.parseFloat(FPSMaster.configManager.configure.getOrCreate("volume", "1"));
            FPSMaster.async.runnable(() -> {
                try {
                    JLayerHelper.playWAV(path.replace(".mp3", ".wav"));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                } catch (LineUnavailableException e) {
                    throw new RuntimeException(e);
                }
                setVolume(v);
            });
        }
    }

    public static float getVolume() {
        return volume;
    }

    public static void setVolume(float volume) {
        MusicPlayer.volume = volume;
        if (JLayerHelper.clip == null) return;
        JLayerHelper.setVolume(volume);
        FPSMaster.configManager.configure.set("volume", String.valueOf(volume));
    }
}

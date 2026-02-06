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

    private static Thread playThread;

    private static float pauseAt;

    public static float getPlayProgress() {
        if (JLayerHelper.clip != null) {
            curPlayProgress = JLayerHelper.getProgress();
        }
        return min(curPlayProgress, 1f);
    }

    public static void play() {
        if (JLayerHelper.clip == null) return;
        isPlaying = true;
        JLayerHelper.start();
        if (pauseAt != 0) {
            JLayerHelper.seek(pauseAt);
            pauseAt = 0;
        }
    }

    public static double[] getCurve() {
        return JLayerHelper.loudnessCurve;
    }

    public static void pause() {
        if (JLayerHelper.clip == null) return;
        isPlaying = false;
        pauseAt = getPlayProgress();
        JLayerHelper.pause();
    }

    public static void stop() {
        if (JLayerHelper.clip == null) return;
        isPlaying = false;
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
            if (playThread != null && playThread.isAlive())
                playThread.interrupt();
            playThread = new Thread(() -> {
                try {
                    JLayerHelper.playWAV(path.replace(".mp3", ".wav"));
                } catch (IOException | LineUnavailableException e) {
                    throw new RuntimeException(e);
                }
                setVolume(v);
            });
            playThread.start();
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

    public float getPauseAt() {
        return pauseAt;
    }
}

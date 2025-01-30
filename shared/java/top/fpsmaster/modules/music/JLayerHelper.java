package top.fpsmaster.modules.music;

import javazoom.jl.converter.Converter;
import javazoom.jl.decoder.JavaLayerException;
import javax.sound.sampled.*;
import java.io.File;

public class JLayerHelper {

    public static Clip clip;

    public static float getProgress() {
        long timeElapsed = clip.getMicrosecondPosition();
        long total = clip.getMicrosecondLength();
        return (float) timeElapsed / total;
    }

    public static void playWAV(String wavFile) {
        try {
            File soundFile = new File(wavFile);
            AudioInputStream audioIn = AudioSystem.getAudioInputStream(soundFile);
            clip = AudioSystem.getClip();
            if (clip != null) {
                clip.open(audioIn);
                clip.start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void seek(float progress) {
        long totalTime = clip.getMicrosecondLength();
        long currentTime = (long) (totalTime * progress);
        clip.setMicrosecondPosition(currentTime);
    }

    public static void setVolume(float vol) {
        vol /= 2;
        vol += 0.5f;
        try {
            FloatControl volumeControl = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
            float volume = (volumeControl.getMaximum() - volumeControl.getMinimum()) * vol + volumeControl.getMinimum();
            volumeControl.setValue(volume);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void convert(String sourcePath, String targetPath) {
        try {
            Converter converter = new Converter();
            File sourceFile = new File(sourcePath);
            File targetFile = new File(targetPath);
            converter.convert(sourceFile.getPath(), targetFile.getPath());
        } catch (JavaLayerException e) {
            e.printStackTrace();
        }
    }

    public static void stop() {
        if (clip != null) {
            clip.stop();
        }
    }

    public static void start() {
        if (clip != null) {
            clip.start();
        }
    }

    public static double getDuration() {
        return clip.getMicrosecondLength() / 1000000.0 / 60.0;
    }
}

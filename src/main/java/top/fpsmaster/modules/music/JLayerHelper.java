package top.fpsmaster.modules.music;

import edu.emory.mathcs.jtransforms.fft.DoubleFFT_1D;
import javazoom.jl.converter.Converter;
import javazoom.jl.decoder.JavaLayerException;
import top.fpsmaster.modules.logger.ClientLogger;

import javax.sound.sampled.*;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import static java.lang.Math.min;
import static java.lang.Math.sqrt;

public class JLayerHelper {
    public static Clip clip;
    private static AudioInputStream audIn;
    private static byte[] audioBytes = new byte[0];
    public static double[] loudnessCurve = new double[0];

    public static float getProgress() {
        if (clip == null) return 0;
        long timeElapsed = clip.getMicrosecondPosition();
        long total = clip.getMicrosecondLength();
        return (float) timeElapsed / total;
    }

    public static void playWAV(String wavFile) throws IOException, LineUnavailableException {
        File soundFile = new File(wavFile);
        try {
            AudioInputStream aud = AudioSystem.getAudioInputStream(soundFile);
            audIn = AudioSystem.getAudioInputStream(soundFile);
            audioBytes = readAudioData(audIn);
            clip = AudioSystem.getClip();
            if (clip == null) return;
            clip.open(aud);
            clip.start();
        } catch (UnsupportedAudioFileException e) {
            ClientLogger.error("Unsupported audio file: " + wavFile);
            e.printStackTrace();
        }
    }

    public static void seek(float progress) {
        long totalTime = clip.getMicrosecondLength();
        long currentTime = (long) (totalTime * progress);
        clip.setMicrosecondPosition(currentTime);
    }

    public static void updateLoudness() {
        if (clip == null || audIn == null || !clip.isActive()) return;

        AudioFormat format = audIn.getFormat();
        if (format.getEncoding() != AudioFormat.Encoding.PCM_SIGNED || format.getSampleSizeInBits() != 16) {
            return;
        }

        double currentTimeSec = clip.getMicrosecondPosition() / 1_000_000.0;
        int fftSize = 1024;
        double fftWindowDuration = fftSize / format.getSampleRate();

        byte[] audioSegment = getAudioSegment(
                audioBytes,
                format.getSampleRate(),
                format.getFrameSize(),
                (float) (currentTimeSec - fftWindowDuration / 2),
                (float) fftWindowDuration
        );

        double[] fftData = performFFT(audioSegment);
        double[] amplitudes = computeAmplitude(fftData);

        loudnessCurve = amplitudes;
    }

    private static byte[] readAudioData(AudioInputStream audioInputStream) throws IOException {
        int bufferSize = (int) (audioInputStream.getFrameLength() * audioInputStream.getFormat().getFrameSize());
        byte[] audioBytes = new byte[bufferSize];
        audioInputStream.read(audioBytes);
        return audioBytes;
    }

    private static byte[] getAudioSegment(byte[] audioData, float sampleRate, int bytesPerFrame, float startSecond, float durationInSeconds) {
        int startSample = (int) (startSecond * sampleRate);
        int numSamples = (int) (durationInSeconds * sampleRate);

        int startByte = startSample * bytesPerFrame;
        int numBytes = numSamples * bytesPerFrame;

        int endByte = Math.min(startByte + numBytes, audioData.length);

        return Arrays.copyOfRange(audioData, startByte, endByte);
    }

    private static double[] performFFT(byte[] buffer) {
        int numSamples = buffer.length / 2;
        double[] audioData = new double[numSamples];

        for (int i = 0; i < numSamples; i++) {
            int low = buffer[i * 2] & 0xff;
            int high = buffer[i * 2 + 1];
            int sample = (high << 8) | low;
            audioData[i] = sample / 32768.0; // Normalize to [-1, 1]
        }

        int fftSize = 1024;
        double[] paddedData = new double[fftSize];
        System.arraycopy(audioData, 0, paddedData, 0, min(fftSize, audioData.length));

        DoubleFFT_1D fft = new DoubleFFT_1D(fftSize);
        fft.realForward(paddedData);

        return paddedData;
    }

    public static double[] computeAmplitude(double[] fftData) {
        int n = fftData.length;
        double[] amplitudes = new double[n / 2];

        for (int i = 0; i < amplitudes.length; i++) {
            double real = fftData[2 * i];
            double imag = (2 * i + 1 < fftData.length) ? fftData[2 * i + 1] : 0.0;
            amplitudes[i] = sqrt(real * real + imag * imag);
        }

        // Simple normalization
        double maxAmp = Arrays.stream(amplitudes).max().orElse(1.0);
        for (int i = 0; i < amplitudes.length; i++) {
            amplitudes[i] /= maxAmp;
        }

        return amplitudes;
    }

    public static void setVolume(float vol) {
        vol /= 2;
        vol += 0.5f;
        FloatControl volumeControl = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
        float volume = (volumeControl.getMaximum() - volumeControl.getMinimum()) * vol + volumeControl.getMinimum();
        volumeControl.setValue(volume);
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
            clip.close();
            clip = null;
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

    public static void pause() {
        if (clip != null) {
            clip.stop();
        }
    }
}




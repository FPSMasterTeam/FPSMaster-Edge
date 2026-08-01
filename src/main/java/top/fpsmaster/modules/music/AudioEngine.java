package top.fpsmaster.modules.music;

import net.minecraft.client.Minecraft;
import top.fpsmaster.modules.logger.ClientLogger;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.SourceDataLine;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * 纯 Java 音频播放引擎，基于 {@code javax.sound.sampled} + mp3spi（JLayer）。
 *
 * <p>兼容任意 Java 8 运行时、零原生依赖。支持 播放 / 暂停 / 停止 / 跳转(seek) / 音量 / 进度。
 * 所有 mp3 流先下载到临时文件再解码播放，以便可靠地 seek（重开流 + 丢弃到目标位置）。
 *
 * <p>解码与播放在后台线程进行，绝不阻塞渲染线程；曲目自然播完时通过
 * {@link Minecraft#addScheduledTask(Runnable)} 在主线程回调 {@code onEnded}。
 */
public class AudioEngine {

    private static final String UA =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) "
                    + "Chrome/123.0.0.0 Safari/537.36";

    private volatile Thread playThread;
    private volatile boolean stopped = true;
    private volatile boolean paused = false;

    private volatile long positionMs = 0;
    private volatile long durationMs = 0;
    private volatile long pendingSeekMs = -1;
    private volatile float volume = 0.7f; // 0..1
    private volatile FloatControl gainControl;

    private volatile File currentTemp;
    private final Object lock = new Object();

    /** 是否正在播放（含暂停态时也算“有曲目”）。 */
    public boolean isActive() {
        return !stopped;
    }

    public boolean isPaused() {
        return paused;
    }

    public boolean isPlaying() {
        return !stopped && !paused;
    }

    public long getPositionMs() {
        return positionMs;
    }

    public long getDurationMs() {
        return durationMs;
    }

    public float getVolume() {
        return volume;
    }

    /**
     * 播放一个音频直链。会先停止当前曲目，然后异步下载 + 解码播放。
     *
     * @param url         音频直链（建议为 mp3）。
     * @param referer     可选 Referer（QQ 直链有时需要 https://y.qq.com/）；可为 null。
     * @param knownDurationMs 已知时长（毫秒），用于进度条；未知传 0。
     * @param onEnded     曲目自然播完时在主线程回调；可为 null。
     */
    public void play(final String url, final String referer, long knownDurationMs, final Runnable onEnded) {
        stop();
        synchronized (lock) {
            stopped = false;
            paused = false;
            positionMs = 0;
            durationMs = knownDurationMs;
            pendingSeekMs = -1;
        }
        final Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                File temp = null;
                try {
                    temp = downloadToTemp(url, referer);
                    synchronized (lock) {
                        currentTemp = temp;
                    }
                    playLoop(temp, onEnded);
                } catch (Throwable e) {
                    ClientLogger.error("Music playback failed: " + e.getMessage());
                } finally {
                    if (temp != null) {
                        //noinspection ResultOfMethodCallIgnored
                        temp.delete();
                    }
                }
            }
        }, "FPSMaster-Music-Audio");
        t.setDaemon(true);
        synchronized (lock) {
            playThread = t;
        }
        t.start();
    }

    private void playLoop(File file, Runnable onEnded) throws Exception {
        long startMs = 0;
        boolean endedNaturally = false;

        while (!stopped) {
            AudioInputStream fileIn = null;
            AudioInputStream din = null;
            SourceDataLine line = null;
            boolean seekBreak = false;
            try {
                fileIn = AudioSystem.getAudioInputStream(new BufferedInputStream(new FileInputStream(file)));
                AudioFormat base = fileIn.getFormat();
                AudioFormat decoded = new AudioFormat(
                        AudioFormat.Encoding.PCM_SIGNED,
                        base.getSampleRate(), 16, base.getChannels(),
                        base.getChannels() * 2, base.getSampleRate(), false);
                din = AudioSystem.getAudioInputStream(decoded, fileIn);

                DataLine.Info info = new DataLine.Info(SourceDataLine.class, decoded);
                line = (SourceDataLine) AudioSystem.getLine(info);
                line.open(decoded);
                line.start();
                applyGain(line);

                double bytesPerMs = decoded.getFrameRate() * decoded.getFrameSize() / 1000.0;
                int frameSize = Math.max(1, decoded.getFrameSize());

                if (startMs > 0) {
                    // PCM has to be skipped a whole frame at a time — one frame is channels × 2 bytes
                    // for 16-bit audio. Landing mid-frame shifts every following sample by a byte or
                    // two, so the high/low halves of each sample and the left/right channels are read
                    // out of position and the result is white noise. startMs * bytesPerMs is only a
                    // multiple of the frame size by luck, which is why it happened on some seeks and
                    // not others, and why seeking again "fixed" it.
                    long skipBytes = (long) (startMs * bytesPerMs);
                    skipBytes -= skipBytes % frameSize;
                    skipDecoded(din, skipBytes);
                }
                long posBytes = 0;
                byte[] buf = new byte[4096];
                // Bytes left over from the previous read that do not yet complete a frame.
                int carry = 0;

                while (!stopped) {
                    long seek = pendingSeekMs;
                    if (seek >= 0) {
                        pendingSeekMs = -1;
                        startMs = seek;
                        seekBreak = true;
                        // Discard whatever is still queued in the line before tearing it down. The
                        // buffer holds PCM decoded at the *old* position; closing without flushing
                        // lets it play out as a burst of noise just before the new stream starts.
                        // stop() first so flush() is not racing an active playback pointer.
                        line.stop();
                        line.flush();
                        break;
                    }
                    if (paused) {
                        Thread.sleep(30);
                        continue;
                    }
                    int n = din.read(buf, carry, buf.length - carry);
                    if (n < 0) {
                        endedNaturally = true;
                        break;
                    }
                    // SourceDataLine.write is documented as undefined unless the length is a whole
                    // number of frames, and the decoder does not promise frame-aligned reads. Write
                    // only the complete frames and carry the remainder into the next read.
                    int available = carry + n;
                    int writable = available - (available % frameSize);
                    if (writable > 0) {
                        line.write(buf, 0, writable);
                        posBytes += writable;
                        positionMs = startMs + (long) (posBytes / bytesPerMs);
                    }
                    carry = available - writable;
                    if (carry > 0) {
                        System.arraycopy(buf, writable, buf, 0, carry);
                    }
                }
            } finally {
                closeQuietly(line, din, fileIn);
            }

            if (stopped) {
                return;
            }
            if (seekBreak) {
                continue; // 重开流并跳到 startMs
            }
            if (endedNaturally) {
                break;
            }
        }

        if (endedNaturally && !stopped && onEnded != null) {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc != null) {
                mc.addScheduledTask(onEnded);
            }
        }
    }

    /** 逐块读丢弃到目标字节数（mp3spi 的 skip 不可靠，故用读丢弃）。 */
    private void skipDecoded(AudioInputStream din, long bytes) throws Exception {
        byte[] junk = new byte[4096];
        long remaining = bytes;
        while (remaining > 0 && !stopped) {
            int toRead = (int) Math.min(junk.length, remaining);
            int n = din.read(junk, 0, toRead);
            if (n < 0) break;
            remaining -= n;
        }
    }

    private void applyGain(SourceDataLine line) {
        try {
            if (line.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                FloatControl ctl = (FloatControl) line.getControl(FloatControl.Type.MASTER_GAIN);
                gainControl = ctl;
                setGainDb(ctl, volume);
            } else {
                gainControl = null;
            }
        } catch (Exception ignored) {
            gainControl = null;
        }
    }

    private void setGainDb(FloatControl ctl, float vol) {
        float min = ctl.getMinimum();
        float max = ctl.getMaximum();
        float db;
        if (vol <= 0.0001f) {
            db = min;
        } else {
            db = (float) (20.0 * Math.log10(vol));
        }
        if (db < min) db = min;
        if (db > max) db = max;
        ctl.setValue(db);
    }

    /** 设置音量 0..1，立即生效。 */
    public void setVolume(float vol) {
        if (vol < 0) vol = 0;
        if (vol > 1) vol = 1;
        this.volume = vol;
        FloatControl ctl = gainControl;
        if (ctl != null) {
            try {
                setGainDb(ctl, vol);
            } catch (Exception ignored) {
            }
        }
    }

    public void pause() {
        paused = true;
    }

    public void resume() {
        paused = false;
    }

    public void togglePause() {
        paused = !paused;
    }

    /** 跳转到指定毫秒。 */
    public void seek(long ms) {
        if (ms < 0) ms = 0;
        if (durationMs > 0 && ms > durationMs) ms = durationMs;
        positionMs = ms;
        pendingSeekMs = ms;
        paused = false;
    }

    /** 停止并释放当前曲目。 */
    public void stop() {
        Thread t;
        synchronized (lock) {
            stopped = true;
            paused = false;
            t = playThread;
            playThread = null;
        }
        if (t != null) {
            t.interrupt();
            try {
                t.join(500);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
        }
        positionMs = 0;
        gainControl = null;
    }

    private File downloadToTemp(String url, String referer) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setInstanceFollowRedirects(true);
        conn.setConnectTimeout(10_000);
        conn.setReadTimeout(20_000);
        conn.setRequestProperty("User-Agent", UA);
        if (referer != null) {
            conn.setRequestProperty("Referer", referer);
        }
        File temp = File.createTempFile("fpsmaster-music-", ".mp3");
        temp.deleteOnExit();
        InputStream in = null;
        FileOutputStream out = null;
        try {
            in = conn.getInputStream();
            out = new FileOutputStream(temp);
            byte[] buf = new byte[16384];
            int n;
            while ((n = in.read(buf)) != -1) {
                if (stopped) break;
                out.write(buf, 0, n);
            }
        } finally {
            closeQuietly(in);
            closeQuietly(out);
            conn.disconnect();
        }
        return temp;
    }

    private static void closeQuietly(SourceDataLine line, AudioInputStream din, AudioInputStream fileIn) {
        if (line != null) {
            try {
                line.stop();
                line.flush();
                line.close();
            } catch (Exception ignored) {
            }
        }
        closeQuietly(din);
        closeQuietly(fileIn);
    }

    private static void closeQuietly(AutoCloseable c) {
        if (c != null) {
            try {
                c.close();
            } catch (Exception ignored) {
            }
        }
    }
}

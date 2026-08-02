package top.fpsmaster.modules.music;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.util.ResourceLocation;
import top.fpsmaster.modules.logger.ClientLogger;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 动态纹理缓存：把网络封面图 / base64 图 / 二维码 上传为 Minecraft {@link ResourceLocation}，供
 * {@code Images.draw(...)} 绘制。
 *
 * <p>下载/解码在后台线程完成，纹理上传（GL 调用）通过 {@link Minecraft#addScheduledTask(Runnable)}
 * 回到渲染线程。首帧返回 {@code null}，调用方每帧重新查询即可（就绪后返回同一个 location）。
 */
public final class MusicTextures {

    private static final String UA =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) "
                    + "Chrome/123.0.0.0 Safari/537.36";

    private static final Map<String, ResourceLocation> READY = new HashMap<>();
    private static final Set<String> LOADING = new HashSet<>();

    // 所有 AWT/ImageIO 图片解码放到单一线程串行执行：macOS(尤其 Rosetta) 下并发调用
    // AWT/CoreGraphics 原生代码会崩（objc_release）。单线程可最大限度规避。
    private static final ExecutorService IMG_EXEC = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "FPSMaster-Music-Img");
        t.setDaemon(true);
        return t;
    });

    private MusicTextures() {
    }

    /** 网络封面图。首帧返回 null，就绪后返回纹理。url 为空返回 null。 */
    public static synchronized ResourceLocation cover(final String url) {
        if (url == null || url.isEmpty()) return null;
        final String key = "cover:" + url;
        ResourceLocation loc = READY.get(key);
        if (loc != null) return loc;
        if (LOADING.contains(key)) return null;
        LOADING.add(key);
        IMG_EXEC.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    BufferedImage img = downloadImage(url);
                    upload(key, img);
                } catch (Throwable e) {
                    ClientLogger.error("Music cover load failed: " + e.getMessage());
                    unmark(key);
                }
            }
        });
        return null;
    }

    /** base64 图（可含 data:image/png;base64, 前缀），用于 QQ 登录二维码。 */
    public static synchronized ResourceLocation base64Image(final String base64OrDataUrl) {
        if (base64OrDataUrl == null || base64OrDataUrl.isEmpty()) return null;
        final String key = "b64:" + Integer.toHexString(base64OrDataUrl.hashCode());
        ResourceLocation loc = READY.get(key);
        if (loc != null) return loc;
        if (LOADING.contains(key)) return null;
        LOADING.add(key);
        IMG_EXEC.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    String data = base64OrDataUrl;
                    int comma = data.indexOf(',');
                    if (data.startsWith("data:") && comma >= 0) {
                        data = data.substring(comma + 1);
                    }
                    byte[] bytes = Base64.getDecoder().decode(data.trim());
                    BufferedImage img = ImageIO.read(new ByteArrayInputStream(bytes));
                    upload(key, img);
                } catch (Throwable e) {
                    ClientLogger.error("Music base64 image failed: " + e.getMessage());
                    unmark(key);
                }
            }
        });
        return null;
    }

    /** 由文本（网易云登录 codekey URL）生成二维码纹理。 */
    public static synchronized ResourceLocation qr(final String text) {        if (text == null || text.isEmpty()) return null;
        final String key = "qr:" + text;
        ResourceLocation loc = READY.get(key);
        if (loc != null) return loc;
        if (LOADING.contains(key)) return null;
        LOADING.add(key);
        IMG_EXEC.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    BufferedImage img = renderQr(text, 256);
                    upload(key, img);
                } catch (Throwable e) {
                    ClientLogger.error("Music QR render failed: " + e.getMessage());
                    unmark(key);
                }
            }
        });
        return null;
    }

    /** 丢弃某个 key（例如二维码刷新时），下次会重新加载。 */
    public static synchronized void invalidate(String rawKey) {
        for (String prefix : new String[]{"cover:", "b64:", "qr:"}) {
            String k = prefix + rawKey;
            READY.remove(k);
            LOADING.remove(k);
        }
    }

    /**
     * 在 {@link #IMG_EXEC} 单线程上下载图片并重新编码为 PNG 字节，结果通过回调返回。
     * 供 SMTC 等需要原始字节（而非 GL 纹理）的调用方复用同一套下载/解码路径，
     * 避免自起线程并发调用 AWT/ImageIO 在 macOS 上触发崩溃。
     */
    public static void downloadPngAsync(final String url, final java.util.function.Consumer<byte[]> callback) {
        if (url == null || url.isEmpty() || callback == null) {
            return;
        }
        IMG_EXEC.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    BufferedImage img = downloadImage(url);
                    if (img == null) {
                        callback.accept(null);
                        return;
                    }
                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    ImageIO.write(img, "png", bos);
                    callback.accept(bos.toByteArray());
                } catch (Throwable e) {
                    ClientLogger.error("Music PNG download failed: " + e.getMessage());
                    callback.accept(null);
                }
            }
        });
    }

    private static synchronized void unmark(String key) {
        LOADING.remove(key);
    }

    private static void upload(final String key, final BufferedImage raw) {
        if (raw == null) {
            unmark(key);
            return;
        }
        final BufferedImage argb = toArgb(raw);
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null) {
            unmark(key);
            return;
        }
        mc.addScheduledTask(new Runnable() {
            @Override
            public void run() {
                try {
                    Minecraft m = Minecraft.getMinecraft();
                    ResourceLocation loc = m.getTextureManager()
                            .getDynamicTextureLocation("music_" + Integer.toHexString(key.hashCode()),
                                    new DynamicTexture(argb));
                    synchronized (MusicTextures.class) {
                        READY.put(key, loc);
                        LOADING.remove(key);
                    }
                } catch (Throwable e) {
                    ClientLogger.error("Music texture upload failed: " + e.getMessage());
                    unmark(key);
                }
            }
        });
    }

    private static BufferedImage toArgb(BufferedImage src) {
        if (src.getType() == BufferedImage.TYPE_INT_ARGB) return src;
        // 纯 Java 拷贝，避开 Graphics2D.createGraphics（macOS 上可能走原生 CoreGraphics）
        int w = src.getWidth();
        int h = src.getHeight();
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        int[] px = src.getRGB(0, 0, w, h, null, 0, w);
        out.setRGB(0, 0, w, h, px, 0, w);
        return out;
    }

    private static BufferedImage downloadImage(String url) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setInstanceFollowRedirects(true);
        conn.setConnectTimeout(10_000);
        conn.setReadTimeout(15_000);
        conn.setRequestProperty("User-Agent", UA);
        try {
            return ImageIO.read(conn.getInputStream());
        } finally {
            conn.disconnect();
        }
    }

    private static BufferedImage renderQr(String text, int size) throws Exception {
        Map<EncodeHintType, Object> hints = new HashMap<>();
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
        hints.put(EncodeHintType.MARGIN, 1);
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
        BitMatrix matrix = new QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, size, size, hints);
        int w = matrix.getWidth();
        int h = matrix.getHeight();
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        int black = Color.BLACK.getRGB();
        int white = Color.WHITE.getRGB();
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                img.setRGB(x, y, matrix.get(x, y) ? black : white);
            }
        }
        return img;
    }
}

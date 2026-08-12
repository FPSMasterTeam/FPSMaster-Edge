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
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
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

    /**
     * 上限存在的原因：每张封面都是一个 {@link DynamicTexture}，它既占一份 GL 纹理，又在堆上长期
     * 持有整幅图的 {@code int[]}。歌单/搜索结果每滚过一屏就是一批新 URL，没有上限的话浏览一会儿
     * 就能攒出几百张常驻纹理——显存和堆一起涨，且永远不会回落。
     *
     * <p>取 64 是因为同屏最多也就 20 来张封面：淘汰的一定不是本帧要画的那张。
     */
    private static final int MAX_READY = 64;

    /**
     * 访问序 LRU。淘汰时必须连同 GL 纹理一起删，只把 map 项拿掉等于把纹理泄漏给 TextureManager：
     * {@code getDynamicTextureLocation} 每次都生成一个全新的 location 并注册进去，没人删就没人回收。
     */
    private static final Map<String, ResourceLocation> READY =
            new LinkedHashMap<String, ResourceLocation>(16, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, ResourceLocation> eldest) {
                    if (size() <= MAX_READY) {
                        return false;
                    }
                    deleteTexture(eldest.getValue());
                    return true;
                }
            };
    private static final Set<String> LOADING = new HashSet<>();

    // 所有 AWT/ImageIO 图片解码放到单一线程串行执行：macOS(尤其 Rosetta) 下并发调用
    // AWT/CoreGraphics 原生代码会崩（objc_release）。单线程可最大限度规避。
    private static final ExecutorService IMG_EXEC = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "FPSMaster-Music-Img");
        t.setDaemon(true);
        return t;
    });

    // 网络下载单独一个池。串行的约束只来自 AWT，HTTP 没有这个问题，混在 IMG_EXEC 上会让一次
    // 超时(连接 10s + 读 15s)把后面所有解码堵死——扫码登录的二维码明明不需要联网，却要排在
    // 某个封面下载后面等最多 25 秒。
    private static final ExecutorService NET_EXEC = Executors.newFixedThreadPool(2, r -> {
        Thread t = new Thread(r, "FPSMaster-Music-Net");
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
        NET_EXEC.execute(new Runnable() {
            @Override
            public void run() {
                final byte[] bytes;
                try {
                    bytes = downloadBytes(url);
                } catch (Throwable e) {
                    ClientLogger.error("Music cover download failed: " + e.getMessage());
                    unmark(key);
                    return;
                }
                if (bytes == null) {
                    unmark(key);
                    return;
                }
                // 拿到字节之后才回到串行线程解码
                IMG_EXEC.execute(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            upload(key, ImageIO.read(new ByteArrayInputStream(bytes)));
                        } catch (Throwable e) {
                            ClientLogger.error("Music cover decode failed: " + e.getMessage());
                            unmark(key);
                        }
                    }
                });
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
            deleteTexture(READY.remove(k));
            LOADING.remove(k);
        }
    }

    /**
     * 删除一个动态纹理。
     *
     * <p>只在渲染线程上调用：淘汰发生在 {@link #upload} 里的 {@code addScheduledTask} 中，
     * {@link #invalidate} 也来自 UI 交互。TextureManager 为 null 说明客户端还没起来或已经关了，
     * 那时候没有纹理需要删。
     */
    private static void deleteTexture(ResourceLocation location) {
        if (location == null) {
            return;
        }
        try {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc != null && mc.getTextureManager() != null) {
                mc.getTextureManager().deleteTexture(location);
            }
        } catch (Throwable e) {
            ClientLogger.warn("Failed to delete music texture " + location + ": " + e.getMessage());
        }
    }

    /**
     * 下载图片的原始字节，结果通过回调返回；下载失败传 {@code null}。
     *
     * <p>供 SMTC 这类只要字节、不要 GL 纹理的调用方使用。这条路径完全不碰 AWT：原先它是
     * 「下载 → ImageIO.read 解成 BufferedImage → ImageIO.write 编回 PNG」，而 SMTC 的
     * {@code RandomAccessStreamReference} 吃的就是图片流，Windows 自己会解码，JPEG 也认——
     * 那一读一写既是白做的，又把这条路径绑上了 {@link #IMG_EXEC} 的串行队列。
     *
     * <p>回调在 {@link #NET_EXEC} 上执行，不要在里面做阻塞的事。
     */
    public static void downloadBytesAsync(final String url, final java.util.function.Consumer<byte[]> callback) {
        if (url == null || url.isEmpty() || callback == null) {
            return;
        }
        NET_EXEC.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    callback.accept(downloadBytes(url));
                } catch (Throwable e) {
                    ClientLogger.error("Music image download failed: " + e.getMessage());
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
                        // 同一个 key 重复上传（淘汰后又被请求）时，旧 location 换下来也要删掉，
                        // 否则 put 覆盖不会触发淘汰回调，那张纹理就没人管了。
                        deleteTexture(READY.put(key, loc));
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

    /** 纯网络 IO，不碰 AWT——调用方拿到字节后自行决定在哪解码。 */
    private static byte[] downloadBytes(String url) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setInstanceFollowRedirects(true);
        conn.setConnectTimeout(10_000);
        conn.setReadTimeout(15_000);
        conn.setRequestProperty("User-Agent", UA);
        try (InputStream in = conn.getInputStream()) {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) != -1) {
                bos.write(buf, 0, n);
            }
            return bos.size() == 0 ? null : bos.toByteArray();
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

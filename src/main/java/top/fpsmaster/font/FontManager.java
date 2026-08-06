package top.fpsmaster.font;

import top.fpsmaster.font.impl.UFontRenderer;
import top.fpsmaster.modules.logger.ClientLogger;
import top.fpsmaster.utils.io.FileUtils;

import java.awt.Font;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.Map;

public class FontManager {

    public UFontRenderer s14;
    public UFontRenderer s16;
    public UFontRenderer s18;
    public UFontRenderer s20;
    public UFontRenderer s22;
    public UFontRenderer s24;
    public UFontRenderer s28;
    public UFontRenderer s36;
    public UFontRenderer s40;

    /** Upper bound for derived renderers, so scaling a component cannot grow the cache forever. */
    private static final int FONT_CACHE_LIMIT = 32;

    /** Parsed once in {@link #load()}; every size is a {@link Font#deriveFont} of this. */
    private Font baseFont;

    /** Derived renderers by size, access-ordered and bounded like the geometry caches. */
    private final Map<Integer, UFontRenderer> fonts =
            new LinkedHashMap<Integer, UFontRenderer>(64, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<Integer, UFontRenderer> eldest) {
                    return size() > FONT_CACHE_LIMIT;
                }
            };

    public void load() {
        baseFont = loadBaseFont();
        s14 = createRenderer(14);
        s16 = createRenderer(16);
        s18 = createRenderer(18);
        s20 = createRenderer(20);
        s22 = createRenderer(22);
        s24 = createRenderer(24);
        s28 = createRenderer(28);
        s36 = createRenderer(36);
        s40 = createRenderer(40);
    }

    public UFontRenderer getFont(int size) {
        UFontRenderer renderer = baseRenderer(size);
        if (renderer == null) {
            renderer = fonts.get(size);
        }
        if (renderer == null) {
            renderer = createRenderer(size);
        }
        return renderer;
    }

    private UFontRenderer baseRenderer(int size) {
        switch (size) {
            case 14:
                return s14;
            case 16:
                return s16;
            case 18:
                return s18;
            case 20:
                return s20;
            case 22:
                return s22;
            case 24:
                return s24;
            case 28:
                return s28;
            case 36:
                return s36;
            case 40:
                return s40;
            default:
                return null;
        }
    }

    private UFontRenderer createRenderer(int size) {
        if (baseFont == null) {
            baseFont = loadBaseFont();
        }
        UFontRenderer renderer = new UFontRenderer(baseFont, size);
        fonts.put(size, renderer);
        return renderer;
    }

    private static Font loadBaseFont() {
        try (InputStream is = Files.newInputStream(new File(FileUtils.fonts, "NotoSansSC-Regular.ttf").toPath())) {
            return Font.createFont(0, is);
        } catch (Exception ex) {
            ClientLogger.error("Error loading font NotoSansSC-Regular");
            return new Font("Arial", Font.PLAIN, 1);
        }
    }
}

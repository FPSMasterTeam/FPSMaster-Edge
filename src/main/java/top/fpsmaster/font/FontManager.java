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

    /** Upper bound for derived (non-base) renderers, so scaling a component cannot grow forever. */
    private static final int FONT_CACHE_LIMIT = 32;

    /** Parsed once in {@link #load()}; every size is a {@link Font#deriveFont} of this. */
    private Font baseFont;

    /**
     * Derived renderers by size, access-ordered and bounded.
     *
     * <p>The fixed {@code s14}–{@code s40} fields are <em>not</em> stored here: they are pinned for
     * the session and would otherwise sit as permanent eldest entries that never see a {@code get}.
     * Eviction disposes the atlas so VRAM does not leak when HUD scale churn creates many sizes.
     */
    private final Map<Integer, UFontRenderer> fonts =
            new LinkedHashMap<Integer, UFontRenderer>(64, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<Integer, UFontRenderer> eldest) {
                    if (size() <= FONT_CACHE_LIMIT) {
                        return false;
                    }
                    disposeRenderer(eldest.getValue());
                    return true;
                }
            };

    public void load() {
        disposeAll();
        baseFont = loadBaseFont();
        s14 = createBaseRenderer(14);
        s16 = createBaseRenderer(16);
        s18 = createBaseRenderer(18);
        s20 = createBaseRenderer(20);
        s22 = createBaseRenderer(22);
        s24 = createBaseRenderer(24);
        s28 = createBaseRenderer(28);
        s36 = createBaseRenderer(36);
        s40 = createBaseRenderer(40);
    }

    public UFontRenderer getFont(int size) {
        UFontRenderer renderer = baseRenderer(size);
        if (renderer != null) {
            return renderer;
        }
        renderer = fonts.get(size);
        if (renderer != null) {
            return renderer;
        }
        return createCachedRenderer(size);
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

    private UFontRenderer createBaseRenderer(int size) {
        ensureBaseFont();
        return new UFontRenderer(baseFont, size);
    }

    private UFontRenderer createCachedRenderer(int size) {
        ensureBaseFont();
        UFontRenderer renderer = new UFontRenderer(baseFont, size);
        fonts.put(size, renderer);
        return renderer;
    }

    private void ensureBaseFont() {
        if (baseFont == null) {
            baseFont = loadBaseFont();
        }
    }

    private void disposeAll() {
        for (UFontRenderer renderer : fonts.values()) {
            disposeRenderer(renderer);
        }
        fonts.clear();
        disposeRenderer(s14);
        disposeRenderer(s16);
        disposeRenderer(s18);
        disposeRenderer(s20);
        disposeRenderer(s22);
        disposeRenderer(s24);
        disposeRenderer(s28);
        disposeRenderer(s36);
        disposeRenderer(s40);
        s14 = null;
        s16 = null;
        s18 = null;
        s20 = null;
        s22 = null;
        s24 = null;
        s28 = null;
        s36 = null;
        s40 = null;
    }

    private static void disposeRenderer(UFontRenderer renderer) {
        if (renderer != null) {
            renderer.dispose();
        }
    }

    private static Font loadBaseFont() {
        try (InputStream is = Files.newInputStream(new File(FileUtils.fonts, "NotoSansSC-Regular.ttf").toPath())) {
            return Font.createFont(Font.TRUETYPE_FONT, is);
        } catch (Exception ex) {
            ClientLogger.error("Error loading font NotoSansSC-Regular");
            return new Font("Arial", Font.PLAIN, 1);
        }
    }
}

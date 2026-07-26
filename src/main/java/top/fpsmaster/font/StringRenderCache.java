package top.fpsmaster.font;

import net.minecraft.client.renderer.GLAllocation;
import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.opengl.GL11;
import top.fpsmaster.benchmark.BenchCounters;
import top.fpsmaster.benchmark.BenchmarkMode;
import top.fpsmaster.forge.mixin.accessor.GlStateManagerAccessor;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Caches rendered strings as display lists, so repeated text costs one call instead of one quad per
 * glyph.
 *
 * <p>HUD text is overwhelmingly repetitive — the same coordinates, the same scoreboard lines, the
 * same tab list, frame after frame — so the glyph work is done again every frame for output that has
 * not changed. Compiling a string once into a display list and replaying it collapses that to a
 * single call.
 *
 * <h3>What this deliberately does not do</h3>
 *
 * <p>The implementation this replaces also merged all 256 unicode pages and the ASCII sheet into one
 * 4096x4224 atlas to avoid texture binds between pages. That is roughly 69 MB of video memory to
 * save at most a couple of binds per string, which is the wrong trade for a client whose goals
 * include reducing memory. Vanilla binds at most two textures for any one string; that stays.
 *
 * <h3>Correctness constraints</h3>
 *
 * <p><b>Colour is part of the key.</b> A display list bakes the colour commands issued while it was
 * compiled, so the same text in a different colour is a different entry.
 *
 * <p><b>Obfuscated text is never cached.</b> A string containing the obfuscation code is meant to
 * change every frame; caching it would freeze it.
 *
 * <p><b>The cursor position is restored on replay.</b> Vanilla advances its pen as it draws, and a
 * replayed list does not, so the end position is stored with the entry and reapplied.
 *
 * <p><b>Eviction is least-recently-used and deletes the GL list.</b> The previous cache emptied
 * itself wholesale on reaching a size limit, which makes the hit rate collapse periodically for
 * exactly the steady text that benefits most, and it leaked the display lists it dropped.
 */
public final class StringRenderCache {

    /**
     * Entry ceiling. Each entry is one display list holding a handful of quads, so this is a small
     * amount of driver memory; the limit exists to bound growth from transient text such as chat.
     */
    private static final int MAX_ENTRIES = 4096;

    private static final char FORMATTING_PREFIX = '§';
    private static final char OBFUSCATED_CODE = 'k';

    /**
     * A compiled string.
     *
     * <p>Public, and returned to the caller rather than handed to a callback, because Mixin
     * relocates anonymous inner classes into the target and mangles their constructors — a callback
     * API compiled cleanly here and then died at runtime with a NoSuchMethodError on the generated
     * constructor.
     */
    public static final class Entry {
        final int displayList;
        public final float endPosX;
        public final float endPosY;

        Entry(int displayList, float endPosX, float endPosY) {
            this.displayList = displayList;
            this.endPosX = endPosX;
            this.endPosY = endPosY;
        }
    }

    /** Access-ordered, so the eldest entry is genuinely the least recently used. */
    private final LinkedHashMap<String, Entry> entries =
            new LinkedHashMap<String, Entry>(256, 0.75f, true);

    private final Map<String, Integer> widths = new LinkedHashMap<String, Integer>(256, 0.75f, true);

    /** Returns the compiled entry for this key, or null. The caller replays it and restores the pen. */
    public Entry lookup(String key) {
        Entry entry = entries.get(key);
        if (BenchmarkMode.ACTIVE) {
            if (entry == null) {
                BenchCounters.fontCacheMisses++;
            } else {
                BenchCounters.fontCacheHits++;
            }
        }
        return entry;
    }

    /** Replays a compiled string. The caller must then restore the pen from the entry. */
    public void replay(Entry entry) {
        GlStateManager.callList(entry.displayList);
        invalidateShadowedGlState();
    }

    /** Begins compiling a new entry. Returns the list id, or 0 when this string must not be cached. */
    public int beginCompile(String key) {
        if (isObfuscated(key)) {
            return 0;
        }
        evictIfFull();
        int displayList = GLAllocation.generateDisplayLists(1);
        GL11.glNewList(displayList, GL11.GL_COMPILE_AND_EXECUTE);
        return displayList;
    }

    /** Closes a compile started by {@link #beginCompile} and stores it. */
    public void endCompile(String key, int displayList, float endPosX, float endPosY) {
        GL11.glEndList();
        entries.put(key, new Entry(displayList, endPosX, endPosY));
    }

    /** Cached width for this text, or null. */
    public Integer cachedWidth(String text) {
        return widths.get(text);
    }

    public void putWidth(String text, int width) {
        if (widths.size() >= MAX_ENTRIES) {
            Iterator<String> iterator = widths.keySet().iterator();
            iterator.next();
            iterator.remove();
            if (BenchmarkMode.ACTIVE) {
                BenchCounters.fontCacheEvictions++;
            }
        }
        widths.put(text, Integer.valueOf(width));
    }

    /** Releases every display list. Call when the font texture changes or the context is lost. */
    public void clear() {
        for (Entry entry : entries.values()) {
            GLAllocation.deleteDisplayLists(entry.displayList);
        }
        entries.clear();
        widths.clear();
    }

    private void evictIfFull() {
        if (entries.size() < MAX_ENTRIES) {
            return;
        }
        Iterator<Map.Entry<String, Entry>> iterator = entries.entrySet().iterator();
        Map.Entry<String, Entry> eldest = iterator.next();
        // Dropping the entry without releasing its list would leak driver memory steadily for as
        // long as text keeps changing.
        GLAllocation.deleteDisplayLists(eldest.getValue().displayList);
        iterator.remove();
        if (BenchmarkMode.ACTIVE) {
            BenchCounters.fontCacheEvictions++;
        }
    }

    /**
     * Forces the next colour and texture call to reach the driver.
     *
     * <p>A display list replays the bind and colour commands recorded when it was compiled, and
     * {@code GlStateManager}'s shadow copy of that state has no way to observe it. Left alone, the
     * next {@code bindTexture} for whatever the shadow copy still believes is bound would be
     * dropped as redundant while the driver actually holds the font texture — the caller would then
     * draw with the wrong texture. Marking the binding unknown makes that call real.
     */
    private static void invalidateShadowedGlState() {
        GlStateManager.resetColor();
        GlStateManager.TextureState[] textureStates = GlStateManagerAccessor.getTextureState();
        if (textureStates != null) {
            int unit = GlStateManagerAccessor.getActiveTextureUnit();
            if (unit >= 0 && unit < textureStates.length) {
                textureStates[unit].textureName = -1;
            }
        }
    }

    private static boolean isObfuscated(String text) {
        for (int i = 0; i < text.length() - 1; i++) {
            if (text.charAt(i) == FORMATTING_PREFIX
                    && Character.toLowerCase(text.charAt(i + 1)) == OBFUSCATED_CODE) {
                return true;
            }
        }
        return false;
    }

}

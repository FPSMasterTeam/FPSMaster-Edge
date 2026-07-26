package top.fpsmaster.font;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import org.lwjgl.opengl.GL11;

import java.awt.Font;
import java.util.HashMap;
import java.util.Map;

/**
 * Lays out and draws a string from a {@link GlyphAtlas}.
 *
 * <p>Because every glyph of a size lives in one atlas page, a whole string — Latin, CJK, formatting
 * codes and all — is one texture bind and one draw. That is what removes the need for the layout
 * cache the previous implementation carried: there is no per-glyph bind to amortise, so laying the
 * string out again is a loop over its characters and a few float multiplies.
 *
 * <p>Widths are still memoised, because callers ask for them far more often than they draw. Layout
 * for measurement walks the same code path as layout for drawing, so the two cannot disagree.
 */
public final class TextRenderer {

    private static final char FORMATTING_PREFIX = '§';

    /** Vanilla's 16 colour codes, in the order the formatting characters index them. */
    private static final int[] COLOR_CODES = new int[16];

    static {
        for (int i = 0; i < 16; i++) {
            int base = (i >> 3 & 1) * 85;
            int red = (i >> 2 & 1) * 170 + base;
            int green = (i >> 1 & 1) * 170 + base;
            int blue = (i & 1) * 170 + base;
            if (i == 6) {
                red += 85;
            }
            COLOR_CODES[i] = (red & 0xFF) << 16 | (green & 0xFF) << 8 | (blue & 0xFF);
        }
    }

    private final GlyphAtlas atlas;
    private final Map<String, Float> widths = new HashMap<String, Float>();

    public TextRenderer(Font font) {
        this.atlas = new GlyphAtlas(font);
    }

    public int height() {
        return atlas.lineHeight();
    }

    public float width(String text) {
        if (text == null || text.isEmpty()) {
            return 0f;
        }
        Float cached = widths.get(text);
        if (cached != null) {
            return cached.floatValue();
        }
        float width = layout(text, 0f, 0f, 0, false);
        widths.put(text, Float.valueOf(width));
        return width;
    }

    /** Draws the string and returns its advance width. */
    public float draw(String text, float x, float y, int argb) {
        if (text == null || text.isEmpty()) {
            return 0f;
        }
        return layout(text, x, y, argb, true);
    }

    /**
     * Single pass over the string, used both to measure and to draw.
     *
     * <p>Keeping one implementation means a measured width can never disagree with what was
     * actually drawn, which is the failure that produces text overflowing its own background.
     */
    private float layout(String text, float x, float y, int argb, boolean draw) {
        WorldRenderer worldRenderer = null;
        if (draw) {
            if (atlas.textureId() == -1) {
                // Force the atlas into existence before the batch opens: rasterising a glyph binds a
                // texture and uploads, which cannot happen between begin() and draw().
                atlas.glyph(' ');
            }
            prewarm(text);

            GlStateManager.enableTexture2D();
            GlStateManager.enableBlend();
            GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0);
            GlStateManager.bindTexture(atlas.textureId());
            worldRenderer = Tessellator.getInstance().getWorldRenderer();
            worldRenderer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
        }

        int alpha = (argb >> 24) & 0xFF;
        if (alpha == 0) {
            alpha = 255;
        }
        int currentRgb = argb & 0xFFFFFF;
        float penX = x;
        float baseline = y + atlas.ascent();

        for (int i = 0; i < text.length(); i++) {
            char character = text.charAt(i);

            if (character == FORMATTING_PREFIX && i + 1 < text.length()) {
                int code = "0123456789abcdefklmnor".indexOf(Character.toLowerCase(text.charAt(i + 1)));
                if (code >= 0) {
                    if (code < 16) {
                        currentRgb = COLOR_CODES[code];
                    } else if (code == 21) {
                        currentRgb = argb & 0xFFFFFF;  // reset
                    }
                    // Style codes other than colour and reset are not supported by this renderer;
                    // they are consumed so they never appear as literal glyphs.
                    i++;
                    continue;
                }
            }

            GlyphAtlas.Glyph glyph = atlas.glyph(character);
            if (draw && glyph.width > 0) {
                emit(worldRenderer, glyph, penX, baseline, currentRgb, alpha);
            }
            penX += glyph.advance;
        }

        if (draw) {
            Tessellator.getInstance().draw();
            GlStateManager.disableBlend();
        }
        return penX - x;
    }

    /**
     * Rasterises any character of the string that is not in the atlas yet.
     *
     * <p>Uploading a glyph binds a texture and calls glTexSubImage2D, neither of which is legal
     * between {@code begin()} and {@code draw()}. Doing it up front keeps the batch uninterrupted.
     */
    private void prewarm(String text) {
        for (int i = 0; i < text.length(); i++) {
            char character = text.charAt(i);
            if (character == FORMATTING_PREFIX && i + 1 < text.length()
                    && "0123456789abcdefklmnor".indexOf(Character.toLowerCase(text.charAt(i + 1))) >= 0) {
                i++;
                continue;
            }
            atlas.glyph(character);
        }
    }

    private static void emit(WorldRenderer worldRenderer, GlyphAtlas.Glyph glyph,
                             float penX, float baseline, int rgb, int alpha) {
        float x0 = penX + glyph.offsetX;
        float y0 = baseline + glyph.offsetY;
        float x1 = x0 + glyph.width;
        float y1 = y0 + glyph.height;
        int red = (rgb >> 16) & 0xFF;
        int green = (rgb >> 8) & 0xFF;
        int blue = rgb & 0xFF;

        worldRenderer.pos(x0, y1, 0.0d).tex(glyph.u0, glyph.v1).color(red, green, blue, alpha).endVertex();
        worldRenderer.pos(x1, y1, 0.0d).tex(glyph.u1, glyph.v1).color(red, green, blue, alpha).endVertex();
        worldRenderer.pos(x1, y0, 0.0d).tex(glyph.u1, glyph.v0).color(red, green, blue, alpha).endVertex();
        worldRenderer.pos(x0, y0, 0.0d).tex(glyph.u0, glyph.v0).color(red, green, blue, alpha).endVertex();
    }
}

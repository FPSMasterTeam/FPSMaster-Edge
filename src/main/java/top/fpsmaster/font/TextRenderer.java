package top.fpsmaster.font;

import net.minecraft.client.renderer.GlStateManager;
import top.fpsmaster.benchmark.HudBreakdown;
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

    /**
     * Glyphs are rasterised at the full point size and drawn at half of it.
     *
     * <p>This is deliberate supersampling, not a mistake: a 16pt font occupies 8 GUI pixels and gets
     * the extra detail for free. Every caller in the client is written against that convention -
     * widths, heights and positions are all in the halved space - so the scale belongs here, where
     * measuring and drawing share one code path, rather than in the callers where the two could
     * drift apart.
     */
    private static final float RENDER_SCALE = 0.5f;

    /** 512 to 2048 is two doublings; a couple of spare passes cost nothing and cannot hang. */
    private static final int MAX_PREWARM_PASSES = 4;

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
        // Truncating, not rounding: the renderer this replaced reported lineHeight / 2 in integer
        // arithmetic, and rounding instead moves every odd-height font by a pixel.
        return (int) (atlas.lineHeight() * RENDER_SCALE);
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

    /**
     * Advance of a single character, in the same units as {@link #width}.
     *
     * <p>Vanilla's layout — chat wrapping, string trimming, tooltip boxes — is all built on
     * per-character widths, so replacing its renderer means answering that question too.
     */
    public float advance(char character) {
        return atlas.glyph(character).advance * RENDER_SCALE;
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
        long mark = HudBreakdown.enabled() ? System.nanoTime() : 0L;
        if (draw) {
            if (atlas.textureId() == -1) {
                // Force the atlas into existence before the batch opens: rasterising a glyph binds a
                // texture and uploads, which cannot happen between begin() and draw().
                atlas.glyph(' ');
            }
            prewarm(text);
            if (mark != 0L) {
                HudBreakdown.record("text:prewarm", System.nanoTime() - mark);
                mark = System.nanoTime();
            }

            GlStateManager.enableTexture2D();
            GlStateManager.enableBlend();
            GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0);
            GlStateManager.bindTexture(atlas.textureId());
            worldRenderer = Tessellator.getInstance().getWorldRenderer();
            worldRenderer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
            if (mark != 0L) {
                HudBreakdown.record("text:setup", System.nanoTime() - mark);
                mark = System.nanoTime();
            }
        }

        // Alpha passes through exactly as given, including zero. Treating zero as opaque - which
        // vanilla's own renderer does - breaks the client's fade animations: Alpha.apply multiplies
        // every colour by a global factor, so a fade that reaches zero would flash back to fully
        // opaque instead of disappearing.
        int alpha = (argb >> 24) & 0xFF;
        int currentRgb = argb & 0xFFFFFF;
        // Snapped to whole pixels like the renderer this replaces, so glyphs land on texel centres
        // instead of being resampled across two of them.
        float penX = Math.round(x);
        float baseline = Math.round(y) + atlas.inkAscent() * RENDER_SCALE;
        // Advance is accumulated separately from the pen, so the pixel snapping above cannot leak
        // into a measured width and make layout disagree with what was drawn.
        float advance = 0f;

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
            penX += glyph.advance * RENDER_SCALE;
            advance += glyph.advance * RENDER_SCALE;
        }

        if (draw) {
            if (mark != 0L) {
                HudBreakdown.record("text:emit", System.nanoTime() - mark);
                mark = System.nanoTime();
            }
            Tessellator.getInstance().draw();
            GlStateManager.disableBlend();
            if (mark != 0L) {
                HudBreakdown.record("text:submit", System.nanoTime() - mark);
            }
        }
        return advance;
    }

    /**
     * Rasterises any character of the string that is not in the atlas yet.
     *
     * <p>Uploading a glyph binds a texture and calls glTexSubImage2D, neither of which is legal
     * between {@code begin()} and {@code draw()}. Doing it up front keeps the batch uninterrupted.
     */
    private void prewarm(String text) {
        // Repeated until a whole pass happens without the page growing. Growing discards every
        // cached glyph, so a pass that triggers one leaves the characters it already handled
        // missing - and they would then be rasterised from inside the vertex batch, which rebinds
        // the texture and can grow the page again underneath UVs that have already been written.
        // That is what made the first characters of a string disappear.
        int generation;
        int attempts = 0;
        do {
            generation = atlas.generation();
            for (int i = 0; i < text.length(); i++) {
                char character = text.charAt(i);
                if (character == FORMATTING_PREFIX && i + 1 < text.length()
                        && "0123456789abcdefklmnor".indexOf(Character.toLowerCase(text.charAt(i + 1))) >= 0) {
                    i++;
                    continue;
                }
                atlas.glyph(character);
            }
            // The page can only double a fixed number of times, so this terminates; the bound is
            // here so a future change to growth cannot turn it into a hang.
        } while (generation != atlas.generation() && ++attempts < MAX_PREWARM_PASSES);
    }

    private static void emit(WorldRenderer worldRenderer, GlyphAtlas.Glyph glyph,
                             float penX, float baseline, int rgb, int alpha) {
        float x0 = penX + glyph.offsetX * RENDER_SCALE;
        float y0 = baseline + glyph.offsetY * RENDER_SCALE;
        float x1 = x0 + glyph.width * RENDER_SCALE;
        float y1 = y0 + glyph.height * RENDER_SCALE;
        int red = (rgb >> 16) & 0xFF;
        int green = (rgb >> 8) & 0xFF;
        int blue = rgb & 0xFF;

        worldRenderer.pos(x0, y1, 0.0d).tex(glyph.u0, glyph.v1).color(red, green, blue, alpha).endVertex();
        worldRenderer.pos(x1, y1, 0.0d).tex(glyph.u1, glyph.v1).color(red, green, blue, alpha).endVertex();
        worldRenderer.pos(x1, y0, 0.0d).tex(glyph.u1, glyph.v0).color(red, green, blue, alpha).endVertex();
        worldRenderer.pos(x0, y0, 0.0d).tex(glyph.u0, glyph.v0).color(red, green, blue, alpha).endVertex();
    }
}

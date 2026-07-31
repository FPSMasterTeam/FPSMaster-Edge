package top.fpsmaster.font;

import net.minecraft.client.renderer.GlStateManager;
import top.fpsmaster.benchmark.HudBreakdown;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import org.lwjgl.opengl.GL11;

import java.awt.Font;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

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

    /** Bold is the glyph drawn twice a pixel apart, and a pixel wider, exactly as vanilla does it. */
    private static final float BOLD_OFFSET = 1.0f;

    /** Italic leans the top of the glyph one pixel right and the bottom one pixel left. */
    private static final float ITALIC_SHEAR = 1.0f;

    /**
     * What obfuscated text is drawn from.
     *
     * <p>Vanilla picks a replacement of exactly the same width, which it can because every glyph
     * comes from a fixed-width page. A proportional face has no such guarantee, so the replacement
     * is drawn from this and the pen still advances by the character it stands in for — the
     * scrambling stays inside the space the real text occupies.
     */
    private static final String OBFUSCATION_POOL =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

    /** Vanilla's 16 colour codes, in the order the formatting characters index them. */
    private static final int[] COLOR_CODES = new int[16];

    /** Stands in for "whatever colour the caller asked for", resolved when the quad is submitted. */
    private static final int BASE_COLOUR = -1;

    /** Obfuscated strings are re-scrambled every frame, so a recording of one would freeze it. */
    private static final String OBFUSCATION_CODE = "§k";

    private static final int GEOMETRY_CACHE_LIMIT = 512;

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
    private final Random random = new Random();
    /** Pool characters grouped by width, so a stand-in can be picked without a search. */
    private Map<Integer, char[]> scrambleBuckets;

    private final Recorder recorder = new Recorder();

    /**
     * Laid-out geometry by string, so a string that has not changed is not laid out again.
     *
     * <p>Access-ordered and bounded: a session can produce an unbounded number of distinct strings —
     * every coordinate readout, every timer — and the ones worth keeping are the ones being drawn
     * now. Held per renderer, so each font size has its own and none of them can serve another's
     * glyphs.
     */
    private final Map<String, Recorded> geometryCache =
            new LinkedHashMap<String, Recorded>(64, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, Recorded> eldest) {
                    return size() > GEOMETRY_CACHE_LIMIT;
                }
            };

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
        float width = layout(text, 0f, 0f, 0, false, false);
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
        return draw(text, x, y, argb, false);
    }

    /**
     * Draws the string, optionally as the shadow pass behind it.
     *
     * <p>The caller can darken the colour it passes in, but not the ones the string names itself:
     * vanilla keeps a second set of its sixteen colours at a quarter intensity and a shadow pass
     * indexes into that, so a red word has a dark red shadow rather than a second red one offset by
     * a pixel. Only the pass knows which set to use, which is why it has to be told.
     */
    public float draw(String text, float x, float y, int argb, boolean shadowPass) {
        if (text == null || text.isEmpty()) {
            return 0f;
        }
        if (HudBreakdown.enabled()) {
            HudBreakdown.string(text, x, y, argb, shadowPass);
        }
        return layout(text, x, y, argb, true, shadowPass);
    }

    /**
     * Single pass over the string, used both to measure and to draw.
     *
     * <p>Keeping one implementation means a measured width can never disagree with what was
     * actually drawn, which is the failure that produces text overflowing its own background.
     */
    private float layout(String text, float x, float y, int argb, boolean draw, boolean shadowPass) {
        if (draw) {
            return drawCached(text, x, y, argb, shadowPass);
        }
        return record(text, shadowPass, false).advance;
    }

    /**
     * Runs the single pass, into the recorder rather than onto the screen.
     *
     * <p>Everything is in coordinates local to the string's own origin, so the same recording serves
     * wherever it is drawn next: a chat line that scrolls up a row, or a scoreboard that shifts when
     * an entry is added, is the same glyphs in the same relative places.
     *
     * <p>Colour is left out of it where it can be. A quad whose colour came from the caller rather
     * than from a formatting code is marked as such and resolved at submission, so the same
     * recording covers a line that fades out, or one drawn in a different colour, without being
     * built again.
     */
    private Recorded record(String text, boolean shadowPass, boolean collect) {
        if (collect) {
            recorder.reset();
        }
        // Alpha passes through exactly as given, including zero. Treating zero as opaque - which
        // vanilla's own renderer does - breaks the client's fade animations: Alpha.apply multiplies
        // every colour by a global factor, so a fade that reaches zero would flash back to fully
        // opaque instead of disappearing.
        int currentRgb = 0;
        boolean baseColour = true;
        boolean bold = false;
        boolean italic = false;
        boolean obfuscated = false;
        boolean strikethrough = false;
        boolean underline = false;

        // Local origin. The pixel snapping that used to happen here now happens at submission, on
        // the string's position rather than on every glyph in it, which is the same thing done once.
        float penX = 0f;
        float baseline = 0f;
        float strikeY = baseline - atlas.inkAscent() * RENDER_SCALE * 0.42f;
        // Advance is accumulated separately from the pen, so the pixel snapping above cannot leak
        // into a measured width and make layout disagree with what was drawn.
        float advance = 0f;

        for (int i = 0; i < text.length(); i++) {
            char character = text.charAt(i);

            // Vanilla consumes both characters whenever a section sign has anything after it, and
            // falls back to white when what follows is not a code. Matching that matters here
            // rather than only looking tidy: vanilla measures the same string the same way, so
            // treating an unknown code as two literal glyphs would draw text its own layout never
            // reserved room for.
            if (character == FORMATTING_PREFIX && i + 1 < text.length()) {
                int code = "0123456789abcdefklmnor".indexOf(Character.toLowerCase(text.charAt(i + 1)));
                if (code < 16) {
                    // A colour clears every style, which is how vanilla ends a bold run.
                    currentRgb = shadowPass
                            ? (COLOR_CODES[code < 0 ? 15 : code] & 0xFCFCFC) >> 2
                            : COLOR_CODES[code < 0 ? 15 : code];
                    baseColour = false;
                    bold = italic = obfuscated = strikethrough = underline = false;
                } else if (code == 16) {
                    obfuscated = true;
                } else if (code == 17) {
                    bold = true;
                } else if (code == 18) {
                    strikethrough = true;
                } else if (code == 19) {
                    underline = true;
                } else if (code == 20) {
                    italic = true;
                } else {
                    baseColour = true;
                    bold = italic = obfuscated = strikethrough = underline = false;
                }
                i++;
                continue;
            }

            GlyphAtlas.Glyph glyph = atlas.glyph(character);
            float step = glyph.advance * RENDER_SCALE + (bold ? BOLD_OFFSET : 0f);

            if (collect) {
                GlyphAtlas.Glyph shown = obfuscated ? atlas.glyph(scramble(character)) : glyph;
                float shear = italic ? ITALIC_SHEAR : 0f;
                int colour = baseColour ? BASE_COLOUR : currentRgb;
                if (shown.width > 0) {
                    recorder.quad(shown, penX, baseline, colour, shear);
                    if (bold) {
                        recorder.quad(shown, penX + BOLD_OFFSET, baseline, colour, shear);
                    }
                }
                if (strikethrough) {
                    recorder.decoration(penX, strikeY, penX + step, strikeY + 1f, colour);
                }
                if (underline) {
                    recorder.decoration(penX - 1f, baseline + 1f, penX + step, baseline + 2f, colour);
                }
            }

            penX += step;
            advance += step;
        }

        return collect ? recorder.finish(advance, atlas.generation()) : recorder.measured(advance);
    }

    /**
     * Draws a string, building its geometry only the first time it is seen.
     *
     * <p>The overlay is rebuilt every frame while its contents change at most once a tick, which at
     * these frame rates is fifteen to twenty-five frames in which the same strings are laid out
     * again to the same answer. Measured on the recorded Hypixel matches: 99.7% of strings on The
     * Pit and around 70% on Bed Wars are identical to the previous frame's.
     *
     * <p>Obfuscated strings are never cached. Their text is stable while their glyphs are chosen
     * afresh every frame, so a recording of one would freeze the scramble it happened to record.
     */
    private float drawCached(String text, float x, float y, int argb, boolean shadowPass) {
        long mark = HudBreakdown.enabled() ? System.nanoTime() : 0L;
        if (atlas.textureId() == -1) {
            // Force the atlas into existence before the batch opens: rasterising a glyph binds a
            // texture and uploads, which cannot happen between begin() and draw().
            atlas.glyph(' ');
        }

        boolean cacheable = text.indexOf(OBFUSCATION_CODE) < 0;
        Recorded geometry = cacheable ? geometryCache.get(cacheKey(text, shadowPass)) : null;
        if (geometry == null || geometry.generation != atlas.generation()) {
            prewarm(text);
            if (mark != 0L) {
                HudBreakdown.record("text:prewarm", System.nanoTime() - mark);
                mark = System.nanoTime();
            }
            geometry = record(text, shadowPass, true);
            if (cacheable) {
                geometryCache.put(cacheKey(text, shadowPass), geometry);
            }
            if (mark != 0L) {
                HudBreakdown.record("text:emit", System.nanoTime() - mark);
                mark = System.nanoTime();
            }
        } else if (mark != 0L) {
            HudBreakdown.record("text:hit", System.nanoTime() - mark);
            mark = System.nanoTime();
        }

        GlStateManager.enableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0);
        GlStateManager.bindTexture(atlas.textureId());
        WorldRenderer worldRenderer = Tessellator.getInstance().getWorldRenderer();
        worldRenderer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
        if (mark != 0L) {
            HudBreakdown.record("text:setup", System.nanoTime() - mark);
            mark = System.nanoTime();
        }

        // Snapped to whole pixels like the renderer this replaces, so glyphs land on texel centres
        // instead of being resampled across two of them.
        float originX = Math.round(x);
        float originY = Math.round(y) + atlas.inkAscent() * RENDER_SCALE;
        int alpha = (argb >> 24) & 0xFF;
        int baseRgb = argb & 0xFFFFFF;
        geometry.submit(worldRenderer, originX, originY, baseRgb, alpha);
        Tessellator.getInstance().draw();
        geometry.submitDecorations(originX, originY, baseRgb, alpha);
        GlStateManager.disableBlend();
        if (mark != 0L) {
            HudBreakdown.record("text:submit", System.nanoTime() - mark);
        }
        return geometry.advance;
    }

    private static String cacheKey(String text, boolean shadowPass) {
        return shadowPass ? "s" + text : text;
    }

    /**
     * A stand-in the same width as the character it replaces, or the character itself.
     *
     * <p>Vanilla picks its replacement by rejecting any of a different width, and leaves anything
     * outside its bitmap page - CJK, most punctuation beyond ASCII - unscrambled, because nothing
     * there can match. Both fall out of the same rule here: a character with no same-width stand-in
     * is drawn as itself, which is what keeps obfuscated text from spilling past its own layout.
     */
    private char scramble(char character) {
        if (character == ' ' || scrambleBuckets == null) {
            return character;
        }
        char[] bucket = scrambleBuckets.get(Integer.valueOf(Math.round(advance(character))));
        return bucket == null ? character : bucket[random.nextInt(bucket.length)];
    }

    /** Built from inside {@link #prewarm}, where rasterising a pool glyph is still legal. */
    private void buildScrambleBuckets() {
        Map<Integer, StringBuilder> grouped = new HashMap<Integer, StringBuilder>();
        for (int i = 0; i < OBFUSCATION_POOL.length(); i++) {
            char candidate = OBFUSCATION_POOL.charAt(i);
            Integer key = Integer.valueOf(Math.round(advance(candidate)));
            StringBuilder bucket = grouped.get(key);
            if (bucket == null) {
                bucket = new StringBuilder();
                grouped.put(key, bucket);
            }
            bucket.append(candidate);
        }
        Map<Integer, char[]> buckets = new HashMap<Integer, char[]>();
        for (Map.Entry<Integer, StringBuilder> entry : grouped.entrySet()) {
            buckets.put(entry.getKey(), entry.getValue().toString().toCharArray());
        }
        scrambleBuckets = buckets;
    }

    /** Scratch the single pass writes into, reused so laying a string out allocates only its result. */
    private static final class Recorder {

        private float[] quads = new float[16 * 64];
        private int[] colours = new int[64];
        private int quadCount;
        private float[] decorations = new float[4 * 16];
        private int[] decorationColours = new int[16];
        private int decorationCount;

        void reset() {
            quadCount = 0;
            decorationCount = 0;
        }

        /** {@code shear} leans the top edge right and the bottom edge left, which is italic. */
        void quad(GlyphAtlas.Glyph glyph, float penX, float baseline, int colour, float shear) {
            if ((quadCount + 1) * 16 > quads.length) {
                float[] wider = new float[quads.length * 2];
                System.arraycopy(quads, 0, wider, 0, quads.length);
                quads = wider;
                int[] widerColours = new int[colours.length * 2];
                System.arraycopy(colours, 0, widerColours, 0, colours.length);
                colours = widerColours;
            }
            float x0 = penX + glyph.offsetX * RENDER_SCALE;
            float y0 = baseline + glyph.offsetY * RENDER_SCALE;
            float x1 = x0 + glyph.width * RENDER_SCALE;
            float y1 = y0 + glyph.height * RENDER_SCALE;
            int base = quadCount * 16;
            quads[base] = x0 - shear;      quads[base + 1] = y1;
            quads[base + 2] = glyph.u0;    quads[base + 3] = glyph.v1;
            quads[base + 4] = x1 - shear;  quads[base + 5] = y1;
            quads[base + 6] = glyph.u1;    quads[base + 7] = glyph.v1;
            quads[base + 8] = x1 + shear;  quads[base + 9] = y0;
            quads[base + 10] = glyph.u1;   quads[base + 11] = glyph.v0;
            quads[base + 12] = x0 + shear; quads[base + 13] = y0;
            quads[base + 14] = glyph.u0;   quads[base + 15] = glyph.v0;
            colours[quadCount] = colour;
            quadCount++;
        }

        /**
         * Strikethrough and underline bars, held back until the glyphs have been drawn.
         *
         * <p>They are untextured, and a batch cannot change that halfway through, so emitting them
         * as they are met would mean ending the glyph batch at every decorated character.
         */
        void decoration(float x0, float y0, float x1, float y1, int colour) {
            if (decorationCount * 4 == decorations.length) {
                float[] wider = new float[decorations.length * 2];
                System.arraycopy(decorations, 0, wider, 0, decorations.length);
                decorations = wider;
                int[] widerColours = new int[decorationColours.length * 2];
                System.arraycopy(decorationColours, 0, widerColours, 0, decorationColours.length);
                decorationColours = widerColours;
            }
            int base = decorationCount * 4;
            decorations[base] = x0;
            decorations[base + 1] = y0;
            decorations[base + 2] = x1;
            decorations[base + 3] = y1;
            decorationColours[decorationCount] = colour;
            decorationCount++;
        }

        Recorded finish(float advance, int generation) {
            return new Recorded(
                    java.util.Arrays.copyOf(quads, quadCount * 16),
                    java.util.Arrays.copyOf(colours, quadCount),
                    java.util.Arrays.copyOf(decorations, decorationCount * 4),
                    java.util.Arrays.copyOf(decorationColours, decorationCount),
                    advance, generation);
        }

        Recorded measured(float advance) {
            return new Recorded(null, null, null, null, advance, -1);
        }
    }

    /** One string's geometry, in coordinates local to its own origin and without its colour. */
    private static final class Recorded {

        private final float[] quads;
        private final int[] colours;
        private final float[] decorations;
        private final int[] decorationColours;
        final float advance;
        final int generation;

        Recorded(float[] quads, int[] colours, float[] decorations, int[] decorationColours,
                 float advance, int generation) {
            this.quads = quads;
            this.colours = colours;
            this.decorations = decorations;
            this.decorationColours = decorationColours;
            this.advance = advance;
            this.generation = generation;
        }

        void submit(WorldRenderer worldRenderer, float originX, float originY, int baseRgb, int alpha) {
            for (int i = 0; i < colours.length; i++) {
                if (HudBreakdown.enabled()) {
                    HudBreakdown.quad();
                }
                int rgb = colours[i] == BASE_COLOUR ? baseRgb : colours[i];
                int red = (rgb >> 16) & 0xFF;
                int green = (rgb >> 8) & 0xFF;
                int blue = rgb & 0xFF;
                int base = i * 16;
                for (int vertex = 0; vertex < 4; vertex++) {
                    int at = base + vertex * 4;
                    worldRenderer.pos(originX + quads[at], originY + quads[at + 1], 0.0d)
                            .tex(quads[at + 2], quads[at + 3])
                            .color(red, green, blue, alpha)
                            .endVertex();
                }
            }
        }

        void submitDecorations(float originX, float originY, int baseRgb, int alpha) {
            if (decorationColours.length == 0) {
                return;
            }
            GlStateManager.disableTexture2D();
            WorldRenderer worldRenderer = Tessellator.getInstance().getWorldRenderer();
            worldRenderer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
            for (int i = 0; i < decorationColours.length; i++) {
                int base = i * 4;
                float x0 = originX + decorations[base];
                float y0 = originY + decorations[base + 1];
                float x1 = originX + decorations[base + 2];
                float y1 = originY + decorations[base + 3];
                int rgb = decorationColours[i] == BASE_COLOUR ? baseRgb : decorationColours[i];
                int red = (rgb >> 16) & 0xFF;
                int green = (rgb >> 8) & 0xFF;
                int blue = rgb & 0xFF;
                worldRenderer.pos(x0, y1, 0.0d).color(red, green, blue, alpha).endVertex();
                worldRenderer.pos(x1, y1, 0.0d).color(red, green, blue, alpha).endVertex();
                worldRenderer.pos(x1, y0, 0.0d).color(red, green, blue, alpha).endVertex();
                worldRenderer.pos(x0, y0, 0.0d).color(red, green, blue, alpha).endVertex();
            }
            Tessellator.getInstance().draw();
            GlStateManager.enableTexture2D();
        }
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
            boolean obfuscated = false;
            for (int i = 0; i < text.length(); i++) {
                char character = text.charAt(i);
                if (character == FORMATTING_PREFIX && i + 1 < text.length()) {
                    char code = Character.toLowerCase(text.charAt(i + 1));
                    obfuscated |= code == 'k';
                    i++;  // consumed as a code by layout, whether or not it is a known one
                    continue;
                }
                atlas.glyph(character);
            }
            if (obfuscated) {
                // Which stand-in each obfuscated character gets is decided inside the batch, so any
                // of them could be the one that is missing. None of the string's own characters
                // would say so.
                for (int i = 0; i < OBFUSCATION_POOL.length(); i++) {
                    atlas.glyph(OBFUSCATION_POOL.charAt(i));
                }
                if (scrambleBuckets == null) {
                    buildScrambleBuckets();
                }
            }
            // The page can only double a fixed number of times, so this terminates; the bound is
            // here so a future change to growth cannot turn it into a hang.
        } while (generation != atlas.generation() && ++attempts < MAX_PREWARM_PASSES);
    }

}

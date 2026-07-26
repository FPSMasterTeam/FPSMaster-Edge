package top.fpsmaster.font;

import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import top.fpsmaster.benchmark.BenchCounters;
import top.fpsmaster.benchmark.BenchmarkMode;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Map;

/**
 * On-demand glyph atlas: one texture per font size, filled as characters are first used.
 *
 * <p>Glyphs are packed with a shelf allocator — a cursor walks left to right along a row and drops
 * to a new row when the current one is full. That is not the tightest packing possible, but glyph
 * heights within a single font size barely vary, so the waste is small and the allocator stays a
 * handful of integer comparisons.
 *
 * <h3>Why one page rather than many</h3>
 *
 * <p>The implementation this replaces spread glyphs across a grid of 256x256 pages, which meant a
 * string mixing Latin and CJK could span several textures and force a bind between them — and a
 * bind splits the draw. A single page large enough for the whole working set keeps any string to
 * one bind and one draw, which is what makes the per-string layout cache unnecessary.
 *
 * <p>A second page is allocated only if the first genuinely fills, which for a UI font means a very
 * large mixed-script working set.
 */
public final class GlyphAtlas {

    /**
     * Starting page size. 512x512 RGBA is 1 MB and holds roughly a thousand glyphs at UI sizes,
     * which covers Latin plus a working set of CJK without reserving memory for glyphs no interface
     * ever draws. The page doubles if it genuinely fills.
     */
    private static final int INITIAL_PAGE_SIZE = 512;

    /** Ceiling on growth, so a pathological glyph set cannot consume unbounded video memory. */
    private static final int MAX_PAGE_SIZE = 2048;

    /** Padding between glyphs so linear filtering cannot sample a neighbour. */
    private static final int PADDING = 1;

    public static final class Glyph {
        public final float u0;
        public final float v0;
        public final float u1;
        public final float v1;
        public final int width;
        public final int height;
        public final int offsetX;
        public final int offsetY;
        public final float advance;

        Glyph(float u0, float v0, float u1, float v1, int width, int height,
              int offsetX, int offsetY, float advance) {
            this.u0 = u0;
            this.v0 = v0;
            this.u1 = u1;
            this.v1 = v1;
            this.width = width;
            this.height = height;
            this.offsetX = offsetX;
            this.offsetY = offsetY;
            this.advance = advance;
        }
    }

    private final Font font;
    private final FontRenderContext fontRenderContext;
    private final Map<Character, Glyph> glyphs = new HashMap<Character, Glyph>();

    private int textureId = -1;
    private int pageSize = INITIAL_PAGE_SIZE;
    private int shelfX;
    private int shelfY;
    private int shelfHeight;

    private final int ascent;
    private final int lineHeight;

    public GlyphAtlas(Font font) {
        this.font = font;
        // Antialiasing on, fractional metrics off: fractional advances make identical text land on
        // different subpixels between draws, which shows up as text that shimmers as it moves.
        this.fontRenderContext = new FontRenderContext(null, true, false);
        BufferedImage probe = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D probeGraphics = probe.createGraphics();
        probeGraphics.setFont(font);
        this.ascent = probeGraphics.getFontMetrics().getAscent();
        this.lineHeight = probeGraphics.getFontMetrics().getHeight();
        probeGraphics.dispose();
    }

    public int ascent() {
        return ascent;
    }

    public int lineHeight() {
        return lineHeight;
    }

    public int textureId() {
        return textureId;
    }

    /** Returns the glyph for a character, rasterising and uploading it on first use. */
    public Glyph glyph(char character) {
        Glyph glyph = glyphs.get(Character.valueOf(character));
        if (glyph != null) {
            return glyph;
        }
        glyph = rasterise(character);
        glyphs.put(Character.valueOf(character), glyph);
        return glyph;
    }

    private Glyph rasterise(char character) {
        ensureTexture();

        GlyphVector vector = font.createGlyphVector(fontRenderContext, new char[]{character});
        Rectangle2D bounds = vector.getGlyphPixelBounds(0, fontRenderContext, 0.0f, 0.0f);
        float advance = (float) vector.getGlyphMetrics(0).getAdvanceX();

        int width = (int) Math.ceil(bounds.getWidth());
        int height = (int) Math.ceil(bounds.getHeight());
        if (width <= 0 || height <= 0) {
            // Whitespace and unrenderable characters still need an advance.
            return new Glyph(0f, 0f, 0f, 0f, 0, 0, 0, 0, advance);
        }

        if (!allocate(width, height) && !grow()) {
            // Out of room and unable to grow. Returning an advance-only glyph would render the text
            // invisible, so the last page is reused rather than dropping output; overlapping glyphs
            // are a visible artefact, silently missing text is not.
            shelfX = 0;
            shelfY = 0;
            shelfHeight = 0;
            allocate(width, height);
        }
        int x = shelfX - width - PADDING;
        int y = shelfY;

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS,
                RenderingHints.VALUE_FRACTIONALMETRICS_OFF);
        graphics.setFont(font);
        graphics.setColor(Color.WHITE);
        graphics.drawGlyphVector(vector, (float) -bounds.getX(), (float) -bounds.getY());
        graphics.dispose();

        upload(image, x, y);

        return new Glyph(
                x / (float) pageSize, y / (float) pageSize,
                (x + width) / (float) pageSize, (y + height) / (float) pageSize,
                width, height,
                (int) Math.floor(bounds.getX()), (int) Math.floor(bounds.getY()),
                advance);
    }

    /** Advances the shelf cursor. Returns false when the page is full. */
    private boolean allocate(int width, int height) {
        if (shelfX + width + PADDING > pageSize) {
            shelfX = 0;
            shelfY += shelfHeight + PADDING;
            shelfHeight = 0;
        }
        if (shelfY + height + PADDING > pageSize) {
            return false;
        }
        shelfX += width + PADDING;
        shelfHeight = Math.max(shelfHeight, height);
        return true;
    }

    /**
     * Doubles the page and drops every cached glyph so they re-rasterise into it.
     *
     * <p>Growing rather than adding a second page keeps any string to a single texture bind, which
     * is the property that makes a per-string layout cache unnecessary. The cost is one frame of
     * re-rasterisation, and only when a page genuinely fills.
     *
     * <p>Safe to call here because rasterisation only happens during prewarm, before the vertex
     * batch opens — binding and uploading a texture between begin() and draw() would not be.
     */
    private boolean grow() {
        if (pageSize >= MAX_PAGE_SIZE) {
            return false;
        }
        pageSize *= 2;
        glyphs.clear();
        shelfX = 0;
        shelfY = 0;
        shelfHeight = 0;
        if (textureId != -1) {
            GlStateManager.deleteTexture(textureId);
            if (BenchmarkMode.ACTIVE) {
                BenchCounters.texturesReleased++;
            }
            textureId = -1;
        }
        ensureTexture();
        return true;
    }

    private void ensureTexture() {
        if (textureId != -1) {
            return;
        }
        textureId = GlStateManager.generateTexture();
        if (BenchmarkMode.ACTIVE) {
            BenchCounters.texturesAllocated++;
        }
        GlStateManager.bindTexture(textureId);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, pageSize, pageSize, 0,
                GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, (ByteBuffer) null);
    }

    private void upload(BufferedImage image, int x, int y) {
        int width = image.getWidth();
        int height = image.getHeight();
        int[] pixels = image.getRGB(0, 0, width, height, null, 0, width);
        ByteBuffer buffer = ByteBuffer.allocateDirect(width * height * 4).order(ByteOrder.nativeOrder());
        for (int pixel : pixels) {
            buffer.put((byte) ((pixel >> 16) & 0xFF));
            buffer.put((byte) ((pixel >> 8) & 0xFF));
            buffer.put((byte) (pixel & 0xFF));
            buffer.put((byte) ((pixel >> 24) & 0xFF));
        }
        buffer.flip();
        GlStateManager.bindTexture(textureId);
        GL11.glTexSubImage2D(GL11.GL_TEXTURE_2D, 0, x, y, width, height,
                GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, buffer);
    }
}

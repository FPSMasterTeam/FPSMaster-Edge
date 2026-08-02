package top.fpsmaster.font.impl;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.util.ResourceLocation;
import top.fpsmaster.benchmark.HudBreakdown;
import top.fpsmaster.font.TextRenderer;
import top.fpsmaster.modules.client.GlobalTextFilter;
import top.fpsmaster.modules.logger.ClientLogger;
import top.fpsmaster.FPSMaster;
import top.fpsmaster.utils.io.FileUtils;
import top.fpsmaster.utils.render.draw.Colors;
import top.fpsmaster.utils.render.gui.UiScale;

import java.awt.*;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;

import org.lwjgl.opengl.GL11;

import static top.fpsmaster.utils.render.state.Alpha.apply;

public class UFontRenderer extends FontRenderer {
    private final int FONT_HEIGHT = 8;
    private TextRenderer textRenderer;
    private final int size;

    public UFontRenderer(String name, int size) {
        super(
                Minecraft.getMinecraft().gameSettings,
                new ResourceLocation("textures/font/ascii.png"),
                Minecraft.getMinecraft().getTextureManager(),
                false
        );
        this.size = size;
        Font font;
        try {
            InputStream is = Files.newInputStream(new File(FileUtils.fonts, name + ".ttf").toPath());
            font = Font.createFont(0, is);
            font = font.deriveFont(Font.PLAIN, size);
        } catch (Exception ex) {
            ClientLogger.error("Error loading font " + name);
            font = new Font("Arial", Font.PLAIN, size);
        }

        this.textRenderer = new TextRenderer(font);
    }

    /**
     * Draws the specified string with a shadow.
     */
    @Override
    public int drawStringWithShadow(String text, float x, float y, int color) {
        Color color1 = Colors.toColor(color);
        this.drawString(text, x, y, new Color(color1.getRed(), color1.getGreen(), color1.getBlue(), color1.getAlpha()).getRGB(), true);
        return getStringWidth(text);
    }


    public String trimStringToWidth(String text, float width) {
        return trimString(text, width, false);
    }

    /**
     * Deliberately does <em>not</em> run {@link GlobalTextFilter}: callers such as {@code TextField} use
     * the returned length as an index back into the original, unfiltered string, and NameProtect's
     * substitution changes the length. Filtering belongs to the draw path only.
     */
    public String trimString(String text, float width, boolean reverse) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        // The atlas renderer measures its own glyphs; StringCache went with the TrueType
        // renderer it belonged to.
        //
        // NOTE: {@code reverse} is not honoured — this always trims from the front. The
        // caller that passes true is TextField, keeping the tail of a right-scrolled line
        // visible, so that path trims from the wrong end.
        StringBuilder trimmed = new StringBuilder();
        for (char c : text.toCharArray()) {
            if (getStringWidth(trimmed.toString()) >= width) {
                break;
            }
            trimmed.append(c);
        }
        return trimmed.toString();
    }

    /**
     * Draws the specified string.
     */
    public int drawString(String text, float x, int y, int color) {
        Color color1 = new Color(color);
        return this.drawString(text, x, y, new Color(color1.getRed(), color1.getGreen(), color1.getBlue()).getRGB(), false);
    }

    public int drawString(String text, float x, float y, int color) {
        this.drawString(text, x, y, color, false);
        return getStringWidth(text);
    }

    public int drawStringCapableWithEmoji(String text, float x, float y, int color) {
        char[] chars = text.toCharArray();
        int lastCut = 0;
        float xOffset = x;
        for (int i = 0; i < chars.length; i++) {
            if (isEmojiCharacter(text.codePointAt(i))) {
                xOffset += this.drawString(text.substring(0, i), xOffset, y, color, false);
                this.drawString(String.valueOf(chars[i]), xOffset, y, color, false);
                xOffset += this.getStringWidth(String.valueOf(chars[i]));
                lastCut = i + 1;
            }
        }
        this.drawString(text.substring(lastCut), xOffset, y, color, false);
        return getStringWidth(text);
    }

    public static boolean isEmojiCharacter(int codePoint) {
        return (codePoint == 0x0) ||
                (codePoint == 0x9) ||
                (codePoint == 0xA) ||
                (codePoint == 0xD) ||
                (codePoint >= 0x20 && codePoint <= 0xD7FF) || ((codePoint >= 0xE000) && (codePoint <= 0xFFFD));
    }

    /**
     * Draws the specified string.
     */
    @Override
    public int drawString(String text, float x, float y, int color, boolean dropShadow) {
        float densityScale = getDensityScale();
        if (densityScale > 1.0f) {
            return drawHighDensityString(text, x, y, color, dropShadow, densityScale);
        }
        return drawStringInternal(text, x, y, color, dropShadow);
    }

    private int drawHighDensityString(String text, float x, float y, int color, boolean dropShadow, float densityScale) {
        UFontRenderer renderer = getDensityRenderer(densityScale);
        if (renderer == this) {
            return drawStringInternal(text, x, y, color, dropShadow);
        }

        float actualDensityScale = renderer.size / (float) size;
        float inverseScale = 1.0f / actualDensityScale;
        GL11.glPushMatrix();
        GL11.glScalef(inverseScale, inverseScale, 1.0f);
        try {
            // Offset is expressed in the density renderer's device space, so scale the 1px GUI-space
            // shadow by the same factor as the coordinates. Passing half of this (the old value) came
            // back as a 0.5px GUI shadow after the inverse scale, i.e. the very offset that pixel
            // snapping in StringCache.renderString was changed to avoid.
            int result = renderer.drawStringInternal(text, x * actualDensityScale, y * actualDensityScale, color, dropShadow, actualDensityScale);
            return Math.round(result * inverseScale);
        } finally {
            GL11.glPopMatrix();
        }
    }

    private int drawStringInternal(String text, float x, float y, int color, boolean dropShadow) {
        return drawStringInternal(text, x, y, color, dropShadow, 1.0f);
    }

    private int drawStringInternal(String text, float x, float y, int color, boolean dropShadow, float shadowOffset) {
        long mark = HudBreakdown.enabled() ? System.nanoTime() : 0L;
        int drawn = edge$drawStringInternal(text, x, y, color, dropShadow, shadowOffset);
        if (mark != 0L) {
            HudBreakdown.record("ourFont:draw", System.nanoTime() - mark);
        }
        return drawn;
    }

    private int edge$drawStringInternal(String text, float x, float y, int color, boolean dropShadow, float shadowOffset) {
        // Vanilla treats a colour with no alpha bits as opaque — its drawString does
        // `if ((color & 0xFC000000) == 0) color |= 0xFF000000` and the custom-font mixin keeps
        // the rule for the vanilla-replacement path. Without it here, callers that pass opaque
        // colours the vanilla way (the custom scoreboard passes 0xFFFFFF) draw fully transparent
        // once routed through this renderer.
        if ((color & 0xFC000000) == 0) {
            color |= 0xFF000000;
        }
        color = apply(color);
        int i;
        if (dropShadow) {
            if (Colors.toColor(color).getAlpha() > 50) {
                textRenderer.draw(text, x + shadowOffset, y + shadowOffset,
                        new Color(20, 20, 20, Colors.toColor(color).getAlpha()).getRGB(), true);
            }
        }
        i = Math.round(textRenderer.draw(text, x, y, color));
        return i;
    }

    @Override
    public int getStringWidth(String text) {
        long mark = HudBreakdown.enabled() ? System.nanoTime() : 0L;
        int measured = edge$getStringWidth(text);
        if (mark != 0L) {
            HudBreakdown.record("ourFont:width", System.nanoTime() - mark);
        }
        return measured;
    }

    private int edge$getStringWidth(String text) {
        text = GlobalTextFilter.filter(text);
        float densityScale = getDensityScale();
        if (densityScale > 1.0f) {
            UFontRenderer renderer = getDensityRenderer(densityScale);
            if (renderer != this) {
                float actualDensityScale = renderer.size / (float) size;
                return Math.round(renderer.textRenderer.width(text) / actualDensityScale);
            }
        }
        return Math.round(textRenderer.width(text));
    }

    /**
     * Draws without the client's global fade or its text filter, for standing in as vanilla's
     * renderer.
     *
     * <p>{@link #drawString} multiplies by {@link top.fpsmaster.utils.render.state.Alpha}, which is
     * how the client's own screens fade in. Vanilla's HUD is not part of that animation and must not
     * fade with it, so this path leaves the colour alone.
     */
    public float drawRaw(String text, float x, float y, int argb, boolean shadowPass) {
        return textRenderer.draw(text, x, y, argb, shadowPass);
    }

    /** The shadowed form of {@link #drawRaw}, as one recording and one draw rather than two. */
    public float drawRawWithShadow(String text, float x, float y, int argb) {
        return textRenderer.drawWithShadow(text, x, y, argb);
    }

    /** Advance of one character, for vanilla's per-character layout. */
    public float advanceOf(char character) {
        return textRenderer.advance(character);
    }

    public void drawCenteredString(String text, float x, float y, int color) {
        drawString(text, x - textRenderer.width(text) / 2f, y, color, false);
    }

    public int getHeight() {
        float densityScale = getDensityScale();
        if (densityScale > 1.0f) {
            UFontRenderer renderer = getDensityRenderer(densityScale);
            if (renderer != this) {
                float actualDensityScale = renderer.size / (float) size;
                return Math.round(renderer.textRenderer.height() / actualDensityScale);
            }
        }
        return textRenderer.height();
    }

    private float getDensityScale() {
        if (!UiScale.isActive()) {
            return 1.0f;
        }
        return Math.max(1.0f, UiScale.getPixelScale());
    }

    private UFontRenderer getDensityRenderer(float densityScale) {
        int scaledSize = Math.max(size, Math.round(size * densityScale));
        if (scaledSize == size) {
            return this;
        }
        return FPSMaster.fontManager.getFont(scaledSize);
    }

    public float drawStringCapableWithEmojiWithShadow(String text, float x, float y, int color) {
        String[] sbs = new String[]{"\uD83C\uDF89", "\uD83C\uDF81", "\uD83D\uDC79", "\uD83C\uDFC0", "⚽", "\uD83C\uDF6D", "\uD83C\uDF20", "\uD83D\uDC7E", "\uD83D\uDC0D"
                , "\uD83D\uDD2E", "\uD83D\uDC7D", "\uD83D\uDCA3", "\uD83C\uDF6B", "\uD83C\uDF82"};
        for (String sb : sbs) {
            text = text.replaceAll(sb, "");
        }
        return drawStringWithShadow(text, x, y, color);
    }
}






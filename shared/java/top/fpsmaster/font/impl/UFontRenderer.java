package top.fpsmaster.font.impl;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.util.ResourceLocation;
import top.fpsmaster.api.ProviderManager;
import top.fpsmaster.modules.client.GlobalTextFilter;
import top.fpsmaster.modules.logger.ClientLogger;
import top.fpsmaster.utils.os.FileUtils;

import java.awt.*;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;

import static top.fpsmaster.utils.render.Render2DUtils.intToColor;

public class UFontRenderer extends FontRenderer {
    private final int FONT_HEIGHT = 8;
    private StringCache stringCache;

    public UFontRenderer(String name, int size) {
        super(
                Minecraft.getMinecraft().gameSettings,
                new ResourceLocation("textures/font/ascii.png"),
                Minecraft.getMinecraft().getTextureManager(),
                false
        );
        boolean antiAlias = true;
        Font font;
        try {
            InputStream is = Files.newInputStream(new File(FileUtils.fonts, name + ".ttf").toPath());
            font = Font.createFont(0, is);
            font = font.deriveFont(Font.PLAIN, size);
        } catch (Exception ex) {
            ClientLogger.error("Error loading font " + name);
            font = new Font("Arial", Font.PLAIN, size);
        }

        ResourceLocation res = new ResourceLocation("textures/font/ascii.png");
        int[] colorCode = new int[32];
        for (int i = 0; i <= 31; i++) {
            int j = (i >> 3 & 1) * 85;
            int k = (i >> 2 & 1) * 170 + j;
            int l = (i >> 1 & 1) * 170 + j;
            int i1 = (i & 1) * 170 + j;
            if (i == 6) {
                k += 85;
            }
            if (Minecraft.getMinecraft().gameSettings.anaglyph) {
                int j1 = (k * 30 + l * 59 + i1 * 11) / 100;
                int k1 = (k * 30 + l * 70) / 100;
                int l1 = (k * 30 + i1 * 70) / 100;
                k = j1;
                l = k1;
                i1 = l1;
            }
            if (i >= 16) {
                k /= 4;
                l /= 4;
                i1 /= 4;
            }
            colorCode[i] = (k & 255) << 16 | (l & 255) << 8 | (i1 & 255);
        }

        if (ProviderManager.utilityProvider.getResourcePath(res).equalsIgnoreCase("textures/font/ascii.png")) {
            stringCache = new StringCache(colorCode);
            stringCache.setDefaultFont(font, size, antiAlias);
        }
    }

    /**
     * Draws the specified string with a shadow.
     */
    @Override
    public int drawStringWithShadow(String text, float x, float y, int color) {
        Color color1 = intToColor(color);
        this.drawString(text, x, y, new Color(color1.getRed(), color1.getGreen(), color1.getBlue(), color1.getAlpha()).getRGB(), true);
        return getStringWidth(text);
    }


    public String trimStringToWidth(String text, float width) {
        return trimString(text, width, false);
    }

    public String trimString(String text, float width, boolean reverse) {
        StringBuilder stringbuilder = new StringBuilder();
        for (char c : text.toCharArray()) {
            if (getStringWidth(stringbuilder.toString()) < width)
                stringbuilder.append(c);
            else
                break;
        }
        return stringbuilder.toString();
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
        int i;
        if (dropShadow) {
            if (intToColor(color).getAlpha() > 50) {
                stringCache.renderString(
                        text,
                        x + 0.5f,
                        y + 0.5f,
                        new Color(20, 20, 20, intToColor(color).getAlpha()).getRGB(),
                        true
                );
            }
        }
        i = stringCache.renderString(text, x, y, color, false);
        return i;
    }

    @Override
    public int getStringWidth(String text) {
        text = GlobalTextFilter.filter(text);
        return stringCache.getStringWidth(text);
    }

    public void drawCenteredString(String text, float x, float y, int color) {
        drawString(text, x - stringCache.getStringWidth(text) / 2f, y, color, false);
    }

    public int getHeight() {
        return stringCache.height / 2;
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

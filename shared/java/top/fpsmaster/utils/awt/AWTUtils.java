package top.fpsmaster.utils.awt;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.util.ResourceLocation;
import top.fpsmaster.modules.logger.ClientLogger;

import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.HashMap;

public class AWTUtils {
    private static final HashMap<Integer, ResourceLocation[]> generated = new HashMap<>();
    private static final HashMap<String, ResourceLocation> generatedFull = new HashMap<>();

    public static ResourceLocation generateRoundImage(int width, int height, int radius, Color borderColor, int borderWidth) {
        if (width <= 0 || height <= 0 || radius < 0 || borderWidth < 0) {
            throw new IllegalArgumentException("Width, height must be positive; radius and borderWidth must be non-negative");
        }

        String key = width + "/" + height + "/" + radius + "/" + borderWidth + "/" + borderColor.getRGB();
        return generatedFull.computeIfAbsent(key, r -> {
            int scaledWidth = width * 2;
            int scaledHeight = height * 2;
            int scaledRadius = radius * 2;
            int scaledBorderWidth = borderWidth * 2;

            try {
                BufferedImage bufferedImage = new BufferedImage(scaledWidth, scaledHeight, BufferedImage.TYPE_INT_ARGB);
                Graphics2D graphics2D = bufferedImage.createGraphics();

                graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // Clear with transparent background
                graphics2D.setColor(new Color(0, 0, 0, 0));
                graphics2D.fillRect(0, 0, scaledWidth, scaledHeight);

                graphics2D.setComposite(AlphaComposite.SrcOver);

                // Draw the border if borderWidth > 0
                if (borderWidth > 0 && borderColor != null) {
                    graphics2D.setColor(borderColor);
                    RoundRectangle2D borderRect = new RoundRectangle2D.Float(
                            0,
                            0,
                            scaledWidth,
                            scaledHeight,
                            scaledRadius,
                            scaledRadius
                    );
                    graphics2D.fill(borderRect);
                }

                // Draw the inner white rectangle (accounting for border)
                int innerX = scaledBorderWidth;
                int innerY = scaledBorderWidth;
                int innerWidth = scaledWidth - (scaledBorderWidth * 2);
                int innerHeight = scaledHeight - (scaledBorderWidth * 2);
                int innerRadius = Math.max(0, scaledRadius - (scaledBorderWidth * 2));

                graphics2D.setColor(Color.WHITE);
                RoundRectangle2D innerRect = new RoundRectangle2D.Float(
                        innerX,
                        innerY,
                        innerWidth,
                        innerHeight,
                        innerRadius,
                        innerRadius
                );
                graphics2D.fill(innerRect);

                Minecraft mc = Minecraft.getMinecraft();
                if (mc == null || mc.getTextureManager() == null) {
                    return null;
                }
                graphics2D.dispose();

                return mc.getTextureManager()
                        .getDynamicTextureLocation(r + "_full", new DynamicTexture(bufferedImage));
            } catch (Exception e) {
                ClientLogger.error("An error occurred while generating round texture: " + r);
                e.printStackTrace();
                return null;
            }
        });
    }

    public static ResourceLocation generateRoundImage(int width, int height, int radius) {
        if (width <= 0 || height <= 0 || radius < 0) {
            throw new IllegalArgumentException("Width, height must be positive and radius must be non-negative");
        }
        return generatedFull.computeIfAbsent(width + "/" + height + "/" + radius, r -> {
            int scaledWidth = width * 2;
            int scaledHeight = height * 2;

            try {
                BufferedImage bufferedImage = new BufferedImage(scaledWidth, scaledHeight, BufferedImage.TYPE_INT_ARGB);
                Graphics2D graphics2D = bufferedImage.createGraphics();

                graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                graphics2D.setColor(new Color(0, 0, 0, 0));
                graphics2D.fillRect(0, 0, bufferedImage.getWidth(), bufferedImage.getHeight());

                graphics2D.setComposite(AlphaComposite.SrcOver);
                graphics2D.setColor(Color.WHITE); // 白色圆角矩形
                RoundRectangle2D roundRectangle = new RoundRectangle2D.Float(0, 0, scaledWidth, scaledHeight, radius * 2, radius * 2);
                graphics2D.fill(roundRectangle);

                Minecraft mc = Minecraft.getMinecraft();
                if (mc == null || mc.getTextureManager() == null) {
                    return null;
                }
                graphics2D.dispose();

                return mc.getTextureManager()
                        .getDynamicTextureLocation(r + "_full", new DynamicTexture(bufferedImage));
            } catch (Exception e) {
                ClientLogger.error("An error occurred while generating round texture: " + r);
                e.printStackTrace();
                return null;
            }
        });
    }

    public static ResourceLocation[] generateRound(int radius) {
        if (generated.get(radius) != null) {
            return generated.get(radius);
        }

        if (radius <= 0)
            radius = 1;
        try {
            String[] fileNames = {"lt.png", "rt.png", "lb.png", "rb.png"}; // 存储文件名
            int radius2 = radius * 2;

            BufferedImage bufferedImage = new BufferedImage(radius2, radius2, BufferedImage.TYPE_INT_ARGB);
            java.awt.Graphics2D graphics2D = bufferedImage.createGraphics();
            graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics2D.setColor(Color.decode("#00000000"));
            graphics2D.fillRect(0, 0, bufferedImage.getWidth(), bufferedImage.getHeight());

            RoundRectangle2D roundRectangle;

            int[] coordinates = {0, -radius2, 0, -radius2};
            int[] coordinates2 = {0, 0, -radius2, -radius2};
            ResourceLocation[] locations = new ResourceLocation[4];
            for (int i = 0; i < 4; i++) {
                graphics2D.setComposite(AlphaComposite.Clear);
                graphics2D.fillRect(0, 0, radius2, radius2);
                graphics2D.setComposite(AlphaComposite.SrcOver);
                graphics2D.setColor(Color.WHITE);
                roundRectangle = new RoundRectangle2D.Float(
                        coordinates[i],
                        coordinates2[i],
                        (radius2 * 2),
                        (radius2 * 2),
                        radius2,
                        radius2
                );
                graphics2D.fill(roundRectangle);

                locations[i] = Minecraft.getMinecraft().getTextureManager().getDynamicTextureLocation(radius + "_" + fileNames[i], new DynamicTexture(bufferedImage));
            }

            graphics2D.dispose();
            generated.put(radius, locations);
        } catch (Exception exception) {
            exception.printStackTrace();
        }
        return generated.get(radius);
    }
}

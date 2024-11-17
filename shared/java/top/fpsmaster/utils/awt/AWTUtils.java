package top.fpsmaster.utils.awt;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.util.ResourceLocation;

import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.HashMap;

public class AWTUtils {
    private static HashMap<Integer, ResourceLocation[]> generated = new HashMap<>();

    public static ResourceLocation[] generateRound(int radius) {
        if (generated.get(radius) != null) {
            return generated.get(radius) ;
        }

        try {
            String[] fileNames = {"lt.png", "rt.png", "lb.png", "rb.png"}; // 存储文件名
            int radius2 = radius * 2;

            BufferedImage bufferedImage = new BufferedImage(radius2, radius2, BufferedImage.TYPE_INT_ARGB);
            java.awt.Graphics2D graphics2D = bufferedImage.createGraphics();
            graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics2D.setColor(Color.decode("#00000000"));
            graphics2D.fillRect(0, 0, bufferedImage.getWidth(), bufferedImage.getHeight());

            RoundRectangle2D roundRectangle;
            File outputfile;

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

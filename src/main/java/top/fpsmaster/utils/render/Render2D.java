package top.fpsmaster.utils.render;

import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.util.ResourceLocation;
import top.fpsmaster.utils.render.draw.Colors;
import top.fpsmaster.utils.render.draw.Hover;
import top.fpsmaster.utils.render.draw.Images;
import top.fpsmaster.utils.render.draw.Rects;
import top.fpsmaster.utils.render.state.Alpha;
import top.fpsmaster.utils.render.state.Blend;
import top.fpsmaster.utils.render.types.Bounding;

import java.awt.*;

public class Render2D {

    public static void alpha(float value) {
        Alpha.set(value);
    }

    public static int colorAlpha(int color, int alpha) {
        return Colors.alpha(Colors.toColor(color), alpha).getRGB();
    }

    public static Color colorAlpha(Color color, int alpha) {
        return Colors.alpha(color, alpha);
    }

    public static void rect(float x, float y, float width, float height, int color) {
        Rects.fill(x, y, width, height, color);
    }

    public static void rect(float x, float y, float width, float height, Color color) {
        Rects.fill(x, y, width, height, color);
    }

    public static void rounded(float x, float y, float width, float height, int radius, int color) {
        Rects.rounded(x, y, width, height, radius, color);
    }

    public static void rounded(float x, float y, float width, float height, int radius, Color color) {
        Rects.rounded(x, y, width, height, radius, color);
    }

    public static void roundedBorder(float x, float y, float width, float height, int radius, float lineWidth, int fill, int border) {
        Rects.roundedBorder(x, y, width, height, radius, lineWidth, fill, border);
    }

    public static void image(ResourceLocation res, float x, float y, float width, float height, int color) {
        Images.draw(res, x, y, width, height, color);
    }

    public static void image(ResourceLocation res, float x, float y, float width, float height, Color color) {
        Images.draw(res, x, y, width, height, color);
    }

    public static void image(ResourceLocation res, float x, float y, float width, float height) {
        Images.draw(res, x, y, width, height);
    }

    public static void roundedImage(float x, float y, float width, float height, int radius, Color color) {
        Rects.roundedImage(x, y, width, height, radius, color);
    }

    public static void playerHead(AbstractClientPlayer player, float x, float y, int w, int h) {
        Images.playerHead(player, x, y, w, h);
    }

    public static boolean hover(float x, float y, float width, float height, int mouseX, int mouseY) {
        return Hover.is(x, y, width, height, mouseX, mouseY);
    }

    public static boolean hover(Bounding bounding, int mouseX, int mouseY) {
        return Hover.is(bounding, mouseX, mouseY);
    }

    public static void beginBlend() {
        Blend.begin();
    }

    public static void endBlend() {
        Blend.end();
    }
}


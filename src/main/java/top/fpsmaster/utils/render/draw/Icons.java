package top.fpsmaster.utils.render.draw;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.util.ResourceLocation;
import top.fpsmaster.utils.render.gui.UiScale;

import java.util.HashMap;
import java.util.Map;

/**
 * 多尺寸线性图标绘制入口。
 *
 * 图标以 SVG 维护（docs/icons/svg/），离线烘焙为 24/48/96 三档白色透明 PNG，
 * 位于 assets/minecraft/client/gui/settings/icons/<size>/<name>.png。
 * 绘制时按"逻辑尺寸 x 实际像素密度"选择不小于所需像素的最小档位，
 * 避免大图缩小采样发虚、小图放大出锯齿。档位需与 docs/icons/Bake.java 一致。
 */
public final class Icons {
    private static final int[] SIZES = {24, 48, 96};
    private static final Map<String, ResourceLocation> CACHE = new HashMap<>();

    private Icons() {
    }

    public static void draw(String name, float x, float y, float size, int color) {
        Images.drawSmooth(resolve(name, size), x, y, size, size, color);
    }

    private static ResourceLocation resolve(String name, float drawSize) {
        float devicePixels = drawSize * pixelsPerUnit();
        int picked = SIZES[SIZES.length - 1];
        for (int size : SIZES) {
            if (size >= devicePixels) {
                picked = size;
                break;
            }
        }
        String key = picked + "/" + name;
        ResourceLocation cached = CACHE.get(key);
        if (cached == null) {
            cached = new ResourceLocation("client/gui/settings/icons/" + key + ".png");
            CACHE.put(key, cached);
        }
        return cached;
    }

    private static float pixelsPerUnit() {
        if (UiScale.isActive() && UiScale.getGuiWidth() > 0) {
            return UiScale.getDisplayWidth() / UiScale.getGuiWidth();
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null) {
            return 2f;
        }
        return new ScaledResolution(mc).getScaleFactor();
    }
}

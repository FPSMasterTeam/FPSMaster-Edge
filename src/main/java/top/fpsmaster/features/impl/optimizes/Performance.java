package top.fpsmaster.features.impl.optimizes;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.world.World;
import top.fpsmaster.features.manager.Category;
import top.fpsmaster.features.manager.Module;
import top.fpsmaster.features.settings.impl.BooleanSetting;
import top.fpsmaster.features.settings.impl.NumberSetting;

public class Performance extends Module {

    public static boolean using = false;

    public static BooleanSetting ignoreStands = new BooleanSetting("IgnoreStands", true);
    public static BooleanSetting entitiesOptimize = new BooleanSetting("EntitiesOptimize", false);
    public static BooleanSetting fastLoad = new BooleanSetting("FastLoad", true);
    public static BooleanSetting fontOptimize = new BooleanSetting("FontOptimize", false);
    public static BooleanSetting staticParticleColor = new BooleanSetting("StaticParticleColor", true);
    public static BooleanSetting limitChunks = new BooleanSetting("LimitChunks", true);
    public static BooleanSetting batchModelRendering = new BooleanSetting("BatchModelRendering", true);
    public static BooleanSetting lowAnimationTick = new BooleanSetting("LowAnimationTick", true);

    public static NumberSetting chunkUpdateLimit = new NumberSetting("ChunkUpdateLimit", 50, 0, 250, 1);
    public static NumberSetting fpsLimit = new NumberSetting("FPSLimit", 30, 0, 360, 1);
    public static NumberSetting entityLimit = new NumberSetting("EntityLimit", 200, 0, 800, 1);
    public static NumberSetting particlesLimit = new NumberSetting("ParticlesLimit", 400, 0, 2000, 1);

    public Performance() {
        super("Performance", Category.OPTIMIZE);
        addSettings(ignoreStands, entitiesOptimize, fastLoad, batchModelRendering, lowAnimationTick, entityLimit, fpsLimit, particlesLimit, fontOptimize, staticParticleColor, limitChunks, chunkUpdateLimit);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        using = true;
    }

    @Override
    public void onDisable() {
        super.onDisable();
        using = false;
    }

    public static boolean isUsing() {
        return using;
    }

    public static void setUsing(boolean using) {
        Performance.using = using;
    }

    public static boolean isVisible(CheckEntity entity) {
        return true;
    }

    public static boolean isVisible(
            World world,
            double minX, double minY, double minZ,
            double maxX, double maxY, double maxZ,
            double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    public static boolean shouldUseFontOptimize(FontRenderer fontRenderer, String text) {
        if (!using) {
            return false;
        }

        if (!fontOptimize.getValue()) {
            return false;
        }

        if (fontRenderer == null) {
            return false;
        }

        if (text == null || text.isEmpty()) {
            return false;
        }

        if (fontRenderer.getUnicodeFlag()) {
            return false;
        }

        Minecraft mc = Minecraft.getMinecraft();

        if (mc == null) {
            return false;
        }

        if (mc.getLanguageManager() != null && mc.getLanguageManager().isCurrentLocaleUnicode()) {
            return false;
        }

        boolean hasRenderableCharacter = false;

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);

            if (c == '\u00A7') {
                if (i + 1 >= text.length()) {
                    return false;
                }

                char code = Character.toLowerCase(text.charAt(i + 1));

                if ("0123456789abcdefklmnor".indexOf(code) == -1) {
                    return false;
                }

                i++;
                continue;
            }

            if (Character.isHighSurrogate(c) || Character.isLowSurrogate(c)) {
                return false;
            }

            if (c > 255) {
                return false;
            }

            if (c < 32 && c != '\n' && c != '\r' && c != '\t') {
                return false;
            }

            hasRenderableCharacter = true;
        }

        return hasRenderableCharacter;
    }
}

package top.fpsmaster.features.impl.optimizes;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.world.World;
import org.lwjgl.opengl.GL11;
import top.fpsmaster.features.manager.Category;
import top.fpsmaster.features.manager.Module;
import top.fpsmaster.features.settings.impl.BooleanSetting;
import top.fpsmaster.features.settings.impl.NumberSetting;

import java.util.Locale;

public class Performance extends Module {

    public static boolean using = false;

    public static BooleanSetting ignoreStands = new BooleanSetting("IgnoreStands", true);
    public static BooleanSetting entitiesOptimize = new BooleanSetting("EntitiesOptimize", false);
    public static BooleanSetting fastLoad = new BooleanSetting("FastLoad", true);
    public static BooleanSetting fontOptimize = new BooleanSetting("FontOptimize", false);
    public static BooleanSetting disableFontOptimizeOnBadRenderer = new BooleanSetting("DisableFontOptimizeOnBadRenderer", true);
    public static BooleanSetting staticParticleColor = new BooleanSetting("StaticParticleColor", true);
    public static BooleanSetting limitChunks = new BooleanSetting("LimitChunks", true);
    public static BooleanSetting batchModelRendering = new BooleanSetting("BatchModelRendering", true);
    public static BooleanSetting lowAnimationTick = new BooleanSetting("LowAnimationTick", true);

    public static NumberSetting chunkUpdateLimit = new NumberSetting("ChunkUpdateLimit", 50, 0, 250, 1);
    public static NumberSetting fpsLimit = new NumberSetting("FPSLimit", 30, 0, 360, 1);
    public static NumberSetting entityLimit = new NumberSetting("EntityLimit", 200, 0, 800, 1);
    public static NumberSetting particlesLimit = new NumberSetting("ParticlesLimit", 400, 0, 2000, 1);

    private static boolean fontOptimizeDirty = true;
    private static boolean fontOptimizeAvailable = false;
    private static boolean glEnvironmentDetected = false;
    private static boolean badFontOptimizeEnvironment = false;
    private static boolean lastUsing = false;
    private static boolean lastFontOptimize = false;
    private static boolean lastDisableOnBadRenderer = true;
    private static boolean lastUnicodeFlag = false;
    private static boolean lastLocaleUnicode = false;
    private static String fontOptimizeBlockReason = "";

    public Performance() {
        super("Performance", Category.OPTIMIZE);
        addSettings(ignoreStands, entitiesOptimize, fastLoad, batchModelRendering, lowAnimationTick, entityLimit, fpsLimit, particlesLimit, fontOptimize, disableFontOptimizeOnBadRenderer, fontOptimize, staticParticleColor, limitChunks, chunkUpdateLimit);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        using = true;
        markFontOptimizeDirty();
    }

    @Override
    public void onDisable() {
        super.onDisable();
        using = false;
        markFontOptimizeDirty();
    }

    public static boolean isUsing() {
        return using;
    }

    public static void setUsing(boolean using) {
        Performance.using = using;
        markFontOptimizeDirty();
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

    public static void markFontOptimizeDirty() {
        fontOptimizeDirty = true;
    }

    public static boolean shouldUseFontOptimize(FontRenderer fontRenderer) {
        boolean currentUsing = using;
        boolean currentFontOptimize = fontOptimize.getValue();
        boolean currentDisableOnBadRenderer = disableFontOptimizeOnBadRenderer.getValue();
        boolean currentUnicodeFlag = fontRenderer != null && fontRenderer.getUnicodeFlag();
        boolean currentLocaleUnicode = Minecraft.getMinecraft().getLanguageManager() != null && Minecraft.getMinecraft().getLanguageManager().isCurrentLocaleUnicode();

        if (fontOptimizeDirty
                || lastUsing != currentUsing
                || lastFontOptimize != currentFontOptimize
                || lastDisableOnBadRenderer != currentDisableOnBadRenderer
                || lastUnicodeFlag != currentUnicodeFlag
                || lastLocaleUnicode != currentLocaleUnicode) {
            refreshFontOptimizeState(fontRenderer, currentUsing, currentFontOptimize, currentDisableOnBadRenderer, currentUnicodeFlag, currentLocaleUnicode);
        }

        return fontOptimizeAvailable;
    }

    private static void refreshFontOptimizeState(FontRenderer fontRenderer, boolean currentUsing, boolean currentFontOptimize, boolean currentDisableOnBadRenderer, boolean currentUnicodeFlag, boolean currentLocaleUnicode) {
        lastUsing = currentUsing;
        lastFontOptimize = currentFontOptimize;
        lastDisableOnBadRenderer = currentDisableOnBadRenderer;
        lastUnicodeFlag = currentUnicodeFlag;
        lastLocaleUnicode = currentLocaleUnicode;
        fontOptimizeDirty = false;

        if (!currentUsing) {
            fontOptimizeAvailable = false;
            fontOptimizeBlockReason = "module disabled";
            return;
        }

        if (!currentFontOptimize) {
            fontOptimizeAvailable = false;
            fontOptimizeBlockReason = "setting disabled";
            return;
        }

        if (fontRenderer == null) {
            fontOptimizeAvailable = false;
            fontOptimizeBlockReason = "font renderer unavailable";
            return;
        }

        if (currentDisableOnBadRenderer && isBadFontOptimizeEnvironment()) {
            fontOptimizeAvailable = false;
            return;
        }

        fontOptimizeAvailable = true;
        fontOptimizeBlockReason = "";
    }

    private static boolean isBadFontOptimizeEnvironment() {
        if (!glEnvironmentDetected) {
            detectFontOptimizeEnvironment();
        }

        return badFontOptimizeEnvironment;
    }

    private static void detectFontOptimizeEnvironment() {
        glEnvironmentDetected = true;
        badFontOptimizeEnvironment = false;
        fontOptimizeBlockReason = "";

        String override = System.getProperty("fpsmaster.fontOptimize", "").trim().toLowerCase(Locale.ENGLISH);

        if ("force".equals(override) || "on".equals(override) || "true".equals(override)) {
            badFontOptimizeEnvironment = false;
            fontOptimizeBlockReason = "";
            return;
        }

        if ("off".equals(override) || "false".equals(override) || "disable".equals(override) || "disabled".equals(override)) {
            badFontOptimizeEnvironment = true;
            fontOptimizeBlockReason = "disabled by system property";
            return;
        }

        String vendor = getGlString(GL11.GL_VENDOR);
        String renderer = getGlString(GL11.GL_RENDERER);
        String version = getGlString(GL11.GL_VERSION);
        String extensions = getGlString(GL11.GL_EXTENSIONS);

        String all = (vendor + " " + renderer + " " + version + " " + extensions).toLowerCase(Locale.ENGLISH);

        if (all.contains("gl4es") || all.contains("libgl4es")) {
            badFontOptimizeEnvironment = true;
            fontOptimizeBlockReason = "blocked renderer: gl4es";
            return;
        }

        if (all.contains("opengl es") && all.contains("wrapper")) {
            badFontOptimizeEnvironment = true;
            fontOptimizeBlockReason = "blocked renderer: OpenGL ES wrapper";
            return;
        }

        if (all.contains("angle") && all.contains("opengl es")) {
            badFontOptimizeEnvironment = true;
            fontOptimizeBlockReason = "blocked renderer: ANGLE OpenGL ES";
        }
    }

    private static String getGlString(int name) {
        try {
            String value = GL11.glGetString(name);
            return value == null ? "" : value;
        } catch (Throwable throwable) {
            return "";
        }
    }

    public static String getFontOptimizeBlockReason() {
        return fontOptimizeBlockReason;
    }

    public static boolean isBadFontOptimizeRendererDetected() {
        return isBadFontOptimizeEnvironment();
    }
}

package top.fpsmaster.ui.screens.mainmenu;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;
import top.fpsmaster.FPSMaster;
import top.fpsmaster.exception.FileException;
import top.fpsmaster.features.settings.impl.ColorSetting;
import top.fpsmaster.features.settings.impl.utils.CustomColor;
import top.fpsmaster.modules.config.ConfigProfileUtils;
import top.fpsmaster.modules.logger.ClientLogger;
import top.fpsmaster.prism.screen.BackgroundsBridge;
import top.fpsmaster.prism.screen.SharedBackgrounds;
import top.fpsmaster.prism.widget.UiFrame;
import top.fpsmaster.ui.kit.EdgeUi;
import top.fpsmaster.utils.io.FileUtils;
import top.fpsmaster.utils.render.draw.Images;
import top.fpsmaster.utils.render.draw.Rects;
import top.fpsmaster.utils.render.gui.Backgrounds;
import top.fpsmaster.utils.render.gui.ScaledGuiScreen;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.FileDialog;
import java.awt.Frame;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

/** Edge host for the shared Prism background selector. */
public class BackgroundSelector extends ScaledGuiScreen {
    private final GuiScreen parent;
    private final SharedBackgrounds backgrounds = new SharedBackgrounds();
    private final BackgroundsBridge bridge = new EdgeBackgroundsBridge();
    private ResourceLocation customPreviewTexture;
    private long customPreviewLastModified = -1L;
    private float customPreviewWidth = 1f;
    private float customPreviewHeight = 1f;

    public BackgroundSelector() {
        this(new MainMenu());
    }

    public BackgroundSelector(GuiScreen parent) {
        this.parent = parent;
    }

    @Override
    public void render(int mouseX, int mouseY, float partialTicks) {
        Backgrounds.draw((int) guiWidth, (int) guiHeight, mouseX, mouseY, partialTicks, (int) zLevel);
        if (backgrounds.draw(EdgeUi.frame(), bridge)) mc.displayGuiScreen(parent);
    }

    @Override
    public void onGuiClosed() {
        try {
            FPSMaster.configManager.saveConfig(ConfigProfileUtils.getActiveProfileName());
        } catch (FileException e) {
            ClientLogger.error("Failed to save background settings: " + e.getMessage());
        }
        super.onGuiClosed();
    }

    private final class EdgeBackgroundsBridge implements BackgroundsBridge {
        @Override public String i18n(String key) { return FPSMaster.i18n.get(key); }
        @Override public String selected() { return FPSMaster.configManager.configure.background; }
        @Override public void select(String id) { FPSMaster.configManager.configure.background = id; }
        @Override public boolean hasCustom() { return FileUtils.background != null && FileUtils.background.isFile(); }
        @Override public float classicHue() { return FPSMaster.configManager.configure.classicBackgroundHue; }
        @Override public float classicSaturation() { return FPSMaster.configManager.configure.classicBackgroundSaturation; }
        @Override public float classicBrightness() { return FPSMaster.configManager.configure.classicBackgroundBrightness; }
        @Override public float classicAlpha() { return FPSMaster.configManager.configure.classicBackgroundAlpha; }
        @Override public String classicMode() { return FPSMaster.configManager.configure.classicBackgroundMode; }

        @Override
        public void pickCustom() {
            FileDialog dialog = new FileDialog((Frame) null, i18n("backgroundselector.filedialog.title"), FileDialog.LOAD);
            dialog.setFilenameFilter((dir, name) -> {
                String lower = name.toLowerCase();
                return lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg");
            });
            dialog.setVisible(true);
            if (dialog.getFile() == null) return;
            try {
                Files.copy(new File(dialog.getDirectory(), dialog.getFile()).toPath(),
                        FileUtils.background.toPath(), StandardCopyOption.REPLACE_EXISTING);
                select("custom");
            } catch (IOException e) {
                ClientLogger.error("Failed to import custom background: " + e.getMessage());
            }
        }

        @Override
        public void setClassic(float hue, float saturation, float brightness, float alpha, String mode) {
            FPSMaster.configManager.configure.classicBackgroundHue = hue;
            FPSMaster.configManager.configure.classicBackgroundSaturation = saturation;
            FPSMaster.configManager.configure.classicBackgroundBrightness = brightness;
            FPSMaster.configManager.configure.classicBackgroundAlpha = alpha;
            FPSMaster.configManager.configure.classicBackgroundMode = mode;
            try {
                Color resolved = ColorSetting.resolveColor(new CustomColor(hue, saturation, brightness, alpha),
                        ColorSetting.ColorType.valueOf(mode), 0f);
                FPSMaster.configManager.configure.classicBackgroundColor = resolved.getRGB();
            } catch (IllegalArgumentException e) {
                FPSMaster.configManager.configure.classicBackgroundMode = ColorSetting.ColorType.STATIC.name();
            }
        }

        @Override
        public void paintPreview(UiFrame ui, String id, float x, float y, float w, float h) {
            if ("classic".equals(id)) {
                Color color;
                try {
                    color = ColorSetting.resolveColor(new CustomColor(classicHue(), classicSaturation(), classicBrightness(), classicAlpha()),
                            ColorSetting.ColorType.valueOf(classicMode()), 0f);
                } catch (IllegalArgumentException e) {
                    color = new CustomColor(classicHue(), classicSaturation(), classicBrightness(), classicAlpha()).getColor();
                }
                Rects.rounded(x, y, w, h, 5, color.getRGB(), false);
                return;
            }
            if ("shader".equals(id)) {
                GL11.glDisable(GL11.GL_TEXTURE_2D);
                GL11.glBegin(GL11.GL_QUADS);
                GL11.glColor4f(0.10f, 0.23f, 0.43f, 1f); GL11.glVertex2f(x, y);
                GL11.glColor4f(0.20f, 0.40f, 0.70f, 1f); GL11.glVertex2f(x + w, y);
                GL11.glColor4f(0.30f, 0.50f, 0.80f, 1f); GL11.glVertex2f(x + w, y + h);
                GL11.glColor4f(0.15f, 0.30f, 0.55f, 1f); GL11.glVertex2f(x, y + h);
                GL11.glEnd();
                GL11.glEnable(GL11.GL_TEXTURE_2D);
                GL11.glColor4f(1f, 1f, 1f, 1f);
                return;
            }
            if (id.startsWith("panorama_")) {
                Images.draw(new ResourceLocation("client/background/" + id + "/panorama_0.png"), x, y, w, h, -1);
                return;
            }
            if ("custom".equals(id) && drawCustom(x, y, w, h)) return;
            Rects.rounded(x, y, w, h, 5, 0xFF647864, false);
        }
    }

    private boolean drawCustom(float x, float y, float w, float h) {
        File file = FileUtils.background;
        if (file == null || !file.isFile()) return false;
        long modified = file.lastModified();
        if (customPreviewTexture == null || modified != customPreviewLastModified) loadCustom(file, modified);
        if (customPreviewTexture == null) return false;
        float scale = Math.min(w / customPreviewWidth, h / customPreviewHeight);
        float dw = customPreviewWidth * scale;
        float dh = customPreviewHeight * scale;
        Images.draw(customPreviewTexture, x + (w - dw) / 2f, y + (h - dh) / 2f, dw, dh, -1);
        return true;
    }

    private void loadCustom(File file, long modified) {
        try {
            BufferedImage image = ImageIO.read(file);
            if (image == null) throw new IOException("Unsupported image");
            customPreviewWidth = image.getWidth();
            customPreviewHeight = image.getHeight();
            customPreviewTexture = mc.getTextureManager().getDynamicTextureLocation(
                    "fpsmaster_custom_bg_preview", new DynamicTexture(image));
        } catch (IOException e) {
            customPreviewTexture = null;
            ClientLogger.warn("Failed to load custom background preview: " + e.getMessage());
        }
        customPreviewLastModified = modified;
    }
}

package top.fpsmaster.ui.screens.mainmenu;

import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;
import top.fpsmaster.FPSMaster;
import top.fpsmaster.features.manager.Category;
import top.fpsmaster.features.manager.Module;
import top.fpsmaster.features.settings.impl.ColorSetting;
import top.fpsmaster.modules.config.ConfigProfileUtils;
import top.fpsmaster.ui.click.ClickGuiTheme;
import top.fpsmaster.ui.click.UiChrome;
import top.fpsmaster.ui.click.component.ScrollContainer;
import top.fpsmaster.ui.click.modules.impl.ColorSettingRender;
import top.fpsmaster.utils.io.FileUtils;
import top.fpsmaster.utils.math.anim.AnimClock;
import top.fpsmaster.utils.math.anim.Animator;
import top.fpsmaster.utils.math.anim.Easings;
import top.fpsmaster.utils.render.draw.Hover;
import top.fpsmaster.utils.render.draw.Icons;
import top.fpsmaster.utils.render.draw.Images;
import top.fpsmaster.utils.render.draw.Rects;
import top.fpsmaster.utils.render.gui.Backgrounds;
import top.fpsmaster.utils.render.gui.Scissor;
import top.fpsmaster.utils.render.gui.ScaledGuiScreen;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.awt.FileDialog;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import top.fpsmaster.exception.FileException;

public class BackgroundSelector extends ScaledGuiScreen {

    private static final float CARD_HEIGHT = 48f;
    private static final float CARD_GAP = 5f;
    private static final float LIST_TOP_PADDING = 2f;
    private static final float LIST_BOTTOM_PADDING = 5f;
    private static final float CLASSIC_EDITOR_BOTTOM_GAP = CARD_GAP;

    private final Animator openAnimation = new Animator();
    private final AnimClock animClock = new AnimClock();
    private final ScrollContainer scrollContainer = new ScrollContainer();
    private final Module classicColorModule = new Module("backgroundselector", Category.Interface);
    private final ColorSetting classicColorSetting = new ColorSetting(
            "classiccolor",
            new Color(0, 0, 0, 255),
            ColorSetting.ColorType.STATIC,
            ColorSetting.ColorType.WAVE,
            ColorSetting.ColorType.CHROMA,
            ColorSetting.ColorType.RAINBOW
    );
    private final ColorSettingRender classicColorRender = new ColorSettingRender(classicColorModule, classicColorSetting);

    private static final BackgroundOption[] OPTIONS = {
            new BackgroundOption("classic", "backgroundselector.option.classic.name", "backgroundselector.option.classic.desc", Color.BLACK),
            new BackgroundOption("shader", "backgroundselector.option.shader.name", "backgroundselector.option.shader.desc", new Color(50, 100, 180)),
            new BackgroundOption("panorama_1", "backgroundselector.option.panorama_1.name", "backgroundselector.option.panorama_1.desc", new Color(60, 80, 120)),
            new BackgroundOption("panorama_2", "backgroundselector.option.panorama_2.name", "backgroundselector.option.panorama_2.desc", new Color(70, 95, 130)),
            new BackgroundOption("panorama_3", "backgroundselector.option.panorama_3.name", "backgroundselector.option.panorama_3.desc", new Color(80, 110, 140)),
            new BackgroundOption("custom", "backgroundselector.option.custom.name", "backgroundselector.option.custom.desc", new Color(100, 150, 100))
    };

    private ResourceLocation customPreviewTexture;
    private long customPreviewLastModified = -1L;
    private float customPreviewWidth = 1f;
    private float customPreviewHeight = 1f;

    @Override
    public void initGui() {
        super.initGui();
        animClock.reset();
        scrollContainer.setHeight(0f);
        classicColorSetting.setColor(
                FPSMaster.configManager.configure.classicBackgroundHue,
                FPSMaster.configManager.configure.classicBackgroundSaturation,
                FPSMaster.configManager.configure.classicBackgroundBrightness,
                FPSMaster.configManager.configure.classicBackgroundAlpha
        );
        try {
            classicColorSetting.setColorType(ColorSetting.ColorType.valueOf(FPSMaster.configManager.configure.classicBackgroundMode));
        } catch (IllegalArgumentException ignored) {
            classicColorSetting.setColorType(ColorSetting.ColorType.STATIC);
        }
    }

    @Override
    public void onGuiClosed() {
        super.onGuiClosed();
        try {
            FPSMaster.configManager.saveConfig(ConfigProfileUtils.getActiveProfileName());
        } catch (FileException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void render(int mouseX, int mouseY, float partialTicks) {
        Backgrounds.draw((int) guiWidth, (int) guiHeight, mouseX, mouseY, partialTicks, (int) zLevel);

        double dt = animClock.tick();
        if (!openAnimation.isRunning() && openAnimation.get() == 0.0) {
            openAnimation.start(0, 1, 0.4f, Easings.CUBIC_OUT);
        }
        openAnimation.update(dt);
        float alpha = (float) openAnimation.get();

        Rects.fill(0f, 0f, guiWidth, guiHeight, new Color(4, 6, 10, (int) (128 * alpha)));
        if (alpha < 0.05f) {
            return;
        }

        // .bg-selector: width 500px, max-height 78vh — halved to GUI units. The column shrinks
        // to its content like the flex prototype instead of always standing max-height tall.
        float panelWidth = Math.min(250f, guiWidth - 24f);
        int optionRows = (OPTIONS.length + 1) / 2;
        float contentTotal = LIST_TOP_PADDING + optionRows * (CARD_HEIGHT + CARD_GAP) + LIST_BOTTOM_PADDING
                + (isOptionSelected("classic") ? getClassicEditorHeight() + CLASSIC_EDITOR_BOTTOM_GAP : 0f);
        float panelHeight = Math.min(30f + contentTotal + 10f, Math.min(guiHeight * 0.78f, guiHeight - 24f));
        float panelX = (guiWidth - panelWidth) / 2f;
        float panelY = (guiHeight - panelHeight) / 2f;

        UiChrome.panel(panelX, panelY, panelWidth, panelHeight);

        // head: back · title · spacer · pick-image
        float headY = panelY + 8f;
        boolean backHover = Hover.is(panelX + 8f, headY, 15f, 15f, mouseX, mouseY);
        UiChrome.ghostButton(panelX + 8f, headY, 15f, 15f, backHover);
        Icons.draw("back", panelX + 12f, headY + 4f, 7f,
                (backHover ? ClickGuiTheme.textPrimary() : ClickGuiTheme.textSecondary()).getRGB());
        if (consumePressInBounds(panelX + 8f, headY, 15f, 15f, 0) != null) {
            mc.displayGuiScreen(new MainMenu());
        }
        UiChrome.boldString(FPSMaster.fontManager.s16, FPSMaster.i18n.get("backgroundselector.title"),
                panelX + 28f, headY + 3.5f, ClickGuiTheme.textPrimary().getRGB());
        float pickW = FPSMaster.fontManager.getFont(13).getStringWidth(FPSMaster.i18n.get("backgroundselector.pick")) + 16f;
        if (UiChrome.buttonClicked(this, panelX + panelWidth - 10f - pickW, headY, pickW, 15f, null,
                FPSMaster.i18n.get("backgroundselector.pick"), UiChrome.Style.DEFAULT, mouseX, mouseY)) {
            selectCustomImage();
        }

        float contentX = panelX + 10f;
        float contentY = panelY + 30f;
        float contentWidth = panelWidth - 20f;
        float contentHeight = panelHeight - 40f;

        GL11.glPushMatrix();
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        Scissor.apply(contentX, contentY, contentWidth, contentHeight);

        final float cardW = (contentWidth - CARD_GAP) / 2f;
        scrollContainer.draw(this, contentX, contentY, contentWidth, contentHeight, mouseX, mouseY, () -> {
            float scroll = scrollContainer.getScroll();
            for (int i = 0; i < OPTIONS.length; i++) {
                float cx = contentX + (i % 2) * (cardW + CARD_GAP);
                float cy = contentY + LIST_TOP_PADDING + (i / 2) * (CARD_HEIGHT + CARD_GAP) + scroll;
                renderCard(cx, cy, cardW, OPTIONS[i], mouseX, mouseY, alpha);
            }
            int rows = (OPTIONS.length + 1) / 2;
            float totalHeight = LIST_TOP_PADDING + rows * (CARD_HEIGHT + CARD_GAP) + LIST_BOTTOM_PADDING;
            if (isOptionSelected("classic")) {
                float editorY = contentY + LIST_TOP_PADDING + rows * (CARD_HEIGHT + CARD_GAP) + scroll;
                renderClassicColorEditor(contentX, editorY, contentWidth, mouseX, mouseY, alpha);
                totalHeight += getClassicEditorHeight() + CLASSIC_EDITOR_BOTTOM_GAP;
            }
            scrollContainer.setHeight(totalHeight);
        });

        syncClassicBackgroundConfig();

        GL11.glDisable(GL11.GL_SCISSOR_TEST);
        GL11.glPopMatrix();

        handlePendingClick(panelX, panelY, panelWidth, panelHeight);
    }

    private void renderCard(float x, float y, float width, BackgroundOption option, int mouseX, int mouseY, float alpha) {
        boolean selected = isOptionSelected(option.id);
        boolean hovered = Hover.is(x, y, width, CARD_HEIGHT, mouseX, mouseY);

        // preview fills the card, hairline (accent when selected) around it
        Rects.rounded(x - 0.5f, y - 0.5f, width + 1f, CARD_HEIGHT + 1f, UiChrome.CARD_RADIUS + 1,
                (selected ? ClickGuiTheme.accent()
                        : hovered ? ClickGuiTheme.strokeStrong() : ClickGuiTheme.stroke()).getRGB(), false);
        Rects.rounded(x, y, width, CARD_HEIGHT, UiChrome.CARD_RADIUS, new Color(22, 22, 25).getRGB(), false);
        renderPreview(x + 1f, y + 1f, width - 2f, CARD_HEIGHT - 2f, option);

        // bottom label scrim
        float labelH = 13f;
        Rects.rounded(x + 1f, y + CARD_HEIGHT - 1f - labelH, width - 2f, labelH, UiChrome.CARD_RADIUS,
                new Color(0, 0, 0, 166).getRGB(), false);
        FPSMaster.fontManager.getFont(12).drawString(FPSMaster.i18n.get(option.nameKey),
                x + 6f, y + CARD_HEIGHT - 1f - labelH + 3.5f, ClickGuiTheme.textPrimary().getRGB());
        if (selected) {
            float ckX = x + width - 14f;
            float ckY = y + CARD_HEIGHT - 1f - labelH + 2.5f;
            Rects.rounded(ckX, ckY, 8f, 8f, 4, ClickGuiTheme.accent().getRGB(), false);
            Icons.draw("check", ckX + 1.5f, ckY + 1.5f, 5f, 0xFFFFFFFF);
        }
    }

    private void renderPreview(float x, float y, float w, float h, BackgroundOption option) {
        if ("classic".equals(option.id)) {
            Rects.rounded(Math.round(x), Math.round(y), Math.round(w), Math.round(h), 5, classicColorSetting.updateAndGetColor());
            return;
        }
        if ("shader".equals(option.id)) {
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            GL11.glBegin(GL11.GL_QUADS);
            GL11.glColor4f(0.10f, 0.23f, 0.43f, 1.0f);
            GL11.glVertex2f(x, y);
            GL11.glColor4f(0.20f, 0.40f, 0.70f, 1.0f);
            GL11.glVertex2f(x + w, y);
            GL11.glColor4f(0.30f, 0.50f, 0.80f, 1.0f);
            GL11.glVertex2f(x + w, y + h);
            GL11.glColor4f(0.15f, 0.30f, 0.55f, 1.0f);
            GL11.glVertex2f(x, y + h);
            GL11.glEnd();
            GL11.glEnable(GL11.GL_TEXTURE_2D);
            return;
        }
        if (isPanoramaOption(option.id)) {
            Images.draw(new ResourceLocation("client/background/" + option.id + "/panorama_0.png"), x, y, w, h, -1);
            return;
        }
        if ("custom".equals(option.id) && renderCustomPreview(x, y, w, h)) {
            return;
        }
        Rects.rounded(Math.round(x), Math.round(y), Math.round(w), Math.round(h), 5, option.previewColor);
        FPSMaster.fontManager.s14.drawCenteredString(FPSMaster.i18n.get("backgroundselector.preview.image"), x + w / 2f, y + h / 2f - 4f, Color.WHITE.getRGB());
    }

    private boolean renderCustomPreview(float x, float y, float w, float h) {
        File file = FileUtils.background;
        if (file == null || !file.exists()) {
            return false;
        }
        long modified = file.lastModified();
        if (customPreviewTexture == null || customPreviewLastModified != modified) {
            loadCustomPreview(file, modified);
        }
        if (customPreviewTexture == null) {
            return false;
        }

        float scale = Math.min(w / customPreviewWidth, h / customPreviewHeight);
        float drawWidth = customPreviewWidth * scale;
        float drawHeight = customPreviewHeight * scale;
        Images.draw(customPreviewTexture, x + (w - drawWidth) * 0.5f, y + (h - drawHeight) * 0.5f, drawWidth, drawHeight, -1);
        return true;
    }

    private void loadCustomPreview(File file, long modified) {
        try {
            BufferedImage image = ImageIO.read(file);
            if (image == null || image.getWidth() <= 0 || image.getHeight() <= 0) {
                customPreviewTexture = null;
                customPreviewLastModified = modified;
                return;
            }
            customPreviewWidth = image.getWidth();
            customPreviewHeight = image.getHeight();
            customPreviewTexture = mc.getTextureManager().getDynamicTextureLocation("fpsmaster_custom_bg_preview", new DynamicTexture(image));
            customPreviewLastModified = modified;
        } catch (IOException exception) {
            customPreviewTexture = null;
            customPreviewLastModified = modified;
        }
    }

    private void renderClassicColorEditor(float x, float y, float width, int mouseX, int mouseY, float alpha) {
        float editorHeight = getClassicEditorHeight();
        Rects.rounded(x, y, width, editorHeight, UiChrome.CARD_RADIUS,
                ClickGuiTheme.layer().getRGB(), false);
        classicColorRender.render(this, x + 3f, y + 3f, width - 6f, 12f, mouseX, mouseY, true);
    }

    private void syncClassicBackgroundConfig() {
        Color resolved = classicColorSetting.updateAndGetColor();
        FPSMaster.configManager.configure.classicBackgroundColor = resolved.getRGB();
        FPSMaster.configManager.configure.classicBackgroundHue = classicColorSetting.getValue().hue;
        FPSMaster.configManager.configure.classicBackgroundSaturation = classicColorSetting.getValue().saturation;
        FPSMaster.configManager.configure.classicBackgroundBrightness = classicColorSetting.getValue().brightness;
        FPSMaster.configManager.configure.classicBackgroundAlpha = classicColorSetting.getValue().alpha;
        FPSMaster.configManager.configure.classicBackgroundMode = classicColorSetting.getColorType().name();
    }

    private float getClassicEditorHeight() {
        return Math.max(32f, classicColorRender.height + 8f);
    }

    private void handlePendingClick(float panelX, float panelY, float panelWidth, float panelHeight) {
        float contentX = panelX + 10f;
        float contentY = panelY + 30f;
        float contentWidth = panelWidth - 20f;
        float contentHeight = panelHeight - 40f;

        ScaledGuiScreen.PointerEvent click = consumePressInBounds(contentX, contentY, contentWidth, contentHeight, 0);
        if (click != null) {
            float cardW = (contentWidth - CARD_GAP) / 2f;
            float scroll = scrollContainer.getScroll();
            for (int i = 0; i < OPTIONS.length; i++) {
                float cx = contentX + (i % 2) * (cardW + CARD_GAP);
                float cy = contentY + LIST_TOP_PADDING + (i / 2) * (CARD_HEIGHT + CARD_GAP) + scroll;
                if (Hover.is(cx, cy, cardW, CARD_HEIGHT, click.x, click.y)) {
                    FPSMaster.configManager.configure.background = OPTIONS[i].id;
                    return;
                }
            }
            return;
        }
        // click outside the panel dismisses the modal
        if (consumePressOutside(panelX, panelY, panelWidth, panelHeight) != null) {
            mc.displayGuiScreen(new MainMenu());
        }
    }

    private void selectCustomImage() {
        try {
            FileDialog fileDialog = new FileDialog((Frame) null, FPSMaster.i18n.get("backgroundselector.filedialog.title"), FileDialog.LOAD);
            fileDialog.setFilenameFilter((dir, name) -> {
                String lower = name.toLowerCase();
                return lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg");
            });
            fileDialog.setVisible(true);

            if (fileDialog.getFile() == null) {
                return;
            }

            File selectedFile = new File(fileDialog.getDirectory(), fileDialog.getFile());
            Files.copy(selectedFile.toPath(), top.fpsmaster.utils.io.FileUtils.background.toPath(), StandardCopyOption.REPLACE_EXISTING);
            FPSMaster.configManager.configure.background = "custom";
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static class BackgroundOption {
        final String id;
        final String nameKey;
        final String descKey;
        final Color previewColor;

        BackgroundOption(String id, String nameKey, String descKey, Color previewColor) {
            this.id = id;
            this.nameKey = nameKey;
            this.descKey = descKey;
            this.previewColor = previewColor;
        }
    }

    private boolean isOptionSelected(String optionId) {
        String current = FPSMaster.configManager.configure.background;
        if ("panorama_1".equals(optionId) && "panorama".equals(current)) {
            return true;
        }
        return optionId.equals(current);
    }

    private boolean isPanoramaOption(String optionId) {
        return optionId.startsWith("panorama_");
    }
}

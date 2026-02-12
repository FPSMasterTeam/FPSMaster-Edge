package top.fpsmaster.ui.screens.mainmenu;

import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;
import top.fpsmaster.FPSMaster;
import top.fpsmaster.features.manager.Category;
import top.fpsmaster.features.manager.Module;
import top.fpsmaster.features.settings.impl.ColorSetting;
import top.fpsmaster.ui.click.component.ScrollContainer;
import top.fpsmaster.ui.click.modules.impl.ColorSettingRender;
import top.fpsmaster.ui.common.GuiButton;
import top.fpsmaster.utils.math.anim.AnimClock;
import top.fpsmaster.utils.math.anim.Animator;
import top.fpsmaster.utils.math.anim.Easings;
import top.fpsmaster.utils.render.draw.Hover;
import top.fpsmaster.utils.render.draw.Images;
import top.fpsmaster.utils.render.draw.Rects;
import top.fpsmaster.utils.render.gui.Backgrounds;
import top.fpsmaster.utils.render.gui.Scissor;
import top.fpsmaster.utils.render.gui.ScaledGuiScreen;

import java.awt.*;
import java.io.File;
import java.awt.FileDialog;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class BackgroundSelector extends ScaledGuiScreen {

    private static final float CARD_HEIGHT = 75f;
    private static final float CARD_GAP = 8f;
    private static final float LIST_TOP_PADDING = 6f;
    private static final float LIST_BOTTOM_PADDING = 6f;
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

    private final GuiButton backButton = new GuiButton("mainmenu.back", () -> mc.displayGuiScreen(new MainMenu()),
            new Color(0, 0, 0, 140), new Color(113, 127, 254));

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
    public void render(int mouseX, int mouseY, float partialTicks) {
        Backgrounds.draw((int) (guiWidth * scaleFactor), (int) (guiHeight * scaleFactor), mouseX, mouseY, partialTicks, (int) zLevel);

        double dt = animClock.tick();
        if (!openAnimation.isRunning() && openAnimation.get() == 0.0) {
            openAnimation.start(0, 1, 0.4f, Easings.CUBIC_OUT);
        }
        openAnimation.update(dt);
        float alpha = (float) openAnimation.get();

        Rects.fill(0f, 0f, guiWidth, guiHeight, new Color(0, 0, 0, (int) (170 * alpha)));
        if (alpha < 0.05f) {
            return;
        }

        float panelWidth = Math.min(410f, guiWidth - 40f);
        float panelHeight = Math.min(320f, guiHeight - 40f);
        float panelX = (guiWidth - panelWidth) / 2f;
        float panelY = (guiHeight - panelHeight) / 2f;

        Rects.rounded(Math.round(panelX), Math.round(panelY), Math.round(panelWidth), Math.round(panelHeight), 12, new Color(24, 24, 28, (int) (240 * alpha)));
        Rects.rounded(Math.round(panelX), Math.round(panelY), Math.round(panelWidth), 44, 12, new Color(30, 30, 36, (int) (210 * alpha)));
        FPSMaster.fontManager.s20.drawCenteredString(FPSMaster.i18n.get("backgroundselector.title"), panelX + panelWidth / 2f, panelY + 24f, new Color(255, 255, 255, (int) (255 * alpha)).getRGB());

        backButton.renderInScreen(this, panelX + panelWidth - 68f, panelY + panelHeight - 34f, 56f, 22f, mouseX, mouseY);

        float contentX = panelX + 12f;
        float contentY = panelY + 52f;
        float contentWidth = panelWidth - 24f;
        float contentHeight = panelHeight - 92f;

        GL11.glPushMatrix();
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        Scissor.apply(contentX, contentY, contentWidth, contentHeight);

        scrollContainer.draw(contentX, contentY, contentWidth, contentHeight, mouseX, mouseY, () -> {
            float listY = LIST_TOP_PADDING + scrollContainer.getScroll();
            for (BackgroundOption option : OPTIONS) {
                float cardY = contentY + listY;
                renderCard(contentX, cardY, contentWidth, option, mouseX, mouseY, alpha);
                listY += CARD_HEIGHT;
                if ("classic".equals(option.id) && isOptionSelected("classic")) {
                    float editorY = contentY + listY;
                    renderClassicColorEditor(contentX, editorY, contentWidth, mouseX, mouseY, alpha);
                    listY += getClassicEditorHeight() + CLASSIC_EDITOR_BOTTOM_GAP;
                } else {
                    listY += CARD_GAP;
                }
            }
            float totalHeight = LIST_TOP_PADDING + OPTIONS.length * CARD_HEIGHT + (OPTIONS.length - 1) * CARD_GAP + LIST_BOTTOM_PADDING;
            if (isOptionSelected("classic")) {
                totalHeight += getClassicEditorHeight();
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

        Color bg = selected
                ? new Color(66, 133, 244, (int) (178 * alpha))
                : (hovered ? new Color(52, 52, 58, (int) (205 * alpha)) : new Color(35, 35, 40, (int) (180 * alpha)));
        Rects.rounded(Math.round(x), Math.round(y), Math.round(width), Math.round(CARD_HEIGHT), 10, bg);

        if (selected) {
            Rects.rounded(Math.round(x), Math.round(y), Math.round(width), Math.round(CARD_HEIGHT), 10, new Color(130, 190, 255, (int) (42 * alpha)));
        }

        float iconSize = 46f;
        float iconX = x + 10f;
        float iconY = y + (CARD_HEIGHT - iconSize) / 2f;
        Rects.rounded(Math.round(iconX), Math.round(iconY), Math.round(iconSize), Math.round(iconSize), 6, new Color(22, 22, 25, (int) (255 * alpha)));
        renderPreview(iconX + 2f, iconY + 2f, iconSize - 4f, iconSize - 4f, option);

        float textX = iconX + iconSize + 10f;
        FPSMaster.fontManager.s18.drawString(FPSMaster.i18n.get(option.nameKey), textX, y + 14f, new Color(255, 255, 255, (int) (255 * alpha)).getRGB());
        FPSMaster.fontManager.s14.drawString(FPSMaster.i18n.get(option.descKey), textX, y + 36f, new Color(188, 188, 188, (int) (255 * alpha)).getRGB());

        if (option.id.equals("custom")) {
            renderPickButton(textX, y + 56f, mouseX, mouseY, alpha);
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
        Rects.rounded(Math.round(x), Math.round(y), Math.round(w), Math.round(h), 5, option.previewColor);
        FPSMaster.fontManager.s14.drawCenteredString(FPSMaster.i18n.get("backgroundselector.preview.image"), x + w / 2f, y + h / 2f - 4f, Color.WHITE.getRGB());
    }

    private void renderPickButton(float x, float y, int mouseX, int mouseY, float alpha) {
        float w = 90f;
        float h = 12f;
        boolean hovered = Hover.is(x, y, w, h, mouseX, mouseY);
        Color bg = hovered ? new Color(100, 181, 246, (int) (180 * alpha)) : new Color(66, 133, 244, (int) (160 * alpha));
        Rects.rounded(Math.round(x), Math.round(y), Math.round(w), Math.round(h), 4, bg);
        FPSMaster.fontManager.s14.drawCenteredString(FPSMaster.i18n.get("backgroundselector.pick"), x + w / 2f, y + h / 2f - 4f, new Color(255, 255, 255, (int) (255 * alpha)).getRGB());
    }

    private void renderClassicColorEditor(float x, float y, float width, int mouseX, int mouseY, float alpha) {
        float editorHeight = getClassicEditorHeight();
        Rects.rounded(Math.round(x), Math.round(y), Math.round(width), Math.round(editorHeight), 10,
                new Color(35, 35, 40, (int) (180 * alpha)));
        classicColorRender.render(x + 4f, y + 4f, width - 8f, 12f, mouseX, mouseY, true);
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
        if (!hasPendingClick(0)) {
            return;
        }

        int mouseX = getPendingClickX();
        int mouseY = getPendingClickY();
        float contentX = panelX + 12f;
        float contentY = panelY + 52f;
        float contentWidth = panelWidth - 24f;
        float contentHeight = panelHeight - 92f;

        if (!Hover.is(contentX, contentY, contentWidth, contentHeight, mouseX, mouseY)) {
            return;
        }

        float listY = LIST_TOP_PADDING + scrollContainer.getScroll();
        for (BackgroundOption option : OPTIONS) {
            float cardY = contentY + listY;
            if (Hover.is(contentX, cardY, contentWidth, CARD_HEIGHT, mouseX, mouseY)) {
                if ("custom".equals(option.id)) {
                    float iconSize = 46f;
                    float textX = contentX + 10f + iconSize + 10f;
                    float btnX = textX;
                    float btnY = cardY + 56f;
                    if (Hover.is(btnX, btnY, 90f, 12f, mouseX, mouseY)) {
                        selectCustomImage();
                    } else {
                        FPSMaster.configManager.configure.background = "custom";
                    }
                } else {
                    FPSMaster.configManager.configure.background = option.id;
                }
                consumePendingClick();
                return;
            }
            listY += CARD_HEIGHT;
            if ("classic".equals(option.id) && isOptionSelected("classic")) {
                listY += getClassicEditorHeight() + CLASSIC_EDITOR_BOTTOM_GAP;
            } else {
                listY += CARD_GAP;
            }
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

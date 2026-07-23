package top.fpsmaster.ui.click;

import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.opengl.GL11;
import top.fpsmaster.FPSMaster;
import top.fpsmaster.exception.FileException;
import top.fpsmaster.font.impl.UFontRenderer;
import top.fpsmaster.modules.config.ConfigProfileUtils;
import top.fpsmaster.modules.config.ConfigProfileUtils.ConfigProfile;
import top.fpsmaster.modules.logger.ClientLogger;
import top.fpsmaster.ui.click.component.ScrollContainer;
import top.fpsmaster.ui.common.TextField;
import top.fpsmaster.utils.math.anim.AnimClock;
import top.fpsmaster.utils.math.anim.Animator;
import top.fpsmaster.utils.math.anim.BezierEasing;
import top.fpsmaster.utils.math.anim.ColorAnimator;
import top.fpsmaster.utils.math.anim.Easings;
import top.fpsmaster.utils.render.draw.Colors;
import top.fpsmaster.utils.render.draw.Hover;
import top.fpsmaster.utils.render.draw.Icons;
import top.fpsmaster.utils.render.draw.Rects;
import top.fpsmaster.utils.render.gui.ScaledGuiScreen;
import top.fpsmaster.utils.render.gui.Scissor;
import top.fpsmaster.utils.render.state.Alpha;

import java.awt.Color;
import java.awt.FileDialog;
import java.awt.Frame;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ConfigProfilesScreen extends ScaledGuiScreen {
    private static final float CARD_HEIGHT = 34f;
    private static final float CARD_GAP = 5f;
    private static final float LIST_PADDING = 6f;
    private static final long STATUS_DURATION_MS = 4000L;
    private static final BezierEasing EASE = BezierEasing.of(0.25, 0.1, 0.25, 1.0);
    private static final int MASK_MAX_ALPHA = 120;

    private enum DialogMode {
        NONE,
        LOAD,
        RENAME,
        DELETE,
        DEFAULTS
    }

    private final ScaledGuiScreen parent;
    private final ScrollContainer scrollContainer = new ScrollContainer();
    private final TextField renameField = new TextField(
            FPSMaster.fontManager.s16,
            "default",
            ClickGuiTheme.textFieldBg().getRGB(),
            ClickGuiTheme.textFieldText().getRGB(),
            48
    );

    private final Animator scaleAnimation = new Animator();
    private final Animator alphaAnimation = new Animator();
    private final Animator maskAlpha = new Animator();
    private final Animator dialogAnim = new Animator();
    private final AnimClock animClock = new AnimClock();
    private final Map<String, ColorAnimator> hoverAnims = new HashMap<>();

    private List<ConfigProfile> profiles = new ArrayList<>();
    private DialogMode dialogMode = DialogMode.NONE;
    private String dialogProfileName = "";
    private String status = "";
    private int statusColor = ClickGuiTheme.textSecondary().getRGB();
    private long statusTime;
    private boolean close;
    private String tooltip;
    private float tooltipX;
    private float tooltipY;

    public ConfigProfilesScreen(ScaledGuiScreen parent) {
        this.parent = parent;
    }

    @Override
    public void initGui() {
        super.initGui();
        scrollContainer.setHeight(0f);
        reloadProfiles();
        animClock.reset();
        scaleAnimation.start(0.9, 1.0, 0.2f, EASE);
        alphaAnimation.start(0.0, 255.0, 0.2f, EASE);
        maskAlpha.start(0.0, MASK_MAX_ALPHA, 0.2f, EASE);
        close = false;
        setStatus("", ClickGuiTheme.textSecondary().getRGB());
    }

    @Override
    public void render(int mouseX, int mouseY, float partialTicks) {
        double dt = animClock.tick();
        scaleAnimation.update(dt);
        alphaAnimation.update(dt);
        maskAlpha.update(dt);
        dialogAnim.update(dt);

        if (close && !scaleAnimation.isRunning()) {
            mc.displayGuiScreen(parent);
            return;
        }

        float panelWidth = MainPanel.width;
        float panelHeight = MainPanel.height;
        float panelX = (guiWidth - panelWidth) / 2f;
        float panelY = (guiHeight - panelHeight) / 2f;
        float scale = (float) scaleAnimation.get();

        Alpha.set(1f);
        Rects.fill(0f, 0f, guiWidth, guiHeight, ClickGuiTheme.mask((int) maskAlpha.get()));
        Alpha.set((float) (alphaAnimation.get() / 255.0));

        GlStateManager.pushMatrix();
        GlStateManager.translate(guiWidth / 2f, guiHeight / 2f, 0f);
        GL11.glScaled(scale, scale, 1.0);
        GlStateManager.translate(-guiWidth / 2f, -guiHeight / 2f, 0f);

        tooltip = null;
        Rects.rounded(Math.round(panelX), Math.round(panelY), Math.round(panelWidth), Math.round(panelHeight), 12, panelBackground().getRGB());
        Rects.rounded(Math.round(panelX), Math.round(panelY), Math.round(panelWidth), 38, 12, headerBackground().getRGB());
        Rects.fill(panelX + 1f, panelY + 38f, panelWidth - 2f, 1f, ClickGuiTheme.divider().getRGB());
        renderBackButton(panelX + 10f, panelY + 9f, mouseX, mouseY);
        FPSMaster.fontManager.s20.drawCenteredString(
                FPSMaster.i18n.get("configprofiles.title"),
                panelX + panelWidth / 2f,
                panelY + 15f,
                ClickGuiTheme.textPrimary().getRGB()
        );
        renderHeaderActions(panelX, panelY, panelWidth, mouseX, mouseY);

        float contentX = panelX + 12f;
        float contentWidth = panelWidth - 24f;
        float listY = panelY + 48f;
        float listHeight = panelY + panelHeight - 28f - listY;

        renderProfileList(contentX, listY, contentWidth, listHeight, scale, mouseX, mouseY);
        renderStatusBar(contentX, panelY + panelHeight - 19f, contentWidth);
        renderTooltip(panelX, panelWidth);
        renderDialog(panelX, panelY, panelWidth, panelHeight, mouseX, mouseY);

        GlStateManager.popMatrix();
        Alpha.set(1f);
    }

    @Override
    public void keyTyped(char typedChar, int keyCode) throws IOException {
        if (dialogMode == DialogMode.RENAME) {
            if (keyCode == 1) {
                closeDialog();
                return;
            }
            renameField.textboxKeyTyped(typedChar, keyCode);
            if (keyCode == 28) {
                runRenameAction();
            }
            return;
        }
        if (dialogMode != DialogMode.NONE) {
            if (keyCode == 1) {
                closeDialog();
            }
            return;
        }
        if (keyCode == 1) {
            requestClose();
            return;
        }
        super.keyTyped(typedChar, keyCode);
    }

    private void requestClose() {
        if (close) {
            return;
        }
        close = true;
        scaleAnimation.animateTo(0.9, 0.12f, EASE);
        alphaAnimation.animateTo(0.0, 0.12f, EASE);
        maskAlpha.animateTo(0.0, 0.12f, EASE);
    }

    private void renderHeaderActions(float panelX, float panelY, float panelWidth, int mouseX, int mouseY) {
        float size = 20f;
        float gap = 6f;
        float y = panelY + 9f;
        float resetX = panelX + panelWidth - 10f - size;
        float exportX = resetX - size - gap;
        float importX = exportX - size - gap;

        if (renderIconButton("action.import", importX, y, size, "import", "configprofiles.importfile", false, ClickGuiTheme.buttonBg(), true, mouseX, mouseY)) {
            importSelectedProfile();
        }
        if (renderIconButton("action.export", exportX, y, size, "export", "configprofiles.exportfile", false, ClickGuiTheme.buttonBg(), true, mouseX, mouseY)) {
            exportCurrentProfile();
        }
        if (renderIconButton("action.reset", resetX, y, size, "reset", "configprofiles.preset.alloff", true, ClickGuiTheme.buttonBg(), true, mouseX, mouseY)) {
            openConfirmDialog(DialogMode.DEFAULTS, "");
        }
    }

    private boolean renderIconButton(String animKey, float x, float y, float size, String icon, String tooltipKey, boolean danger, Color base, boolean enabled, int mouseX, int mouseY) {
        boolean interactive = enabled && dialogMode == DialogMode.NONE && !close;
        boolean hovered = interactive && Hover.is(x, y, size, size, mouseX, mouseY);
        Color hoverColor = danger ? withAlpha(ClickGuiTheme.danger(), 200) : ClickGuiTheme.buttonHoverBg();
        ColorAnimator bg = anim(animKey, base);
        bg.animateTo(hovered ? hoverColor : base, 0.12, Easings.QUAD_OUT);
        bg.update();
        Rects.rounded(Math.round(x), Math.round(y), Math.round(size), Math.round(size), 6, bg.get().getRGB());
        int iconColor = danger && hovered ? Color.WHITE.getRGB() : ClickGuiTheme.textPrimary().getRGB();
        float pad = (size - 12f) / 2f;
        Icons.draw(icon, x + pad, y + pad, 12f, iconColor);
        if (hovered && tooltipKey != null) {
            tooltip = FPSMaster.i18n.get(tooltipKey);
            tooltipX = x + size / 2f;
            tooltipY = y + size + 5f;
        }
        return interactive && consumePressInBounds(x, y, size, size, 0) != null;
    }

    private void renderTooltip(float panelX, float panelWidth) {
        if (tooltip == null || tooltip.isEmpty()) {
            return;
        }
        float width = FPSMaster.fontManager.s14.getStringWidth(tooltip) + 12f;
        float height = 16f;
        float x = Math.max(panelX + 6f, Math.min(tooltipX - width / 2f, panelX + panelWidth - 6f - width));
        Rects.rounded(Math.round(x), Math.round(tooltipY), Math.round(width), Math.round(height), 5, tooltipBackground().getRGB());
        FPSMaster.fontManager.s14.drawCenteredString(tooltip, x + width / 2f, tooltipY + 4f, ClickGuiTheme.textPrimary().getRGB());
    }

    private void renderProfileList(float x, float y, float width, float height, float scale, int mouseX, int mouseY) {
        Rects.rounded(Math.round(x), Math.round(y), Math.round(width), Math.round(height), 8, ClickGuiTheme.settingsBg().getRGB());
        scrollContainer.draw(this, x, y, width, height, mouseX, mouseY, () -> {
            float centerX = guiWidth / 2f;
            float centerY = guiHeight / 2f;
            GL11.glEnable(GL11.GL_SCISSOR_TEST);
            Scissor.apply(
                    centerX + (x - centerX) * scale,
                    centerY + (y - centerY) * scale,
                    width * scale,
                    height * scale
            );
            try {
                if (profiles.isEmpty()) {
                    int emptyColor = ClickGuiTheme.textDisabled().getRGB();
                    Icons.draw("folder", x + width / 2f - 11f, y + height / 2f - 26f, 22f, emptyColor);
                    FPSMaster.fontManager.s16.drawCenteredString(
                            FPSMaster.i18n.get("configprofiles.empty"),
                            x + width / 2f,
                            y + height / 2f + 2f,
                            emptyColor
                    );
                    scrollContainer.setHeight(height);
                    return;
                }

                float rowY = y + LIST_PADDING + scrollContainer.getScroll();
                for (ConfigProfile profile : profiles) {
                    renderProfileCard(profile, x + LIST_PADDING, rowY, width - LIST_PADDING * 2f, x, y, width, height, mouseX, mouseY);
                    rowY += CARD_HEIGHT + CARD_GAP;
                }
                scrollContainer.setHeight(LIST_PADDING * 2f + profiles.size() * (CARD_HEIGHT + CARD_GAP) - CARD_GAP);
            } finally {
                GL11.glDisable(GL11.GL_SCISSOR_TEST);
            }
        });
    }

    private void renderProfileCard(ConfigProfile profile, float x, float y, float width, float listX, float listY, float listWidth, float listHeight, int mouseX, int mouseY) {
        boolean interactive = dialogMode == DialogMode.NONE && !close;
        boolean pointerInsideList = Hover.is(listX, listY, listWidth, listHeight, mouseX, mouseY);
        boolean active = profile.getName().equals(ConfigProfileUtils.getActiveProfileName());
        boolean cardHovered = interactive && pointerInsideList && Hover.is(x, y, width, CARD_HEIGHT, mouseX, mouseY);

        Color base = active
                ? withAlpha(ClickGuiTheme.accent(), ClickGuiTheme.isLight() ? 40 : 60)
                : ClickGuiTheme.cardBg();
        Color hoverColor = active
                ? withAlpha(ClickGuiTheme.accent(), ClickGuiTheme.isLight() ? 60 : 85)
                : ClickGuiTheme.cardHoverBg();
        ColorAnimator bg = anim("card." + profile.getName(), base);
        bg.animateTo(cardHovered ? hoverColor : base, 0.12, Easings.QUAD_OUT);
        bg.update();
        Rects.rounded(Math.round(x), Math.round(y), Math.round(width), Math.round(CARD_HEIGHT), 7, bg.get().getRGB());
        if (active) {
            Rects.rounded(Math.round(x), Math.round(y + 9f), 3, Math.round(CARD_HEIGHT - 18f), 1, ClickGuiTheme.accent().getRGB());
        }

        boolean manageable = !ConfigProfileUtils.CURRENT_CONFIG.equals(profile.getName());
        float buttonSize = 20f;
        float buttonGap = 4f;
        float buttonY = y + (CARD_HEIGHT - buttonSize) / 2f;
        float deleteX = x + width - buttonSize - 7f;
        float renameX = deleteX - buttonSize - buttonGap;

        float textX = x + 12f;
        float textMax = (manageable ? renameX : x + width) - textX - 8f;
        String badge = FPSMaster.i18n.get("configprofiles.current");
        if (active) {
            textMax -= FPSMaster.fontManager.s14.getStringWidth(badge) + 18f;
        }
        String name = trimText(FPSMaster.fontManager.s16, profile.getName(), textMax);
        FPSMaster.fontManager.s16.drawString(name, textX, y + CARD_HEIGHT / 2f - 4.5f, ClickGuiTheme.textPrimary().getRGB());

        if (active) {
            float badgeX = textX + FPSMaster.fontManager.s16.getStringWidth(name) + 7f;
            float badgeWidth = FPSMaster.fontManager.s14.getStringWidth(badge) + 10f;
            Rects.rounded(Math.round(badgeX), Math.round(y + (CARD_HEIGHT - 13f) / 2f), Math.round(badgeWidth), 13, 6, ClickGuiTheme.accent().getRGB());
            FPSMaster.fontManager.s14.drawCenteredString(badge, badgeX + badgeWidth / 2f, y + CARD_HEIGHT / 2f - 3.5f, Color.WHITE.getRGB());
        }

        if (manageable) {
            if (renderIconButton("rename." + profile.getName(), renameX, buttonY, buttonSize, "rename", "configprofiles.rename", false, ClickGuiTheme.settingsBg(), pointerInsideList, mouseX, mouseY)) {
                openRenameDialog(profile.getName());
                return;
            }
            if (renderIconButton("delete." + profile.getName(), deleteX, buttonY, buttonSize, "delete", "configprofiles.delete", true, ClickGuiTheme.settingsBg(), pointerInsideList, mouseX, mouseY)) {
                openConfirmDialog(DialogMode.DELETE, profile.getName());
                return;
            }
        }

        if (interactive && pointerInsideList && consumePressInBounds(x, y, width, CARD_HEIGHT, 0) != null && !active) {
            openConfirmDialog(DialogMode.LOAD, profile.getName());
        }
    }

    private void renderStatusBar(float x, float y, float width) {
        String text = status;
        int color = statusColor;
        if (text.isEmpty() || System.currentTimeMillis() - statusTime > STATUS_DURATION_MS) {
            text = FPSMaster.i18n.get("configprofiles.switch.tip");
            color = ClickGuiTheme.textSecondary().getRGB();
        }
        drawDot(x + 2.5f, y + 4f, 2f, color);
        FPSMaster.fontManager.s14.drawString(trimText(FPSMaster.fontManager.s14, text, width - 12f), x + 8f, y, color);
    }

    private void renderDialog(float panelX, float panelY, float panelWidth, float panelHeight, int mouseX, int mouseY) {
        if (dialogMode == DialogMode.NONE) {
            return;
        }

        float progress = (float) dialogAnim.get();
        Rects.rounded(Math.round(panelX), Math.round(panelY), Math.round(panelWidth), Math.round(panelHeight), 12, ClickGuiTheme.mask((int) (90 * progress)).getRGB());

        float dialogWidth = panelWidth - 130f;
        float dialogHeight = dialogMode == DialogMode.RENAME ? 118f : 92f;
        float dialogX = panelX + (panelWidth - dialogWidth) / 2f;
        float dialogY = panelY + (panelHeight - dialogHeight) / 2f - 8f;

        float outerAlpha = (float) (alphaAnimation.get() / 255.0);
        Alpha.set(outerAlpha * progress);
        GlStateManager.pushMatrix();
        float dialogCenterX = dialogX + dialogWidth / 2f;
        float dialogCenterY = dialogY + dialogHeight / 2f;
        float dialogScale = 0.92f + 0.08f * progress;
        GlStateManager.translate(dialogCenterX, dialogCenterY, 0f);
        GL11.glScaled(dialogScale, dialogScale, 1.0);
        GlStateManager.translate(-dialogCenterX, -dialogCenterY, 0f);

        Rects.rounded(Math.round(dialogX), Math.round(dialogY), Math.round(dialogWidth), Math.round(dialogHeight), 10, dialogBackground().getRGB());
        if (dialogMode == DialogMode.RENAME) {
            renderRenameDialog(dialogX, dialogY, dialogWidth, mouseX, mouseY);
        } else {
            renderConfirmDialog(dialogX, dialogY, dialogWidth, mouseX, mouseY);
        }

        GlStateManager.popMatrix();
        Alpha.set(outerAlpha);

        ScaledGuiScreen.PointerEvent leftover = consumePressInBounds(panelX, panelY, panelWidth, panelHeight, 0);
        if (leftover != null && !Hover.is(dialogX, dialogY, dialogWidth, dialogHeight, leftover.x, leftover.y)) {
            closeDialog();
        }
    }

    private void renderRenameDialog(float dialogX, float dialogY, float dialogWidth, int mouseX, int mouseY) {
        FPSMaster.fontManager.s16.drawCenteredString(
                FPSMaster.i18n.get("configprofiles.rename.title"),
                dialogX + dialogWidth / 2f,
                dialogY + 14f,
                ClickGuiTheme.textPrimary().getRGB()
        );
        FPSMaster.fontManager.s14.drawString(
                FPSMaster.i18n.get("configprofiles.rename.name"),
                dialogX + 18f,
                dialogY + 36f,
                ClickGuiTheme.textSecondary().getRGB()
        );

        renameField.backGroundColor = ClickGuiTheme.textFieldBg().getRGB();
        renameField.fontColor = ClickGuiTheme.textFieldText().getRGB();
        renameField.placeHolder = FPSMaster.i18n.get("configprofiles.name.placeholder");
        renameField.drawTextBox(dialogX + 18f, dialogY + 50f, dialogWidth - 36f, 22f);
        handleRenameFieldClick(dialogX + 18f, dialogY + 50f, dialogWidth - 36f, 22f);

        float btnY = dialogY + 84f;
        float confirmX = dialogX + dialogWidth / 2f - 74f;
        float cancelX = dialogX + dialogWidth / 2f + 4f;
        if (renderDialogButton("dialog.save", confirmX, btnY, 70f, 22f, "configprofiles.save",
                ClickGuiTheme.accent(), accentHover(), Color.WHITE.getRGB(), mouseX, mouseY)) {
            runRenameAction();
        }
        if (renderDialogButton("dialog.cancel", cancelX, btnY, 70f, 22f, "configprofiles.cancel",
                ClickGuiTheme.buttonBg(), ClickGuiTheme.buttonHoverBg(), ClickGuiTheme.textPrimary().getRGB(), mouseX, mouseY)) {
            closeDialog();
        }
    }

    private void renderConfirmDialog(float dialogX, float dialogY, float dialogWidth, int mouseX, int mouseY) {
        boolean destructive = dialogMode == DialogMode.DELETE || dialogMode == DialogMode.DEFAULTS;
        FPSMaster.fontManager.s16.drawCenteredString(
                trimText(FPSMaster.fontManager.s16, getConfirmMessage(), dialogWidth - 24f),
                dialogX + dialogWidth / 2f,
                dialogY + 24f,
                ClickGuiTheme.textPrimary().getRGB()
        );

        float btnY = dialogY + 56f;
        float confirmX = dialogX + dialogWidth / 2f - 74f;
        float cancelX = dialogX + dialogWidth / 2f + 4f;
        Color confirmBase = destructive ? ClickGuiTheme.danger() : ClickGuiTheme.accent();
        Color confirmHover = destructive ? dangerHover() : accentHover();
        String confirmKey = destructive ? "dialog.confirm.danger" : "dialog.confirm.accent";
        if (renderDialogButton(confirmKey, confirmX, btnY, 70f, 22f, "configprofiles.confirm",
                confirmBase, confirmHover, Color.WHITE.getRGB(), mouseX, mouseY)) {
            runConfirmAction();
        }
        if (renderDialogButton("dialog.cancel", cancelX, btnY, 70f, 22f, "configprofiles.cancel",
                ClickGuiTheme.buttonBg(), ClickGuiTheme.buttonHoverBg(), ClickGuiTheme.textPrimary().getRGB(), mouseX, mouseY)) {
            closeDialog();
        }
    }

    private String getConfirmMessage() {
        switch (dialogMode) {
            case LOAD:
                return String.format(FPSMaster.i18n.get("configprofiles.confirm.load"), dialogProfileName);
            case DELETE:
                return String.format(FPSMaster.i18n.get("configprofiles.confirm.delete"), dialogProfileName);
            case DEFAULTS:
                return FPSMaster.i18n.get("configprofiles.confirm.alloff");
            default:
                return "";
        }
    }

    private boolean renderDialogButton(String animKey, float x, float y, float width, float height, String textKey, Color base, Color hoverColor, int textColor, int mouseX, int mouseY) {
        boolean hovered = Hover.is(x, y, width, height, mouseX, mouseY);
        ColorAnimator bg = anim(animKey, base);
        bg.animateTo(hovered ? hoverColor : base, 0.12, Easings.QUAD_OUT);
        bg.update();
        Rects.rounded(Math.round(x), Math.round(y), Math.round(width), Math.round(height), 6, bg.get().getRGB());
        FPSMaster.fontManager.s14.drawCenteredString(FPSMaster.i18n.get(textKey), x + width / 2f, y + height / 2f - 4f, textColor);
        return consumePressInBounds(x, y, width, height, 0) != null;
    }

    private void renderBackButton(float x, float y, int mouseX, int mouseY) {
        if (renderIconButton("back", x, y, 20f, "back", null, false, ClickGuiTheme.buttonBg(), true, mouseX, mouseY)) {
            requestClose();
        }
    }

    private void drawDot(float cx, float cy, float r, int color) {
        Color c = Colors.toColor(Alpha.apply(color));
        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glColor4f(c.getRed() / 255f, c.getGreen() / 255f, c.getBlue() / 255f, c.getAlpha() / 255f);
        GL11.glBegin(GL11.GL_TRIANGLE_FAN);
        GL11.glVertex2f(cx, cy);
        int steps = 16;
        for (int i = 0; i <= steps; i++) {
            double angle = Math.PI * 2 * i / steps;
            GL11.glVertex2f(cx + (float) (Math.cos(angle) * r), cy + (float) (Math.sin(angle) * r));
        }
        GL11.glEnd();
        GlStateManager.enableTexture2D();
        GL11.glColor4f(1f, 1f, 1f, 1f);
    }

    private void handleRenameFieldClick(float x, float y, float width, float height) {
        ScaledGuiScreen.PointerEvent pendingPress = peekAnyPress();
        if (pendingPress != null && !Hover.is(x, y, width, height, pendingPress.x, pendingPress.y)) {
            renameField.setFocused(false);
        }
        ScaledGuiScreen.PointerEvent click = consumePressInBounds(x, y, width, height, 0);
        if (click != null) {
            renameField.mouseClicked(click.x, click.y, click.button);
        }
    }

    private void openRenameDialog(String profileName) {
        dialogMode = DialogMode.RENAME;
        dialogProfileName = profileName;
        dialogAnim.start(0.0, 1.0, 0.18f, Easings.CUBIC_OUT);
        renameField.setText(profileName);
        renameField.setCursorPositionEnd();
        renameField.setFocused(true);
    }

    private void openConfirmDialog(DialogMode mode, String profileName) {
        dialogMode = mode;
        dialogProfileName = profileName == null ? "" : profileName;
        dialogAnim.start(0.0, 1.0, 0.18f, Easings.CUBIC_OUT);
    }

    private void closeDialog() {
        dialogMode = DialogMode.NONE;
        dialogProfileName = "";
        renameField.setFocused(false);
    }

    private void runRenameAction() {
        String oldName = dialogProfileName;
        String newName = renameField.getText();
        closeDialog();
        renameProfile(oldName, newName);
    }

    private void runConfirmAction() {
        DialogMode mode = dialogMode;
        String profileName = dialogProfileName;
        closeDialog();
        switch (mode) {
            case LOAD:
                loadProfile(profileName);
                break;
            case DELETE:
                deleteProfile(profileName);
                break;
            case DEFAULTS:
                applyDefaultPreset();
                break;
            default:
                break;
        }
    }

    private void importSelectedProfile() {
        try {
            FileDialog fileDialog = new FileDialog((Frame) null, FPSMaster.i18n.get("configprofiles.filedialog.import"), FileDialog.LOAD);
            fileDialog.setDirectory(ConfigProfileUtils.getProfileDir().getAbsolutePath());
            fileDialog.setFilenameFilter((dir, name) -> name.toLowerCase(Locale.ROOT).endsWith(".json"));
            fileDialog.setVisible(true);
            if (fileDialog.getFile() == null) {
                return;
            }

            File selectedFile = new File(fileDialog.getDirectory(), fileDialog.getFile());
            String profileName = ConfigProfileUtils.importProfile(selectedFile);
            ConfigProfileUtils.loadProfile(profileName);
            reloadProfiles();
            setStatus(String.format(FPSMaster.i18n.get("configprofiles.status.imported"), profileName), successColor());
        } catch (Exception exception) {
            ClientLogger.error("Failed to import config profile from file: " + exception.getMessage());
            setStatus(FPSMaster.i18n.get("configprofiles.status.import_failed"), errorColor());
        }
    }

    private void exportCurrentProfile() {
        try {
            FileDialog fileDialog = new FileDialog((Frame) null, FPSMaster.i18n.get("configprofiles.filedialog.export"), FileDialog.SAVE);
            fileDialog.setDirectory(ConfigProfileUtils.getProfileDir().getAbsolutePath());
            fileDialog.setFile(ConfigProfileUtils.getActiveProfileName() + ".json");
            fileDialog.setVisible(true);
            if (fileDialog.getFile() == null) {
                return;
            }

            File targetFile = normalizeJsonFile(new File(fileDialog.getDirectory(), fileDialog.getFile()));
            ConfigProfileUtils.exportActiveProfile(targetFile);
            reloadProfiles();
            setStatus(String.format(FPSMaster.i18n.get("configprofiles.status.exported"), targetFile.getName()), successColor());
        } catch (FileException exception) {
            ClientLogger.error("Failed to export config profile to file: " + exception.getMessage());
            setStatus(FPSMaster.i18n.get("configprofiles.status.export_failed"), errorColor());
        }
    }

    private File normalizeJsonFile(File file) {
        if (file.getName().toLowerCase(Locale.ROOT).endsWith(".json")) {
            return file;
        }
        return new File(file.getParentFile(), file.getName() + ".json");
    }

    private void loadProfile(String profileName) {
        try {
            ConfigProfileUtils.loadProfile(profileName);
            reloadProfiles();
            setStatus(String.format(FPSMaster.i18n.get("configprofiles.status.loaded"), profileName), successColor());
        } catch (Exception exception) {
            ClientLogger.error("Failed to switch config profile: " + profileName + " / " + exception.getMessage());
            setStatus(FPSMaster.i18n.get("configprofiles.status.load_failed"), errorColor());
        }
    }

    private void renameProfile(String oldName, String newName) {
        try {
            String renamed = ConfigProfileUtils.renameProfile(oldName, newName, "");
            if (oldName.equals(ConfigProfileUtils.getActiveProfileName())) {
                ConfigProfileUtils.setActiveProfileName(renamed);
            }
            reloadProfiles();
            setStatus(String.format(FPSMaster.i18n.get("configprofiles.status.renamed"), renamed), successColor());
        } catch (FileException exception) {
            ClientLogger.error("Failed to rename config profile: " + exception.getMessage());
            setStatus(FPSMaster.i18n.get("configprofiles.status.rename_failed"), errorColor());
        }
    }

    private void deleteProfile(String profileName) {
        try {
            ConfigProfileUtils.deleteProfile(profileName);
            reloadProfiles();
            if (profileName.equals(ConfigProfileUtils.getActiveProfileName())) {
                ConfigProfileUtils.loadProfileWithoutSavingCurrent(ConfigProfileUtils.CURRENT_CONFIG);
            }
            setStatus(String.format(FPSMaster.i18n.get("configprofiles.status.deleted"), profileName), successColor());
        } catch (Exception exception) {
            ClientLogger.error("Failed to delete config profile: " + exception.getMessage());
            setStatus(FPSMaster.i18n.get("configprofiles.status.delete_failed"), errorColor());
        }
    }

    private void applyDefaultPreset() {
        try {
            String profileName = ConfigProfileUtils.getActiveProfileName();
            ConfigProfileUtils.resetActiveProfileToDefaults();
            reloadProfiles();
            setStatus(String.format(FPSMaster.i18n.get("configprofiles.status.alloff"), profileName), successColor());
        } catch (FileException exception) {
            ClientLogger.error("Failed to reset active config profile: " + exception.getMessage());
            setStatus(FPSMaster.i18n.get("configprofiles.status.alloff_failed"), errorColor());
        }
    }

    private void reloadProfiles() {
        profiles = ConfigProfileUtils.listConfigs();
    }

    private void setStatus(String status, int color) {
        this.status = status == null ? "" : status;
        this.statusColor = color;
        this.statusTime = System.currentTimeMillis();
    }

    private ColorAnimator anim(String key, Color base) {
        ColorAnimator animator = hoverAnims.get(key);
        if (animator == null) {
            animator = new ColorAnimator(base);
            hoverAnims.put(key, animator);
        }
        return animator;
    }

    private String trimText(UFontRenderer font, String text, float maxWidth) {
        if (text == null) {
            return "";
        }
        if (font.getStringWidth(text) <= maxWidth) {
            return text;
        }
        String value = text;
        while (value.length() > 1 && font.getStringWidth(value + "...") > maxWidth) {
            value = value.substring(0, value.length() - 1);
        }
        return value + "...";
    }

    private static Color withAlpha(Color color, int alpha) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
    }

    private Color accentHover() {
        return new Color(108, 119, 245);
    }

    private Color dangerHover() {
        return ClickGuiTheme.isLight() ? new Color(190, 50, 50) : new Color(255, 110, 110);
    }

    private Color panelBackground() {
        return ClickGuiTheme.isLight() ? new Color(235, 238, 248, 238) : new Color(20, 20, 24, 238);
    }

    private Color headerBackground() {
        return ClickGuiTheme.isLight() ? new Color(255, 255, 255, 180) : new Color(30, 30, 36, 210);
    }

    private Color dialogBackground() {
        return ClickGuiTheme.isLight() ? new Color(248, 249, 253, 250) : new Color(28, 28, 34, 250);
    }

    private Color tooltipBackground() {
        return ClickGuiTheme.isLight() ? new Color(255, 255, 255, 245) : new Color(40, 40, 48, 245);
    }

    private int successColor() {
        return new Color(110, 255, 150).getRGB();
    }

    private int errorColor() {
        return new Color(255, 120, 120).getRGB();
    }
}

package top.fpsmaster.ui.click;

import org.lwjgl.opengl.GL11;
import top.fpsmaster.FPSMaster;
import top.fpsmaster.exception.FileException;
import top.fpsmaster.modules.config.ConfigProfileUtils;
import top.fpsmaster.modules.config.ConfigProfileUtils.ConfigProfile;
import top.fpsmaster.modules.logger.ClientLogger;
import top.fpsmaster.ui.click.component.ScrollContainer;
import top.fpsmaster.ui.common.TextField;
import top.fpsmaster.utils.render.draw.Hover;
import top.fpsmaster.utils.render.draw.Rects;
import top.fpsmaster.utils.render.gui.ScaledGuiScreen;
import top.fpsmaster.utils.render.gui.Scissor;

import java.awt.Color;
import java.awt.FileDialog;
import java.awt.Frame;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ConfigProfilesScreen extends ScaledGuiScreen {
    private static final float CARD_HEIGHT = 31f;
    private static final float CARD_GAP = 6f;
    private static final float LIST_PADDING = 5f;

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

    private List<ConfigProfile> profiles = new ArrayList<>();
    private DialogMode dialogMode = DialogMode.NONE;
    private String dialogProfileName = "";
    private String status = "";
    private int statusColor = ClickGuiTheme.textSecondary().getRGB();
    private boolean statusStrong;

    public ConfigProfilesScreen(ScaledGuiScreen parent) {
        this.parent = parent;
    }

    @Override
    public void initGui() {
        super.initGui();
        scrollContainer.setHeight(0f);
        reloadProfiles();
        setStatus("", ClickGuiTheme.textSecondary().getRGB());
    }

    @Override
    public void render(int mouseX, int mouseY, float partialTicks) {
        Rects.fill(0f, 0f, guiWidth, guiHeight, ClickGuiTheme.mask(120));

        float panelWidth = MainPanel.width;
        float panelHeight = MainPanel.height;
        float panelX = (guiWidth - panelWidth) / 2f;
        float panelY = (guiHeight - panelHeight) / 2f;

        Rects.rounded(Math.round(panelX), Math.round(panelY), Math.round(panelWidth), Math.round(panelHeight), 12, panelBackground().getRGB());
        Rects.rounded(Math.round(panelX), Math.round(panelY), Math.round(panelWidth), 38, 12, headerBackground().getRGB());
        renderBackButton(panelX + 10f, panelY + 9f, mouseX, mouseY);
        FPSMaster.fontManager.s20.drawCenteredString(
                FPSMaster.i18n.get("configprofiles.title"),
                panelX + panelWidth / 2f,
                panelY + 15f,
                ClickGuiTheme.textPrimary().getRGB()
        );

        float contentX = panelX + 12f;
        float contentY = panelY + 48f;
        float contentWidth = panelWidth - 24f;

        renderActionBar(contentX, contentY, contentWidth, mouseX, mouseY);
        renderHint(contentX, contentY + 29f, contentWidth);
        renderProfileList(contentX, contentY + 46f, contentWidth, panelY + panelHeight - 74f - contentY, mouseX, mouseY);

        FPSMaster.fontManager.s14.drawString(status, contentX, panelY + panelHeight - 19f, statusColor);
        renderDialog(panelX, panelY, panelWidth, panelHeight, mouseX, mouseY);
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
            mc.displayGuiScreen(parent);
            return;
        }
        super.keyTyped(typedChar, keyCode);
    }

    private void renderActionBar(float x, float y, float width, int mouseX, int mouseY) {
        float gap = 8f;
        float buttonWidth = (width - gap * 2f) / 3f;

        if (renderActionButton(x, y, buttonWidth, 22f, "configprofiles.importfile", mouseX, mouseY)) {
            importSelectedProfile();
        }
        if (renderActionButton(x + buttonWidth + gap, y, buttonWidth, 22f, "configprofiles.exportfile", mouseX, mouseY)) {
            exportCurrentProfile();
        }
        if (renderActionButton(x + (buttonWidth + gap) * 2f, y, buttonWidth, 22f, "configprofiles.preset.alloff", mouseX, mouseY)) {
            openConfirmDialog(DialogMode.DEFAULTS, "");
        }
    }

    private void renderHint(float x, float y, float width) {
        FPSMaster.fontManager.s14.drawString(
                FPSMaster.i18n.get("configprofiles.switch.tip"),
                x,
                y,
                ClickGuiTheme.textSecondary().getRGB()
        );
    }

    private void renderProfileList(float x, float y, float width, float height, int mouseX, int mouseY) {
        Rects.rounded(Math.round(x), Math.round(y), Math.round(width), Math.round(height), 8, ClickGuiTheme.settingsBg().getRGB());
        scrollContainer.draw(this, x, y, width, height, mouseX, mouseY, () -> {
            GL11.glEnable(GL11.GL_SCISSOR_TEST);
            Scissor.apply(x, y, width, height);
            try {
                float rowY = y + LIST_PADDING + scrollContainer.getScroll();
                if (profiles.isEmpty()) {
                    FPSMaster.fontManager.s14.drawCenteredString(
                            FPSMaster.i18n.get("configprofiles.empty"),
                            x + width / 2f,
                            rowY + 16f,
                            ClickGuiTheme.textDisabled().getRGB()
                    );
                    scrollContainer.setHeight(height);
                    return;
                }

                for (ConfigProfile profile : profiles) {
                    renderProfileCard(profile, x + LIST_PADDING, rowY, width - LIST_PADDING * 2f, x, y, width, height, mouseX, mouseY);
                    rowY += CARD_HEIGHT + CARD_GAP;
                }
                scrollContainer.setHeight(LIST_PADDING * 2f + profiles.size() * (CARD_HEIGHT + CARD_GAP));
            } finally {
                GL11.glDisable(GL11.GL_SCISSOR_TEST);
            }
        });
    }

    private void renderProfileCard(ConfigProfile profile, float x, float y, float width, float listX, float listY, float listWidth, float listHeight, int mouseX, int mouseY) {
        boolean pointerInsideList = Hover.is(listX, listY, listWidth, listHeight, mouseX, mouseY);
        boolean active = profile.getName().equals(ConfigProfileUtils.getActiveProfileName());
        boolean hovered = pointerInsideList && Hover.is(x, y, width, CARD_HEIGHT, mouseX, mouseY);
        Color bg = active
                ? new Color(89, 101, 241, ClickGuiTheme.isLight() ? 170 : 140)
                : (hovered ? ClickGuiTheme.buttonHoverBg() : ClickGuiTheme.buttonBg());
        Rects.rounded(Math.round(x), Math.round(y), Math.round(width), Math.round(CARD_HEIGHT), 7, bg.getRGB());

        boolean manageable = !ConfigProfileUtils.CURRENT_CONFIG.equals(profile.getName());
        float buttonWidth = 36f;
        float buttonGap = 4f;
        float buttonY = y + 5f;
        float deleteX = x + width - buttonWidth - 8f;
        float renameX = deleteX - buttonWidth - buttonGap;
        float textWidth = manageable ? width - 96f : width - 18f;

        String title = profile.getName() + (active ? "  " + FPSMaster.i18n.get("configprofiles.badge.current") : "");
        FPSMaster.fontManager.s16.drawString(trimPath(title, textWidth), x + 9f, y + 11f, ClickGuiTheme.textPrimary().getRGB());

        if (dialogMode == DialogMode.NONE && manageable) {
            if (renderSmallButton(renameX, buttonY, buttonWidth, 20f, "configprofiles.rename", mouseX, mouseY, pointerInsideList)) {
                openRenameDialog(profile.getName());
                return;
            }
            if (renderSmallButton(deleteX, buttonY, buttonWidth, 20f, "configprofiles.delete", mouseX, mouseY, pointerInsideList)) {
                openConfirmDialog(DialogMode.DELETE, profile.getName());
                return;
            }
        }

        if (dialogMode == DialogMode.NONE && pointerInsideList && consumePressInBounds(x, y, width, CARD_HEIGHT, 0) != null) {
            if (!active) {
                openConfirmDialog(DialogMode.LOAD, profile.getName());
            }
        }
    }

    private boolean renderActionButton(float x, float y, float width, float height, String textKey, int mouseX, int mouseY) {
        boolean hovered = Hover.is(x, y, width, height, mouseX, mouseY);
        Color bg = hovered ? ClickGuiTheme.buttonHoverBg() : ClickGuiTheme.buttonBg();
        Rects.rounded(Math.round(x), Math.round(y), Math.round(width), Math.round(height), 5, bg.getRGB());
        FPSMaster.fontManager.s14.drawCenteredString(
                FPSMaster.i18n.get(textKey),
                x + width / 2f,
                y + height / 2f - 4f,
                ClickGuiTheme.textPrimary().getRGB()
        );
        return dialogMode == DialogMode.NONE && consumePressInBounds(x, y, width, height, 0) != null;
    }

    private boolean renderSmallButton(float x, float y, float width, float height, String textKey, int mouseX, int mouseY) {
        return renderSmallButton(x, y, width, height, textKey, mouseX, mouseY, true);
    }

    private boolean renderSmallButton(float x, float y, float width, float height, String textKey, int mouseX, int mouseY, boolean enabled) {
        boolean hovered = enabled && Hover.is(x, y, width, height, mouseX, mouseY);
        Rects.rounded(Math.round(x), Math.round(y), Math.round(width), Math.round(height), 5,
                (hovered ? ClickGuiTheme.buttonHoverBg() : ClickGuiTheme.settingsBg()).getRGB());
        FPSMaster.fontManager.s14.drawCenteredString(FPSMaster.i18n.get(textKey), x + width / 2f, y + height / 2f - 4f, ClickGuiTheme.textPrimary().getRGB());
        return enabled && consumePressInBounds(x, y, width, height, 0) != null;
    }

    private void renderDialog(float panelX, float panelY, float panelWidth, float panelHeight, int mouseX, int mouseY) {
        if (dialogMode == DialogMode.NONE) {
            return;
        }

        Rects.fill(panelX, panelY, panelWidth, panelHeight, ClickGuiTheme.mask(80));
        if (dialogMode == DialogMode.RENAME) {
            renderRenameDialog(panelX, panelY, panelWidth, mouseX, mouseY);
        } else {
            renderConfirmDialog(panelX, panelY, panelWidth, mouseX, mouseY);
        }
        consumePressInBounds(panelX, panelY, panelWidth, panelHeight, 0);
    }

    private void renderRenameDialog(float panelX, float panelY, float panelWidth, int mouseX, int mouseY) {
        float dialogX = panelX + 64f;
        float dialogY = panelY + 68f;
        float dialogW = panelWidth - 128f;
        Rects.rounded(Math.round(dialogX), Math.round(dialogY), Math.round(dialogW), 118, 10, panelBackground().getRGB());
        FPSMaster.fontManager.s16.drawCenteredString(
                FPSMaster.i18n.get("configprofiles.rename.title"),
                dialogX + dialogW / 2f,
                dialogY + 16f,
                ClickGuiTheme.textPrimary().getRGB()
        );
        FPSMaster.fontManager.s14.drawString(
                FPSMaster.i18n.get("configprofiles.rename.name"),
                dialogX + 18f,
                dialogY + 38f,
                ClickGuiTheme.textSecondary().getRGB()
        );

        renameField.backGroundColor = ClickGuiTheme.textFieldBg().getRGB();
        renameField.fontColor = ClickGuiTheme.textFieldText().getRGB();
        renameField.placeHolder = FPSMaster.i18n.get("configprofiles.name.placeholder");
        renameField.drawTextBox(dialogX + 18f, dialogY + 52f, dialogW - 36f, 22f);
        handleRenameFieldClick(dialogX + 18f, dialogY + 52f, dialogW - 36f, 22f);

        float btnY = dialogY + 86f;
        float confirmX = dialogX + dialogW / 2f - 72f;
        float cancelX = dialogX + dialogW / 2f + 8f;
        if (renderConfirmButton(confirmX, btnY, 64f, 22f, "configprofiles.save", mouseX, mouseY)) {
            runRenameAction();
        }
        if (renderConfirmButton(cancelX, btnY, 64f, 22f, "configprofiles.cancel", mouseX, mouseY)) {
            closeDialog();
        }
    }

    private void renderConfirmDialog(float panelX, float panelY, float panelWidth, int mouseX, int mouseY) {
        float dialogX = panelX + 58f;
        float dialogY = panelY + 76f;
        float dialogW = panelWidth - 116f;
        Rects.rounded(Math.round(dialogX), Math.round(dialogY), Math.round(dialogW), 88, 10, panelBackground().getRGB());
        FPSMaster.fontManager.s16.drawCenteredString(getConfirmMessage(), panelX + panelWidth / 2f, dialogY + 26f, ClickGuiTheme.textPrimary().getRGB());

        float btnY = dialogY + 54f;
        float confirmX = panelX + panelWidth / 2f - 72f;
        float cancelX = panelX + panelWidth / 2f + 8f;
        if (renderConfirmButton(confirmX, btnY, 64f, 22f, "configprofiles.confirm", mouseX, mouseY)) {
            runConfirmAction();
        }
        if (renderConfirmButton(cancelX, btnY, 64f, 22f, "configprofiles.cancel", mouseX, mouseY)) {
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

    private boolean renderConfirmButton(float x, float y, float width, float height, String textKey, int mouseX, int mouseY) {
        boolean hovered = Hover.is(x, y, width, height, mouseX, mouseY);
        Color bg = hovered ? ClickGuiTheme.buttonHoverBg() : ClickGuiTheme.buttonBg();
        Rects.rounded(Math.round(x), Math.round(y), Math.round(width), Math.round(height), 5, bg.getRGB());
        FPSMaster.fontManager.s14.drawCenteredString(FPSMaster.i18n.get(textKey), x + width / 2f, y + height / 2f - 4f, ClickGuiTheme.textPrimary().getRGB());
        return consumePressInBounds(x, y, width, height, 0) != null;
    }

    private void renderBackButton(float x, float y, int mouseX, int mouseY) {
        boolean hovered = Hover.is(x, y, 20f, 20f, mouseX, mouseY);
        Rects.rounded(Math.round(x), Math.round(y), 20, 20, 5,
                (hovered ? ClickGuiTheme.buttonHoverBg() : ClickGuiTheme.buttonBg()).getRGB());
        drawBackArrow(x + 10f, y + 10f, ClickGuiTheme.textPrimary().getRGB());
        if (dialogMode == DialogMode.NONE && consumePressInBounds(x, y, 20f, 20f, 0) != null) {
            mc.displayGuiScreen(parent);
        }
    }

    private void drawBackArrow(float centerX, float centerY, int color) {
        Color c = new Color(color, true);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glLineWidth(2f);
        GL11.glColor4f(c.getRed() / 255f, c.getGreen() / 255f, c.getBlue() / 255f, c.getAlpha() / 255f);
        GL11.glBegin(GL11.GL_LINE_STRIP);
        GL11.glVertex2f(centerX + 3f, centerY - 5f);
        GL11.glVertex2f(centerX - 3f, centerY);
        GL11.glVertex2f(centerX + 3f, centerY + 5f);
        GL11.glEnd();
        GL11.glLineWidth(1f);
        GL11.glColor4f(1f, 1f, 1f, 1f);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
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
        renameField.setText(profileName);
        renameField.setCursorPositionEnd();
        renameField.setFocused(true);
    }

    private void openConfirmDialog(DialogMode mode, String profileName) {
        dialogMode = mode;
        dialogProfileName = profileName == null ? "" : profileName;
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
    }

    private String trimPath(String path, float maxWidth) {
        if (path == null) {
            return "";
        }
        if (FPSMaster.fontManager.s14.getStringWidth(path) <= maxWidth) {
            return path;
        }
        String value = path;
        while (value.length() > 4 && FPSMaster.fontManager.s14.getStringWidth("..." + value) > maxWidth) {
            value = value.substring(1);
        }
        return "..." + value;
    }

    private Color panelBackground() {
        return ClickGuiTheme.isLight() ? new Color(235, 238, 248, 238) : new Color(20, 20, 24, 238);
    }

    private Color headerBackground() {
        return ClickGuiTheme.isLight() ? new Color(255, 255, 255, 180) : new Color(30, 30, 36, 210);
    }

    private int successColor() {
        return new Color(110, 255, 150).getRGB();
    }

    private int errorColor() {
        return new Color(255, 120, 120).getRGB();
    }
}

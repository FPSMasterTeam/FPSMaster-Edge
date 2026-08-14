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
        DEFAULTS,
        CREATE
    }

    private final ScaledGuiScreen parent;
    private final ScrollContainer scrollContainer = new ScrollContainer();
    private final TextField renameField = new TextField(
            FPSMaster.fontManager.getFont(12),
            "default",
            0,
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

        // .panel: min(800px, 100vw-48px) x min(500px, 100vh-64px), halved to GUI units.
        float panelWidth = Math.min(400f, guiWidth - 24f);
        float panelHeight = Math.min(250f, guiHeight - 32f);
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
        UiChrome.panel(panelX, panelY, panelWidth, panelHeight);

        float leftW = 140f;
        Rects.fill(panelX + 1f, panelY + 1f, leftW - 1f, panelHeight - 2f, ClickGuiTheme.categoryBg());
        Rects.fill(panelX + leftW, panelY + 1f, 0.5f, panelHeight - 2f, ClickGuiTheme.divider());

        renderBackButton(panelX + 7f, panelY + 7f, mouseX, mouseY);
        renderCurrentColumn(panelX, panelY, leftW, panelHeight, mouseX, mouseY);
        renderAllColumn(panelX + leftW, panelY, panelWidth - leftW, panelHeight, scale, mouseX, mouseY);
        renderTooltip(panelX, panelWidth);
        renderDialog(panelX, panelY, panelWidth, panelHeight, mouseX, mouseY);

        GlStateManager.popMatrix();
        Alpha.set(1f);
    }

    @Override
    public void keyTyped(char typedChar, int keyCode) throws IOException {
        if (dialogMode == DialogMode.RENAME || dialogMode == DialogMode.CREATE) {
            if (keyCode == 1) {
                closeDialog();
                return;
            }
            renameField.textboxKeyTyped(typedChar, keyCode);
            if (keyCode == 28) {
                if (dialogMode == DialogMode.CREATE) {
                    runCreateAction();
                } else {
                    runRenameAction();
                }
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

    private void renderCurrentColumn(float panelX, float panelY, float leftW, float panelHeight, int mouseX, int mouseY) {
        ConfigProfile current = activeProfile();
        float colX = panelX + 13f;
        float colW = leftW - 26f;
        FPSMaster.fontManager.getFont(11).drawString(
                FPSMaster.i18n.get("configprofiles.current.label"),
                colX,
                panelY + 28f,
                ClickGuiTheme.accentText().getRGB()
        );

        String name = current == null ? ConfigProfileUtils.getActiveProfileName() : current.getName();
        String letter = name.isEmpty() ? "F" : name.substring(0, 1).toUpperCase(Locale.ROOT);
        Rects.rounded(colX, panelY + 38f, 32f, 32f, 9, ClickGuiTheme.accent().getRGB(), false);
        UiChrome.boldCentered(FPSMaster.fontManager.getFont(26), letter, colX + 16f, panelY + 47.5f, Color.WHITE.getRGB());

        UiChrome.boldString(FPSMaster.fontManager.s20,
                trimText(FPSMaster.fontManager.s20, name, colW),
                colX,
                panelY + 78f,
                ClickGuiTheme.textPrimary().getRGB()
        );
        String author = current == null || current.getAuthor() == null || current.getAuthor().isEmpty()
                ? FPSMaster.i18n.get("configprofiles.author.unknown")
                : current.getAuthor();
        String saved = current == null ? "" : relativeTime(current.getFile().lastModified());
        String sub = String.format(FPSMaster.i18n.get("configprofiles.author.fmt"), author)
                + (saved.isEmpty() ? "" : " · " + String.format(FPSMaster.i18n.get("configprofiles.saved.at"), saved));
        FPSMaster.fontManager.getFont(12).drawString(
                trimText(FPSMaster.fontManager.getFont(12), sub, colW),
                colX,
                panelY + 91f,
                ClickGuiTheme.textSecondary().getRGB()
        );

        float factY = panelY + 106f;
        factY = drawFact(colX, factY, colW, "box", FPSMaster.i18n.get("configprofiles.modules.enabled"), String.valueOf(countEnabledModules()));
        factY = drawFact(colX, factY, colW, "grid", FPSMaster.i18n.get("configprofiles.hud"), String.valueOf(countHudModules()));
        File file = current == null ? null : current.getFile();
        drawFact(colX, factY, colW, "folder", FPSMaster.i18n.get("configprofiles.size"), formatSize(file == null ? 0L : file.length()));

        float btnH = UiChrome.BTN_H;
        float resetY = panelY + panelHeight - 12f - btnH;
        float exportY = resetY - 4f - btnH;
        if (dialogMode == DialogMode.NONE
                && UiChrome.buttonClicked(this, colX, exportY, colW, btnH, "export",
                FPSMaster.i18n.get("configprofiles.export.share"), UiChrome.Style.DEFAULT, mouseX, mouseY)) {
            exportCurrentProfile();
        }
        if (dialogMode == DialogMode.NONE
                && UiChrome.buttonClicked(this, colX, resetY, colW, btnH, "replay",
                FPSMaster.i18n.get("configprofiles.preset.alloff"), UiChrome.Style.DANGER, mouseX, mouseY)) {
            openConfirmDialog(DialogMode.DEFAULTS, "");
        }

        String text = status;
        int color = statusColor;
        if (text.isEmpty() || System.currentTimeMillis() - statusTime > STATUS_DURATION_MS) {
            text = "";
        }
        if (!text.isEmpty()) {
            FPSMaster.fontManager.getFont(11).drawString(
                    trimText(FPSMaster.fontManager.getFont(11), text, colW),
                    colX,
                    exportY - 12f,
                    color
            );
        }
    }

    private float drawFact(float x, float y, float width, String icon, String key, String value) {
        Icons.draw(icon, x, y + 4f, 7f, ClickGuiTheme.textDisabled().getRGB());
        FPSMaster.fontManager.getFont(13).drawString(key, x + 11f, y + 4f, ClickGuiTheme.textSecondary().getRGB());
        float vw = FPSMaster.fontManager.getFont(13).getStringWidth(value);
        FPSMaster.fontManager.getFont(13).drawString(value, x + width - vw, y + 4f, ClickGuiTheme.textPrimary().getRGB());
        UiChrome.hairlineH(x, y + 15f, width);
        return y + 17f;
    }

    private void renderAllColumn(float x, float y, float width, float height, float scale, int mouseX, int mouseY) {
        UiChrome.boldString(FPSMaster.fontManager.s16,
                FPSMaster.i18n.get("configprofiles.all"),
                x + 11f,
                y + 12f,
                ClickGuiTheme.textPrimary().getRGB()
        );
        FPSMaster.fontManager.getFont(12).drawString(
                String.format(FPSMaster.i18n.get("configprofiles.count"), profiles.size()),
                x + 11f + FPSMaster.fontManager.s16.getStringWidth(FPSMaster.i18n.get("configprofiles.all")) + 5f,
                y + 13.5f,
                ClickGuiTheme.textDisabled().getRGB()
        );

        float importW = Math.max(44f, FPSMaster.fontManager.s14.getStringWidth(FPSMaster.i18n.get("configprofiles.importfile")) + 24f);
        float importH = 16f;
        float importX = x + width - 11f - importW;
        if (dialogMode == DialogMode.NONE
                && UiChrome.buttonClicked(this, importX, y + 9f, importW, importH, "import",
                FPSMaster.i18n.get("configprofiles.importfile"), UiChrome.Style.DEFAULT, mouseX, mouseY)) {
            importSelectedProfile();
        }

        // footer hint
        float footH = 15f;
        UiChrome.hairlineH(x + 1f, y + height - footH, width - 2f);
        FPSMaster.fontManager.getFont(11).drawString(
                FPSMaster.i18n.get("configprofiles.foot"),
                x + 11f, y + height - footH + 4.5f,
                ClickGuiTheme.textDisabled().getRGB());

        float gridX = x + 11f;
        float gridY = y + 30f;
        float gridW = width - 22f;
        float gridH = height - 38f - footH;
        renderProfileGrid(gridX, gridY, gridW, gridH, scale, mouseX, mouseY);
    }

    private void renderProfileGrid(float x, float y, float width, float height, float scale, int mouseX, int mouseY) {
        final float gap = 5f;
        final float cardH = 59f;
        final int cols = width > 180f ? 2 : 1;
        final float cardW = (width - gap * (cols - 1)) / cols;
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
                int count = profiles.size() + 1;
                float scroll = scrollContainer.getScroll();
                for (int i = 0; i < count; i++) {
                    int col = i % cols;
                    int row = i / cols;
                    float cx = x + col * (cardW + gap);
                    float cy = y + row * (cardH + gap) + scroll;
                    if (cy + cardH < y || cy > y + height) {
                        continue;
                    }
                    if (i < profiles.size()) {
                        renderGridCard(profiles.get(i), cx, cy, cardW, cardH, x, y, width, height, mouseX, mouseY);
                    } else {
                        renderNewCard(cx, cy, cardW, cardH, x, y, width, height, mouseX, mouseY);
                    }
                }
                int rows = (count + cols - 1) / cols;
                scrollContainer.setHeight(rows * (cardH + gap));
            } finally {
                GL11.glDisable(GL11.GL_SCISSOR_TEST);
            }
        });
    }

    private void renderGridCard(ConfigProfile profile, float x, float y, float width, float height, float listX, float listY, float listWidth, float listHeight, int mouseX, int mouseY) {
        boolean interactive = dialogMode == DialogMode.NONE && !close;
        boolean pointerInsideList = Hover.is(listX, listY, listWidth, listHeight, mouseX, mouseY);
        boolean active = profile.getName().equals(ConfigProfileUtils.getActiveProfileName());
        boolean hovered = interactive && pointerInsideList && Hover.is(x, y, width, height, mouseX, mouseY);
        if (active) {
            UiChrome.selectedSurface(x, y, width, height, UiChrome.PANEL_RADIUS);
        } else {
            Rects.rounded(x - 0.5f, y - 0.5f, width + 1f, height + 1f, UiChrome.PANEL_RADIUS + 1,
                    ClickGuiTheme.stroke().getRGB(), false);
            Rects.rounded(x, y, width, height, UiChrome.PANEL_RADIUS,
                    (hovered ? ClickGuiTheme.cardHoverBg() : ClickGuiTheme.cardBg()).getRGB(), false);
        }

        String letter = profile.getName().isEmpty() ? "P" : profile.getName().substring(0, 1).toUpperCase(Locale.ROOT);
        Rects.rounded(x + 8f, y + 8f, 20f, 20f, 6,
                (active ? ClickGuiTheme.accent() : ClickGuiTheme.layerActive()).getRGB(), false);
        UiChrome.boldCentered(FPSMaster.fontManager.s16, letter, x + 18f, y + 14f, Color.WHITE.getRGB());
        FPSMaster.fontManager.s14.drawString(
                trimText(FPSMaster.fontManager.s14, profile.getName(), width - 76f),
                x + 34f,
                y + 9f,
                ClickGuiTheme.textPrimary().getRGB()
        );
        String meta = profile.getAuthor() == null || profile.getAuthor().isEmpty()
                ? relativeTime(profile.getFile().lastModified())
                : profile.getAuthor() + " · " + relativeTime(profile.getFile().lastModified());
        FPSMaster.fontManager.getFont(11).drawString(
                trimText(FPSMaster.fontManager.getFont(11), meta, width - 76f),
                x + 34f,
                y + 19f,
                ClickGuiTheme.textDisabled().getRGB()
        );

        boolean manageable = !ConfigProfileUtils.CURRENT_CONFIG.equals(profile.getName());
        if (hovered) {
            float opY = y + 6f;
            float opX = x + width - 6f - 14f;
            if (manageable) {
                if (renderIconButton("delete." + profile.getName(), opX, opY, 14f, "delete", "configprofiles.delete", true, pointerInsideList, mouseX, mouseY)) {
                    openConfirmDialog(DialogMode.DELETE, profile.getName());
                    return;
                }
                opX -= 16f;
            }
            if (renderIconButton("rename." + profile.getName(), opX, opY, 14f, "rename", "configprofiles.rename", false, pointerInsideList, mouseX, mouseY)) {
                openRenameDialog(profile.getName());
                return;
            }
        }

        if (active) {
            Icons.draw("check", x + 8f, y + height - 17f, 6.5f, ClickGuiTheme.accentText().getRGB());
            FPSMaster.fontManager.getFont(12).drawString(
                    FPSMaster.i18n.get("configprofiles.inuse"),
                    x + 17.5f,
                    y + height - 16.5f,
                    ClickGuiTheme.accentText().getRGB()
            );
        } else {
            float applyW = width - 16f;
            float applyH = 16f;
            float applyX = x + 8f;
            float applyY = y + height - applyH - 8f;
            boolean applyHov = hovered && Hover.is(applyX, applyY, applyW, applyH, mouseX, mouseY);
            UiChrome.button(applyX, applyY, applyW, applyH, applyHov);
            FPSMaster.fontManager.getFont(12).drawCenteredString(
                    FPSMaster.i18n.get("configprofiles.apply"),
                    applyX + applyW / 2f,
                    applyY + 5f,
                    ClickGuiTheme.textPrimary().getRGB()
            );
            if (interactive && pointerInsideList && consumePressInBounds(applyX, applyY, applyW, applyH, 0) != null) {
                openConfirmDialog(DialogMode.LOAD, profile.getName());
            }
        }
    }

    private void renderNewCard(float x, float y, float width, float height, float listX, float listY, float listWidth, float listHeight, int mouseX, int mouseY) {
        boolean interactive = dialogMode == DialogMode.NONE && !close;
        boolean inside = Hover.is(listX, listY, listWidth, listHeight, mouseX, mouseY);
        boolean hovered = interactive && inside && Hover.is(x, y, width, height, mouseX, mouseY);
        // dashed border stand-in: hairline border, layer fill only on hover
        Rects.rounded(x - 0.5f, y - 0.5f, width + 1f, height + 1f, UiChrome.PANEL_RADIUS + 1,
                ClickGuiTheme.strokeStrong().getRGB(), false);
        Rects.rounded(x, y, width, height, UiChrome.PANEL_RADIUS,
                (hovered ? ClickGuiTheme.layer() : ClickGuiTheme.glass()).getRGB(), false);
        int color = (hovered ? ClickGuiTheme.textPrimary() : ClickGuiTheme.textSecondary()).getRGB();
        String label = FPSMaster.i18n.get("configprofiles.new");
        float labelW = FPSMaster.fontManager.getFont(13).getStringWidth(label);
        float cx = x + (width - labelW - 11f) / 2f;
        Icons.draw("plus", cx, y + height / 2f - 3.5f, 7f, color);
        FPSMaster.fontManager.getFont(13).drawString(label, cx + 11f, y + height / 2f - 3f, color);
        if (interactive && inside && consumePressInBounds(x, y, width, height, 0) != null) {
            openCreateDialog();
        }
    }

    private boolean renderIconButton(String animKey, float x, float y, float size, String icon, String tooltipKey, boolean danger, boolean enabled, int mouseX, int mouseY) {
        boolean interactive = enabled && dialogMode == DialogMode.NONE && !close;
        boolean hovered = interactive && Hover.is(x, y, size, size, mouseX, mouseY);
        Color base = ClickGuiTheme.mask(90);
        Color hoverColor = danger ? withAlpha(ClickGuiTheme.danger(), 200) : ClickGuiTheme.buttonHoverBg();
        ColorAnimator bg = anim(animKey, base);
        bg.animateTo(hovered ? hoverColor : base, 0.12, Easings.QUAD_OUT);
        bg.update();
        Rects.rounded(x, y, size, size, 4, bg.get().getRGB(), false);
        int iconColor = danger && hovered ? Color.WHITE.getRGB() : ClickGuiTheme.textPrimary().getRGB();
        float pad = (size - 7f) / 2f;
        Icons.draw(icon, x + pad, y + pad, 7f, iconColor);
        if (hovered && tooltipKey != null) {
            tooltip = FPSMaster.i18n.get(tooltipKey);
            tooltipX = x + size / 2f;
            tooltipY = y + size + 3f;
        }
        return interactive && consumePressInBounds(x, y, size, size, 0) != null;
    }

    private void renderTooltip(float panelX, float panelWidth) {
        if (tooltip == null || tooltip.isEmpty()) {
            return;
        }
        float width = FPSMaster.fontManager.getFont(12).getStringWidth(tooltip) + 8f;
        float height = 11f;
        float x = Math.max(panelX + 4f, Math.min(tooltipX - width / 2f, panelX + panelWidth - 4f - width));
        Rects.rounded(x, tooltipY, width, height, 4, tooltipBackground().getRGB(), false);
        FPSMaster.fontManager.getFont(12).drawCenteredString(tooltip, x + width / 2f, tooltipY + 3f, ClickGuiTheme.textPrimary().getRGB());
    }

    private void renderDialog(float panelX, float panelY, float panelWidth, float panelHeight, int mouseX, int mouseY) {
        if (dialogMode == DialogMode.NONE) {
            return;
        }

        float progress = (float) dialogAnim.get();
        Rects.rounded(panelX, panelY, panelWidth, panelHeight, UiChrome.PANEL_RADIUS, ClickGuiTheme.mask((int) (90 * progress)).getRGB(), false);

        float dialogWidth = 170f;
        float dialogHeight = (dialogMode == DialogMode.RENAME || dialogMode == DialogMode.CREATE) ? 74f : 58f;
        float dialogX = panelX + (panelWidth - dialogWidth) / 2f;
        float dialogY = panelY + (panelHeight - dialogHeight) / 2f - 4f;

        float outerAlpha = (float) (alphaAnimation.get() / 255.0);
        Alpha.set(outerAlpha * progress);
        GlStateManager.pushMatrix();
        float dialogCenterX = dialogX + dialogWidth / 2f;
        float dialogCenterY = dialogY + dialogHeight / 2f;
        float dialogScale = 0.92f + 0.08f * progress;
        GlStateManager.translate(dialogCenterX, dialogCenterY, 0f);
        GL11.glScaled(dialogScale, dialogScale, 1.0);
        GlStateManager.translate(-dialogCenterX, -dialogCenterY, 0f);

        Rects.rounded(dialogX - 0.5f, dialogY - 0.5f, dialogWidth + 1f, dialogHeight + 1f, UiChrome.PANEL_RADIUS + 1,
                ClickGuiTheme.stroke().getRGB(), false);
        Rects.rounded(dialogX, dialogY, dialogWidth, dialogHeight, UiChrome.PANEL_RADIUS, dialogBackground().getRGB(), false);
        if (dialogMode == DialogMode.RENAME || dialogMode == DialogMode.CREATE) {
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
        UiChrome.boldString(FPSMaster.fontManager.s14,
                FPSMaster.i18n.get(dialogMode == DialogMode.CREATE ? "configprofiles.create.title" : "configprofiles.rename.title"),
                dialogX + 11f,
                dialogY + 9f,
                ClickGuiTheme.textPrimary().getRGB()
        );

        renameField.backGroundColor = 0;
        renameField.fontColor = ClickGuiTheme.textFieldText().getRGB();
        renameField.placeHolder = FPSMaster.i18n.get("configprofiles.name.placeholder");
        UiChrome.inputBox(dialogX + 11f, dialogY + 23f, dialogWidth - 22f, 17f, renameField.isFocused());
        renameField.drawTextBox(dialogX + 15f, dialogY + 24f, dialogWidth - 30f, 15f);
        handleRenameFieldClick(dialogX + 11f, dialogY + 23f, dialogWidth - 22f, 17f);

        float btnY = dialogY + 48f;
        if (renderDialogButton("dialog.save", dialogX + dialogWidth - 11f - 45f, btnY, 45f, 18f, "configprofiles.save",
                ClickGuiTheme.accent(), accentHover(), Color.WHITE.getRGB(), mouseX, mouseY)) {
            if (dialogMode == DialogMode.CREATE) {
                runCreateAction();
            } else {
                runRenameAction();
            }
        }
        if (renderDialogButton("dialog.cancel", dialogX + dialogWidth - 11f - 45f - 5f - 40f, btnY, 40f, 18f, "configprofiles.cancel",
                ClickGuiTheme.buttonBg(), ClickGuiTheme.buttonHoverBg(), ClickGuiTheme.textPrimary().getRGB(), mouseX, mouseY)) {
            closeDialog();
        }
    }

    private void renderConfirmDialog(float dialogX, float dialogY, float dialogWidth, int mouseX, int mouseY) {
        boolean destructive = dialogMode == DialogMode.DELETE || dialogMode == DialogMode.DEFAULTS;
        UiChrome.boldString(FPSMaster.fontManager.s14,
                trimText(FPSMaster.fontManager.s14, getConfirmMessage(), dialogWidth - 22f),
                dialogX + 11f,
                dialogY + 11f,
                ClickGuiTheme.textPrimary().getRGB()
        );

        float btnY = dialogY + 32f;
        Color confirmBase = destructive ? ClickGuiTheme.danger() : ClickGuiTheme.accent();
        Color confirmHover = destructive ? dangerHover() : accentHover();
        String confirmKey = destructive ? "dialog.confirm.danger" : "dialog.confirm.accent";
        if (renderDialogButton(confirmKey, dialogX + dialogWidth - 11f - 45f, btnY, 45f, 18f, "configprofiles.confirm",
                confirmBase, confirmHover, Color.WHITE.getRGB(), mouseX, mouseY)) {
            runConfirmAction();
        }
        if (renderDialogButton("dialog.cancel", dialogX + dialogWidth - 11f - 45f - 5f - 40f, btnY, 40f, 18f, "configprofiles.cancel",
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
        Rects.rounded(x, y, width, height, UiChrome.CTL_RADIUS, bg.get().getRGB(), false);
        FPSMaster.fontManager.s14.drawCenteredString(FPSMaster.i18n.get(textKey), x + width / 2f, y + height / 2f - 3.5f, textColor);
        return consumePressInBounds(x, y, width, height, 0) != null;
    }

    private void renderBackButton(float x, float y, int mouseX, int mouseY) {
        boolean hovered = Hover.is(x, y, 16f, 16f, mouseX, mouseY);
        UiChrome.ghostButton(x, y, 16f, 16f, hovered);
        Icons.draw("back", x + 4.5f, y + 4.5f, 7f,
                (hovered ? ClickGuiTheme.textPrimary() : ClickGuiTheme.textSecondary()).getRGB());
        if (consumePressInBounds(x, y, 16f, 16f, 0) != null) {
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

    private void openCreateDialog() {
        dialogMode = DialogMode.CREATE;
        dialogProfileName = "";
        dialogAnim.start(0.0, 1.0, 0.18f, Easings.CUBIC_OUT);
        renameField.setText(nextProfileName());
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

    private void runCreateAction() {
        String name = renameField.getText();
        closeDialog();
        createProfile(name);
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

    private void createProfile(String name) {
        try {
            String created = ConfigProfileUtils.saveCurrentAs(name);
            reloadProfiles();
            setStatus(String.format(FPSMaster.i18n.get("configprofiles.status.renamed"), created), successColor());
        } catch (FileException exception) {
            ClientLogger.error("Failed to create config profile: " + exception.getMessage());
            setStatus(FPSMaster.i18n.get("configprofiles.status.rename_failed"), errorColor());
        }
    }

    private ConfigProfile activeProfile() {
        String active = ConfigProfileUtils.getActiveProfileName();
        for (ConfigProfile profile : profiles) {
            if (profile.getName().equals(active)) {
                return profile;
            }
        }
        return profiles.isEmpty() ? null : profiles.get(0);
    }

    private int countEnabledModules() {
        int n = 0;
        if (FPSMaster.moduleManager == null) {
            return 0;
        }
        for (top.fpsmaster.features.manager.Module module : FPSMaster.moduleManager.modules) {
            if (module.isEnabled() && !"ClientSettings".equals(module.name)) {
                n++;
            }
        }
        return n;
    }

    private int countHudModules() {
        int n = 0;
        if (FPSMaster.moduleManager == null) {
            return 0;
        }
        for (top.fpsmaster.features.manager.Module module : FPSMaster.moduleManager.modules) {
            if (module instanceof top.fpsmaster.features.impl.InterfaceModule && module.isEnabled()) {
                n++;
            }
        }
        return n;
    }

    private String nextProfileName() {
        int index = profiles.size() + 1;
        String candidate = "profile-" + index;
        while (nameTaken(candidate)) {
            index++;
            candidate = "profile-" + index;
        }
        return candidate;
    }

    private boolean nameTaken(String name) {
        for (ConfigProfile profile : profiles) {
            if (profile.getName().equalsIgnoreCase(name)) {
                return true;
            }
        }
        return false;
    }

    private static String formatSize(long bytes) {
        if (bytes < 1024L) {
            return bytes + " B";
        }
        if (bytes < 1024L * 1024L) {
            return (bytes / 1024L) + " KB";
        }
        return String.format(Locale.ROOT, "%.1f MB", bytes / (1024.0 * 1024.0));
    }

    private static String relativeTime(long millis) {
        if (millis <= 0L) {
            return "";
        }
        long delta = Math.max(0L, System.currentTimeMillis() - millis);
        long minutes = delta / 60000L;
        if (minutes < 1L) {
            return FPSMaster.i18n.get("configprofiles.time.now");
        }
        if (minutes < 60L) {
            return String.format(FPSMaster.i18n.get("configprofiles.time.minutes"), minutes);
        }
        long hours = minutes / 60L;
        if (hours < 24L) {
            return String.format(FPSMaster.i18n.get("configprofiles.time.hours"), hours);
        }
        long days = hours / 24L;
        return String.format(FPSMaster.i18n.get("configprofiles.time.days"), days);
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

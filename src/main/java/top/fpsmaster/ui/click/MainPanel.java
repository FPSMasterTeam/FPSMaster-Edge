package top.fpsmaster.ui.click;

import top.fpsmaster.utils.render.gui.UiScale;
import top.fpsmaster.utils.render.state.Alpha;
import top.fpsmaster.utils.render.draw.Images;
import top.fpsmaster.utils.render.draw.Hover;
import top.fpsmaster.utils.render.draw.Colors;
import top.fpsmaster.utils.render.draw.Icons;
import top.fpsmaster.utils.render.draw.Rects;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;
import top.fpsmaster.FPSMaster;
import top.fpsmaster.exception.FileException;
import top.fpsmaster.features.manager.Category;
import top.fpsmaster.features.manager.Module;
import top.fpsmaster.modules.config.ConfigProfileUtils;
import top.fpsmaster.modules.logger.ClientLogger;
import top.fpsmaster.ui.click.component.ScrollContainer;
import top.fpsmaster.ui.click.modules.ModuleRenderer;
import top.fpsmaster.utils.math.anim.AnimMath;
import top.fpsmaster.utils.math.anim.AnimClock;
import top.fpsmaster.utils.math.anim.Animator;
import top.fpsmaster.utils.math.anim.BezierEasing;
import top.fpsmaster.utils.math.anim.ColorAnimator;
import top.fpsmaster.utils.math.anim.Easings;
import top.fpsmaster.features.impl.interfaces.ClientSettings;
import top.fpsmaster.utils.render.gui.ScaledGuiScreen;
import top.fpsmaster.utils.render.gui.Scissor;

import java.awt.*;
import java.io.IOException;
import java.util.LinkedList;
import java.util.Locale;

public class MainPanel extends ScaledGuiScreen {
    boolean drag = false;
    float dragX = 0f;
    float dragY = 0f;
    Category curType = Category.OPTIMIZE;
    LinkedList<CategoryComponent> categories = new LinkedList<>();
    float modsWheel = 0f;
    float wheelTemp = 0f;
    private boolean zoomModifierHeld = false;

    private final Animator scaleAnimation = new Animator();
    private final Animator alphaAnimation = new Animator();
    private final Animator maskAlpha = new Animator();
    private final Animator themeSwitchAnim = new Animator();
    private final ColorAnimator themeBtnAnim = new ColorAnimator(ClickGuiTheme.themeBtnBg());
    private final ColorAnimator configBtnAnim = new ColorAnimator(ClickGuiTheme.themeBtnBg());
    private final ColorAnimator musicBtnAnim = new ColorAnimator(ClickGuiTheme.themeBtnBg());
    private final SearchBar searchBar = new SearchBar();
    private final AnimClock animClock = new AnimClock();
    private static final BezierEasing CLICKGUI_EASE = BezierEasing.of(0.25, 0.1, 0.25, 1.0);
    private static final int MASK_MAX_ALPHA = 110;

    float selection = 0f;


    float categoryAnimation = 30;

    boolean close = false;
    private boolean configSavedOnClose;

    float moduleListAlpha = 0f;
    float modHeight = 0f;
    ScrollContainer modsContainer = new ScrollContainer();

    public LinkedList<ModuleRenderer> mods = new LinkedList<>();

    static int x = -1;
    static int y = -1;
    static float width = 430f;
    static float height = 245.5f;
    public static final float leftWidth = 50f;
    public static String bindLock = "";
    public static Module curModule = null;
    public MainPanel() {
        super();
    }
    private float getCategoryItemSpacing() {
        return 27f;
    }

    private float getCategoryListHeight() {
        return categories.size() * getCategoryItemSpacing();
    }

    private float getCategoryBgHeight() {
        return Math.max(40f, getCategoryListHeight() + 8f);
    }

    private float getCategoryBgY() {
        return y + (height - getCategoryBgHeight()) / 2f;
    }

    private float getCategoryStartY() {
        return getCategoryBgY() + 10f;
    }

    // author:Serendisand
    // reason:全局搜索
    private float getSearchBarX() {
        return x + width + 8 - getSearchBarWidth();
    }

    private float getSearchBarY() {
        return y - 3 - getSearchBarHeight();
    }

    private float getSearchBarWidth() {
        return 130f;
    }

    private float getSearchBarHeight() {
        return 18f;
    }

    private boolean isSearchActive() {
        return !searchBar.getQuery().trim().isEmpty();
    }

    private boolean matchesSearch(Module module, String rawQuery) {
        String query = rawQuery.trim().toLowerCase(Locale.getDefault());
        if (query.isEmpty()) {
            return false;
        }
        String key = module.name.toLowerCase(Locale.getDefault());
        String name = FPSMaster.i18n.get(key);
        String desc = FPSMaster.i18n.get(key + ".desc");
        return name.toLowerCase(Locale.getDefault()).contains(query)
                || module.name.toLowerCase(Locale.getDefault()).contains(query)
                || desc.toLowerCase(Locale.getDefault()).contains(query);
    }

    @Override
    public void render(int mouseX, int mouseY, float partialTicks) {
        updateZoomModifierState();
        //aiChatPanel.render(mouseX, mouseY, scaleFactor);
        x = (int) ( guiWidth - width) / 2;
        y = (int) (guiHeight - height) / 2;
        if (!isMouseDown(0)) {
            drag = false;
        }

//        if (drag) {
//            mouseY -= (int) dragY;
//            x = (int) (mouseX - dragX);
//            y = mouseY;
//        }

        x = (int) Math.max(0, Math.min(guiWidth - (int) width, x));
        y = (int) Math.max(0, Math.min(guiHeight - (int) height, y));

        if (close) {
            if (scaleAnimation.get() <= 0.7) {
                mc.displayGuiScreen(null);
                if (mc.currentScreen == null) {
                    mc.setIngameFocus();
                }
            }
        }
        double dt = animClock.tick();
        scaleAnimation.update(dt);
        alphaAnimation.update(dt);
        maskAlpha.update(dt);
        Alpha.set(1f);
        Rects.fill(0f, 0f, guiWidth, guiHeight, ClickGuiTheme.mask((int) maskAlpha.get()));
        Alpha.set((float) alphaAnimation.get() / 255f);

        GlStateManager.translate(guiWidth / 2.0, guiHeight / 2.0, 0.0);
        GL11.glScaled(scaleAnimation.get(), scaleAnimation.get(), 0.0);
        GlStateManager.translate(-guiWidth / 2.0, -guiHeight / 2.0, 0.0);


        Images.draw(new ResourceLocation("client/gui/settings/window/panel.png"),
                x + leftWidth - 8,
                y - 2,
                width - leftWidth + 16,
                height + 12,
                -1
        );

        if (ClickGuiTheme.isLight()) {
            Rects.rounded(Math.round(x + leftWidth), Math.round(y + 4),
                    Math.round(width - leftWidth), Math.round(height),
                    8, ClickGuiTheme.panelBg());
        }

        searchBar.draw(this, getSearchBarX(), getSearchBarY(), getSearchBarWidth(), getSearchBarHeight(), mouseX, mouseY);

        moduleListAlpha = (float) AnimMath.base(moduleListAlpha, 255.0, 0.1f);

        float scale = (float) scaleAnimation.get();
        float centerX = guiWidth / 2f;
        float centerY = guiHeight / 2f;
        float scissorX = centerX + (x - centerX) * scale;
        float scissorY = centerY + (y - centerY + 10) * scale;
        float scissorW = width * scale;
        float scissorH = (height - 12) * scale;

        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        Scissor.apply(
                scissorX, scissorY, scissorW,
                scissorH
        );
        modHeight = 20f;
        float containerWidth = width - leftWidth - 10;
        int finalMouseY = mouseY;
        boolean searching = isSearchActive();
        String query = searchBar.getQuery();
        modsContainer.draw(this, x + leftWidth, y + 25f, containerWidth, height - 20f, mouseX, mouseY, () -> {
            float modsY = y + 22f;
            boolean anyMatch = false;
            for (ModuleRenderer m : mods) {
                m.highlight = searching ? query.trim() : null;
                m.searchMode = searching;
                boolean show = searching ? matchesSearch(m.mod, query) : m.mod.category == curType;
                if (show) {
                    anyMatch = true;
                    float moduleY = modsY + modsContainer.getScroll();
                    if (moduleY + 40 + m.height > y && moduleY < y + height) {
                        m.render(
                                this,
                                x + leftWidth + 10,
                                moduleY,
                                containerWidth - 10,
                                40f,
                                mouseX,
                                finalMouseY,
                                curModule == m.mod
                        );
                    }
                    modsY += 45 + m.height;
                    modHeight += 45 + m.height;
                }
            }
            if (searching && !anyMatch) {
                FPSMaster.fontManager.s16.drawCenteredString(
                        FPSMaster.i18n.get("clickgui.search.noresults"),
                        x + leftWidth + containerWidth / 2f,
                        y + 25f + (height - 20f) / 2f,
                        ClickGuiTheme.textDisabled().getRGB()
                );
            }
            modsContainer.setHeight(modHeight);
        });
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glDisable(GL11.GL_SCISSOR_TEST);


        float categoryBgHeight = getCategoryBgHeight();
        float categoryBgY = getCategoryBgY();
        float categoryStartY = getCategoryStartY();

        if (Hover.is(x, (int) categoryBgY, categoryAnimation, categoryBgHeight, mouseX, mouseY)) {
            categoryAnimation = (float) AnimMath.base(categoryAnimation, 100f, 0.15f);
        } else {
            categoryAnimation = (float) AnimMath.base(categoryAnimation, 30f, 0.15f);
        }

        Rects.roundedImage(
                Math.round(x + categoryAnimation / 50f),
                Math.round(categoryBgY),
                Math.round(categoryAnimation),
                Math.round(categoryBgHeight),
                10,
                ClickGuiTheme.categoryBg()
        );

        float my = categoryStartY;
        Rects.roundedImage(
                Math.round(x + 4 + categoryAnimation / 50f),
                Math.round(selection - 6),
                Math.round(categoryAnimation - 8),
                22,
                10,
                ClickGuiTheme.categorySelection()
        );


        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        float categoryScissorX = centerX + (x - centerX) * scale;
        float categoryScissorY = centerY + (categoryBgY - centerY) * scale;
        float categoryScissorW = categoryAnimation * scale;
        float categoryScissorH = categoryBgHeight * scale;
        Scissor.apply(
                categoryScissorX, categoryScissorY, categoryScissorW,
                categoryScissorH
        );

        for (CategoryComponent m : categories) {
            if (Hover.is(x, my - 6, leftWidth - 10, 20f, mouseX, mouseY)) {
                m.categorySelectionColor.animateTo(ClickGuiTheme.categoryHover(), 0.15f, Easings.QUAD_OUT);
            } else {
                m.categorySelectionColor.animateTo(Colors.alpha(ClickGuiTheme.categoryHover(), 0), 0.15f, Easings.QUAD_OUT);
            }
            m.categorySelectionColor.update(dt);

            if (m.category == curType) {
                selection = drag
                        ? my
                        : (float) AnimMath.base(selection, my, 0.2);
            }

            m.render(
                    x + categoryAnimation / 50f,
                    my,
                    leftWidth - 10,
                    20f,
                    mouseX,
                    mouseY,
                    curType == m.category,
                    dt
            );
            my += 27f;
        }
        GL11.glDisable(GL11.GL_SCISSOR_TEST);

        // Theme / config buttons (bottom-left)
        themeSwitchAnim.update(dt);
        float sideBtnX = x + 4 + categoryAnimation / 50f;
        float sideBtnW = categoryAnimation - 8;
        float sideBtnH = 15;
        boolean isLightTheme = ClickGuiTheme.isLight();
        if (renderMusicButton(sideBtnX, y + height - 53, sideBtnW, sideBtnH, "Music", musicBtnAnim, mouseX, mouseY)) {
            mc.displayGuiScreen(new top.fpsmaster.ui.screens.music.MusicScreen());
        }
        String themeLabel = FPSMaster.i18n.get(isLightTheme ? "theme.light" : "theme.dark");
        if (renderSideButton(sideBtnX, y + height - 36, sideBtnW, sideBtnH, themeLabel, themeBtnAnim, true, mouseX, mouseY)) {
            ClientSettings.theme.setValue(isLightTheme ? 0 : 1);
            themeSwitchAnim.animateTo(isLightTheme ? 0.0 : 1.0, 0.35, Easings.CUBIC_OUT);
        }
        if (renderSideButton(sideBtnX, y + height - 19, sideBtnW, sideBtnH, FPSMaster.i18n.get("configprofiles.button"), configBtnAnim, false, mouseX, mouseY)) {
            mc.displayGuiScreen(new ConfigProfilesScreen(this));
        }

        Alpha.set(1f);

        handlePointerPress();
    }

    private boolean renderSideButton(float x, float y, float width, float height, String text, ColorAnimator bgAnim, boolean themeIcon, int mouseX, int mouseY) {
        boolean hovered = Hover.is(x, y, width, height, mouseX, mouseY);
        bgAnim.animateTo(hovered ? ClickGuiTheme.sideBtnHoverBg() : ClickGuiTheme.themeBtnBg(), 0.15, Easings.QUAD_OUT);
        bgAnim.update();
        Rects.rounded(Math.round(x), Math.round(y), Math.round(width), Math.round(height), 4, bgAnim.get().getRGB());

        int contentColor = ClickGuiTheme.themeBtnText().getRGB();
        float iconSize = 10f;
        float textWidth = FPSMaster.fontManager.s14.getStringWidth(text);
        boolean showText = width >= iconSize + textWidth + 16f;
        float contentWidth = showText ? iconSize + 4f + textWidth : iconSize;
        float contentX = x + (width - contentWidth) / 2f;
        float iconY = y + (height - iconSize) / 2f;
        if (themeIcon) {
            float progress = (float) themeSwitchAnim.get();
            if (progress < 0.999f) {
                Icons.draw("moon", contentX, iconY, iconSize, fade(contentColor, 1f - progress));
            }
            if (progress > 0.001f) {
                Icons.draw("sun", contentX, iconY, iconSize, fade(contentColor, progress));
            }
        } else {
            Icons.draw("sliders", contentX, iconY, iconSize, contentColor);
        }
        if (showText) {
            FPSMaster.fontManager.s14.drawString(text, contentX + iconSize + 4f, y + height / 2f - 4f, contentColor);
        }
        return consumePressInBounds(x, y, width, height) != null;
    }

    private boolean renderMusicButton(float x, float y, float width, float height, String text, ColorAnimator bgAnim, int mouseX, int mouseY) {
        boolean hovered = Hover.is(x, y, width, height, mouseX, mouseY);
        bgAnim.animateTo(hovered ? ClickGuiTheme.sideBtnHoverBg() : ClickGuiTheme.themeBtnBg(), 0.15, Easings.QUAD_OUT);
        bgAnim.update();
        Rects.rounded(Math.round(x), Math.round(y), Math.round(width), Math.round(height), 4, bgAnim.get().getRGB());

        int contentColor = ClickGuiTheme.themeBtnText().getRGB();
        float iconSize = 10f;
        float textWidth = FPSMaster.fontManager.s14.getStringWidth(text);
        boolean showText = width >= iconSize + textWidth + 16f;
        float contentWidth = showText ? iconSize + 4f + textWidth : iconSize;
        float contentX = x + (width - contentWidth) / 2f;
        float iconY = y + (height - iconSize) / 2f;
        Icons.draw("music", contentX, iconY, iconSize, contentColor);
        if (showText) {
            FPSMaster.fontManager.s14.drawString(text, contentX + iconSize + 4f, y + height / 2f - 4f, contentColor);
        }
        return consumePressInBounds(x, y, width, height) != null;
    }

    private int fade(int color, float alpha) {
        Color c = new Color(color, true);
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), Colors.clamp((int) (c.getAlpha() * alpha))).getRGB();
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
    }

    @Override
    public void initGui() {
        super.initGui();
//        aiChatPanel.init();
        animClock.reset();
        themeSwitchAnim.set(ClickGuiTheme.isLight() ? 1.0 : 0.0);
        scaleAnimation.start(0.8, 1.0, 0.2f, CLICKGUI_EASE);
        alphaAnimation.start(0.0, 255.0, 0.2f, CLICKGUI_EASE);
        maskAlpha.start(0.0, MASK_MAX_ALPHA, 0.2f, CLICKGUI_EASE);
        close = false;
        configSavedOnClose = false;
        searchBar.clear();
        searchBar.setFocused(false);

//        if (width == 0f || height == 0f) {
//            width = scaledWidth / 2f;
//            height = scaledHeight / 2f;
//        }


        categories.clear();
        for (Category c : Category.values()) {
            categories.add(new CategoryComponent(c));
        }

        selection = y + height / 2f;
    }

    @Override
    public void onResize(Minecraft mcIn, int w, int h) {
        super.onResize(mcIn, w, h);
    }

    @Override
    public void onGuiClosed() {
        super.onGuiClosed();
        saveConfigOnClose();
    }

    @Override
    public void keyTyped(char typedChar, int keyCode) throws IOException {
//        aiChatPanel.keyTyped(typedChar, keyCode);

        if (searchBar.isFocused()) {
            searchBar.keyTyped(typedChar, keyCode);
            return;
        }

        if (keyCode == 1) {
            if (scaleAnimation.isRunning() || scaleAnimation.get() != 0.7) {
                requestClose();
            }
            return;
        }

        for (ModuleRenderer m : mods) {
            if (m.mod.category == curType) {
                m.keyTyped(typedChar, keyCode);
            }
        }

        super.keyTyped(typedChar, keyCode);
    }

    @Override
    protected float[] getOccludingBounds() {
        // Only the panel itself, not the whole screen: dragging a HUD element that sits beside the
        // ClickGUI is exactly what the user opened it for.
        return new float[]{x, y, width, height};
    }

    private void handlePointerPress() {
        // author:Serendisand
        // reason:全局搜索
        if (searchBar.isFocused()) {
            ScaledGuiScreen.PointerEvent rawPress = peekRawPress();
            if (rawPress != null && !Hover.is(getSearchBarX(), getSearchBarY(), getSearchBarWidth(), getSearchBarHeight(), rawPress.x, rawPress.y)) {
                searchBar.setFocused(false);
            }
        }

        ScaledGuiScreen.PointerEvent press = peekAnyPress();
        if (press == null) {
            return;
        }

        int mouseX = (int) press.x;
        int mouseY = (int) press.y;
        int mouseButton = press.button;

        if (!Hover.is(x, y, width, height, mouseX, mouseY)) {
            return;
        }

        // A hasPointerCapture() guard used to sit here. It compensated for peekAnyPress() returning
        // presses that another widget had already consumed — beginDrag consumes the press that starts
        // a slider drag, yet the category strip still saw it and switched category underneath. Now that
        // peekAnyPress() skips consumed presses the guard is redundant: the press is gone on the frame
        // a drag begins, and later frames of the same drag produce no press at all.
        float my = getCategoryStartY();
        for (Category c : Category.values()) {
            if (Hover.is(x, my - 8, leftWidth, 24f, mouseX, mouseY)) {
                if (isSearchActive()) {
                    searchBar.clear();
                }
                wheelTemp = 0f;
                modsWheel = 0f;
                if (curType != c) {
                    moduleListAlpha = 0f;
                }
                curType = c;
            }
            my += 27f;
        }

        if (mouseButton == 0) {
            consumePressInBounds(x, y, width, height, mouseButton);
        }
    }

    private void updateZoomModifierState() {
        zoomModifierHeld = ClientSettings.isZoomBindDown();
    }

    @Override
    protected void mouseScrolled(int mouseX, int mouseY, int wheelDelta) {
        super.mouseScrolled(mouseX, mouseY, wheelDelta);
        if (zoomModifierHeld) {
            if (wheelDelta > 0) {
                ClientSettings.fixedScale.setValue(ClientSettings.fixedScale.getValue() + 1);
            } else if (wheelDelta < 0) {
                ClientSettings.fixedScale.setValue(ClientSettings.fixedScale.getValue() - 1);
            }
        }
    }

    private void requestClose() {
        saveConfigOnClose();
        close = true;
        scaleAnimation.animateTo(0.7, 0.1f, CLICKGUI_EASE);
        alphaAnimation.animateTo(0.0, 0.1f, CLICKGUI_EASE);
        maskAlpha.animateTo(0.0, 0.1f, CLICKGUI_EASE);
    }

    private void saveConfigOnClose() {
        if (configSavedOnClose) {
            return;
        }
        try {
            FPSMaster.configManager.saveConfig(ConfigProfileUtils.getActiveProfileName());
            configSavedOnClose = true;
        } catch (FileException e) {
            ClientLogger.error("Failed to save config when closing MainPanel: " + e.getMessage());
            e.printStackTrace();
        }
    }
}





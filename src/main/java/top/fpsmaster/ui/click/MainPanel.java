package top.fpsmaster.ui.click;

import top.fpsmaster.utils.render.state.Alpha;
import top.fpsmaster.utils.render.draw.Hover;
import top.fpsmaster.utils.render.draw.Colors;
import top.fpsmaster.utils.render.draw.Icons;
import top.fpsmaster.utils.render.draw.Rects;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
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
    public Category curType = Category.OPTIMIZE;
    LinkedList<CategoryComponent> categories = new LinkedList<>();
    float modsWheel = 0f;
    float wheelTemp = 0f;
    private boolean zoomModifierHeld = false;

    private final Animator scaleAnimation = new Animator();
    private final Animator alphaAnimation = new Animator();
    private final Animator maskAlpha = new Animator();
    private final Animator themeSwitchAnim = new Animator();
    private final SearchBar searchBar = new SearchBar();
    private boolean searchWasActive = false;
    private final AnimClock animClock = new AnimClock();
    private static final BezierEasing CLICKGUI_EASE = BezierEasing.of(0.25, 0.1, 0.25, 1.0);
    private static final int MASK_MAX_ALPHA = 110;

    boolean close = false;
    private boolean configSavedOnClose;

    float moduleListAlpha = 0f;
    float modHeight = 0f;
    ScrollContainer modsContainer = new ScrollContainer();

    public LinkedList<ModuleRenderer> mods = new LinkedList<>();

    static int x = -1;
    static int y = -1;
    static float width = 490f;
    static float height = 310f;
    public static final float leftWidth = UiChrome.SIDEBAR;
    public static String bindLock = "";
    public static Module curModule = null;
    public MainPanel() {
        super();
    }
    private float getCategoryItemSpacing() {
        return UiChrome.NAV_ITEM + 1f;
    }

    private float getNavStartY() {
        return y + 47f;
    }

    private float getSearchBarX() {
        return x + 5.5f;
    }

    private float getSearchBarY() {
        return y + 25f;
    }

    private float getSearchBarWidth() {
        return leftWidth - 11f;
    }

    private float getSearchBarHeight() {
        return 16f;
    }

    private int countModules(Category category) {
        int n = 0;
        for (ModuleRenderer renderer : mods) {
            if (renderer.mod.category == category) {
                n++;
            }
        }
        return n;
    }

    private int countEnabled(Category category) {
        int n = 0;
        for (ModuleRenderer renderer : mods) {
            if (renderer.mod.category == category && renderer.mod.isEnabled()) {
                n++;
            }
        }
        return n;
    }

    /** Screenshot-pipeline hook: filters the list down so one module's settings are on screen. */
    public void searchForShot(String query) {
        searchBar.setQuery(query);
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
        if (name.toLowerCase(Locale.getDefault()).contains(query)
                || desc.toLowerCase(Locale.getDefault()).contains(query)) {
            return true;
        }
        // author:Serendisand
        // reason:全局搜索 - 中文模式下只检索中文，不匹配英文原始名
        if (ClientSettings.language.getValue() == 1) {
            return false;
        }
        return module.name.toLowerCase(Locale.getDefault()).contains(query);
    }

    @Override
    public void render(int mouseX, int mouseY, float partialTicks) {
        updateZoomModifierState();
        //aiChatPanel.render(mouseX, mouseY, scaleFactor);
        // .panel: min(980px, 100vw-40px) x min(620px, 100vh-48px), halved to GUI units.
        width = Math.min(490f, Math.max(300f, guiWidth - 20f));
        height = Math.min(310f, Math.max(220f, guiHeight - 24f));
        x = (int) ((guiWidth - width) / 2f);
        y = (int) ((guiHeight - height) / 2f);
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

        UiChrome.panel(x, y, width, height);
        Rects.fill(x + 1, y + 1, leftWidth - 1, height - 2, ClickGuiTheme.categoryBg());
        Rects.fill(x + leftWidth, y + 1, 0.5f, height - 2, ClickGuiTheme.divider());

        Rects.rounded(x + 7f, y + 7f, 12f, 12f, 4, ClickGuiTheme.accent().getRGB(), false);
        UiChrome.boldCentered(FPSMaster.fontManager.getFont(12), "F", x + 13f, y + 10f, 0xFFFFFFFF);
        UiChrome.boldString(FPSMaster.fontManager.getFont(13), "FPSMaster", x + 23f, y + 7f,
                ClickGuiTheme.textPrimary().getRGB());
        FPSMaster.fontManager.getFont(10).drawString(
                "Edge · " + FPSMaster.CLIENT_VERSION,
                x + 23f,
                y + 15f,
                ClickGuiTheme.textDisabled().getRGB()
        );

        searchBar.draw(this, getSearchBarX(), getSearchBarY(), getSearchBarWidth(), getSearchBarHeight(), mouseX, mouseY);

        boolean searching = isSearchActive();
        String query = searchBar.getQuery();
        if (searching != searchWasActive) {
            modsContainer.resetScroll();
        }
        searchWasActive = searching;

        moduleListAlpha = (float) AnimMath.base(moduleListAlpha, 255.0, 0.1f);

        float scale = (float) scaleAnimation.get();
        float centerX = guiWidth / 2f;
        float centerY = guiHeight / 2f;
        float mainX = x + leftWidth;
        float mainHeadY = y + 8f;
        float listY = y + 22f;
        float listH = height - 28f;
        float containerWidth = width - leftWidth - 12f;

        UiChrome.boldString(
                FPSMaster.fontManager.s16,
                searchingTitle(searching),
                mainX + 9,
                mainHeadY,
                ClickGuiTheme.textPrimary().getRGB()
        );
        String meta = searching
                ? ""
                : String.format(
                        FPSMaster.i18n.get("clickgui.category.meta"),
                        countModules(curType),
                        countEnabled(curType)
                );
        if (!meta.isEmpty()) {
            FPSMaster.fontManager.getFont(12).drawString(meta, mainX + 9 + FPSMaster.fontManager.s16.getStringWidth(searchingTitle(searching)) + 5, mainHeadY + 1.5f, ClickGuiTheme.textSecondary().getRGB());
        }

        float scissorX = centerX + (mainX - centerX) * scale;
        float scissorY = centerY + (listY - centerY) * scale;
        float scissorW = (width - leftWidth) * scale;
        float scissorH = listH * scale;

        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        Scissor.apply(scissorX, scissorY, scissorW, scissorH);
        modHeight = 6f;
        int finalMouseY = mouseY;
        modsContainer.draw(this, mainX + 6, listY, containerWidth, listH, mouseX, mouseY, () -> {
            float modsY = listY + 1f;
            boolean anyMatch = false;
            for (ModuleRenderer m : mods) {
                m.highlight = searching ? query.trim() : null;
                m.searchMode = searching;
                boolean show = searching ? matchesSearch(m.mod, query) : m.mod.category == curType;
                if (show) {
                    anyMatch = true;
                    float moduleY = modsY + modsContainer.getScroll();
                    float row = UiChrome.MODULE_ROW;
                    if (moduleY + row + m.height > listY && moduleY < listY + listH) {
                        m.render(
                                this,
                                mainX + 6,
                                moduleY,
                                containerWidth - 6,
                                row,
                                mouseX,
                                finalMouseY,
                                curModule == m.mod
                        );
                    }
                    modsY += row + 3 + m.height;
                    modHeight += row + 3 + m.height;
                }
            }
            if (searching && !anyMatch) {
                FPSMaster.fontManager.s14.drawCenteredString(
                        FPSMaster.i18n.get("clickgui.search.noresults"),
                        mainX + containerWidth / 2f,
                        listY + listH / 2f,
                        ClickGuiTheme.textDisabled().getRGB()
                );
            }
            modsContainer.setHeight(modHeight);
        });
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glDisable(GL11.GL_SCISSOR_TEST);

        float navX = x + 5.5f;
        float navW = leftWidth - 11f;
        float my = getNavStartY();
        for (CategoryComponent m : categories) {
            m.render(
                    this,
                    navX,
                    my,
                    navW,
                    UiChrome.NAV_ITEM,
                    mouseX,
                    mouseY,
                    curType == m.category,
                    countModules(m.category),
                    dt
            );
            my += getCategoryItemSpacing();
        }

        themeSwitchAnim.update(dt);
        float footerY = y + height - 7 - getCategoryItemSpacing() * 3;
        UiChrome.hairlineH(x + 7, footerY - 4, leftWidth - 14);
        boolean isLightTheme = ClickGuiTheme.isLight();
        if (renderSideNav(navX, footerY, navW, UiChrome.NAV_ITEM, FPSMaster.i18n.get("clickgui.nav.music"), "music", false, mouseX, mouseY)) {
            mc.displayGuiScreen(new top.fpsmaster.ui.screens.music.MusicScreen());
        }
        if (renderSideNav(navX, footerY + getCategoryItemSpacing(), navW, UiChrome.NAV_ITEM, FPSMaster.i18n.get("configprofiles.button"), "folder", false, mouseX, mouseY)) {
            mc.displayGuiScreen(new ConfigProfilesScreen(this));
        }
        String themeLabel = FPSMaster.i18n.get(isLightTheme ? "clickgui.nav.theme.light" : "clickgui.nav.theme.dark");
        if (renderSideNav(navX, footerY + getCategoryItemSpacing() * 2, navW, UiChrome.NAV_ITEM, themeLabel, isLightTheme ? "sun" : "moon", true, mouseX, mouseY)) {
            ClientSettings.theme.setValue(isLightTheme ? 0 : 1);
            themeSwitchAnim.animateTo(isLightTheme ? 0.0 : 1.0, 0.35, Easings.CUBIC_OUT);
        }

        Alpha.set(1f);

        handlePointerPress();
    }

    private String searchingTitle(boolean searching) {
        if (searching) {
            return FPSMaster.i18n.get("clickgui.search.placeholder");
        }
        return FPSMaster.i18n.get("category." + curType.name().toLowerCase(Locale.getDefault()));
    }

    private boolean renderSideNav(float x, float y, float width, float height, String text, String icon, boolean themeIcon, int mouseX, int mouseY) {
        boolean hovered = Hover.is(x, y, width, height, mouseX, mouseY);
        UiChrome.navItem(x, y, width, height, false, hovered);
        int contentColor = hovered ? ClickGuiTheme.textPrimary().getRGB() : ClickGuiTheme.textSecondary().getRGB();
        float iconY = y + (height - 7) / 2f;
        if (themeIcon) {
            float progress = (float) themeSwitchAnim.get();
            if (progress < 0.999f) {
                Icons.draw("moon", x + 6, iconY, 7f, fade(contentColor, 1f - progress));
            }
            if (progress > 0.001f) {
                Icons.draw("sun", x + 6, iconY, 7f, fade(contentColor, progress));
            }
        } else {
            Icons.draw(icon, x + 6, iconY, 7f, contentColor);
        }
        FPSMaster.fontManager.getFont(13).drawString(text, x + 17.5f, y + height / 2f - 3f, contentColor);
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

        if (!bindLock.isEmpty()) {
            for (ModuleRenderer m : mods) {
                m.keyTyped(typedChar, keyCode);
            }
            return;
        }

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
            if (isSearchActive() || m.mod.category == curType) {
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
        float my = getNavStartY();
        float navX = x + 5.5f;
        float navW = leftWidth - 11f;
        for (Category c : Category.values()) {
            if (Hover.is(navX, my, navW, UiChrome.NAV_ITEM, mouseX, mouseY)) {
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
            my += getCategoryItemSpacing();
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





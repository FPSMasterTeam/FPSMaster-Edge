package top.fpsmaster.ui.click;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;
import top.fpsmaster.FPSMaster;
import top.fpsmaster.features.manager.Category;
import top.fpsmaster.features.manager.Module;
import top.fpsmaster.ui.click.component.ScrollContainer;
import top.fpsmaster.ui.click.music.MusicPanel;
import top.fpsmaster.ui.click.modules.ModuleRenderer;
import top.fpsmaster.ui.click.themes.DarkTheme;
import top.fpsmaster.ui.click.themes.LightTheme;
import top.fpsmaster.utils.math.animation.Animation;
import top.fpsmaster.utils.math.animation.AnimationUtils;
import top.fpsmaster.utils.math.animation.ColorAnimation;
import top.fpsmaster.utils.math.animation.Type;
import top.fpsmaster.utils.render.Render2DUtils;
import top.fpsmaster.utils.render.ScaledGuiScreen;

import java.awt.Color;
import java.io.IOException;
import java.util.LinkedList;

public class MainPanel extends ScaledGuiScreen {
    boolean drag = false;
    float dragX = 0f;
    float dragY = 0f;
    Category curType = Category.OPTIMIZE;
    LinkedList<CategoryComponent> categories = new LinkedList<>();
    final float leftWidth = 110f;
    float modsWheel = 0f;
    float wheelTemp = 0f;
    boolean sizeDrag = false;
    float sizeDragX = 0f;
    float sizeDragY = 0f;

    Animation scaleAnimation = new Animation();

    float selection = 0f;

    ColorAnimation sizeDragBorder = new ColorAnimation(255, 255, 255, 0);
    ColorAnimation backgroundColor = new ColorAnimation(FPSMaster.theme.getBackground());
    ColorAnimation modeColor = new ColorAnimation(FPSMaster.theme.getTypeSelectionBackground());
    ColorAnimation logoColor = new ColorAnimation(FPSMaster.theme.getLogo());

    boolean close = false;

    float moduleListAlpha = 0f;
    float modHeight = 0f;
    ScrollContainer modsContainer = new ScrollContainer();

    public LinkedList<ModuleRenderer> mods = new LinkedList<>();

    static int x = -1;
    static int y = -1;
    static float width = 0f;
    static float height = 0f;
    public static String bindLock = "";
    public static Module curModule = null;
    public static String dragLock = "null";

    public MainPanel() {
        super();
    }

    @Override
    public void render(int mouseX, int mouseY, float partialTicks) {
        if (!Mouse.isButtonDown(0)) {
            dragLock = "null";
            drag = false;
            sizeDrag = false;
        }

        if (drag) {
            x = (int) (mouseX - dragX);
            mouseY -= (int) dragY;
            y = mouseY;
        }

        float w = mouseX + sizeDragX - x;
        float h = mouseY + sizeDragY - y;

        if (sizeDrag) {
            width = w;
            height = h;
        }

        width = Math.min(Math.max(400f, width), guiWidth);
        height = Math.min(Math.max(240f, height), guiHeight);

        x = (int) Math.max(0, Math.min(guiWidth - (int) width, x));
        y = (int) Math.max(0, Math.min(guiHeight - (int) height, y));

        if (close) {
            if (scaleAnimation.value <= 0.5) {
                mc.displayGuiScreen(null);
                if (mc.currentScreen == null) {
                    mc.setIngameFocus();
                }
            }
        } else {
            scaleAnimation.start(0.9, 1.0, 0.2f, Type.EASE_OUT_BACK);
        }
        scaleAnimation.update();

        Render2DUtils.drawOptimizedRoundedRect(
                (x - 1),
                (y - 1),
                width + 2,
                height + 2,
                sizeDragBorder.getColor()
        );

        backgroundColor.base(FPSMaster.theme.getBackground());
        Render2DUtils.drawOptimizedRoundedRect(
                x,
                y,
                width,
                height,
                backgroundColor.getColor()
        );

        logoColor.base(FPSMaster.theme.getLogo());
        Render2DUtils.drawImage(
                new ResourceLocation("client/gui/settings/logo.png"),
                x + leftWidth / 2 - 40 - 5,
                y + 15f,
                81.5f,
                64 / 2f,
                logoColor.getColor()
        );

        if (drag || sizeDrag) {
            sizeDragBorder.start(new Color(255, 255, 255, 100), new Color(255, 255, 255), 0.15f, Type.EASE_IN_OUT_QUAD);
        } else {
            sizeDragBorder.start(sizeDragBorder.getColor(), new Color(255, 255, 255, 0), 0.2f, Type.EASE_IN_OUT_QUAD);
        }

        sizeDragBorder.update();

        if (Render2DUtils.isHoveredWithoutScale(
                x + width - 10,
                y + height - 10,
                10f,
                10f,
                mouseX,
                mouseY
        )) {
            Render2DUtils.drawImage(
                    new ResourceLocation("client/gui/settings/drag.png"),
                    x + width - 5,
                    y + height - 5,
                    5f,
                    5f,
                    FPSMaster.theme.getDragHovered()
            );
        } else {
            Render2DUtils.drawImage(
                    new ResourceLocation("client/gui/settings/drag.png"),
                    x + width - 5,
                    y + height - 5,
                    5f,
                    5f,
                    FPSMaster.theme.getDrag()
            );
        }

        float my = (y + 60);
        for (CategoryComponent m : categories) {
            Render2DUtils.drawOptimizedRoundedRect(
                    x + 5,
                    my - 6,
                    leftWidth - 10,
                    20f,
                    m.categorySelectionColor.getColor()
            );
            my += 24f;
        }

        my = (y + 60);
        Render2DUtils.drawOptimizedRoundedRect(
                x + 5,
                selection - 6,
                leftWidth - 10,
                20f,
                FPSMaster.theme.getPrimary()
        );

        for (CategoryComponent m : categories) {
            if (Render2DUtils.isHoveredWithoutScale(x, my - 6, leftWidth - 10, 20f, mouseX, mouseY)) {
                m.categorySelectionColor.base(FPSMaster.theme.getTypeSelectionBackground());
            } else {
                m.categorySelectionColor.base(Render2DUtils.reAlpha(FPSMaster.theme.getTypeSelectionBackground(), 0));
            }

            if (m.category == curType) {
                selection = (sizeDrag || drag)
                        ? my
                        : (float) AnimationUtils.base(selection, my, 0.2);
            }

            m.render(
                    x + 5,
                    my,
                    leftWidth - 10,
                    20f,
                    mouseX,
                    mouseY,
                    curType == m.category
            );
            my += 24f;
        }

        Render2DUtils.drawOptimizedRoundedRect(
                x + 40,
                y + height - 22,
                34f,
                14f,
                10,
                modeColor.getColor().getRGB()
        );

        Render2DUtils.drawImage(
                new ResourceLocation("client/textures/ui/" + FPSMaster.themeSlot + ".png"),
                x + 43,
                y + height - 19,
                8f,
                8f,
                -1
        );

        FPSMaster.fontManager.s16.drawString(
                FPSMaster.i18n.get("theme.title"),
                x + 20,
                y + height - 20,
                FPSMaster.theme.getCategoryText().getRGB()
        );

        FPSMaster.fontManager.s16.drawString(
                FPSMaster.i18n.get("theme." + FPSMaster.themeSlot),
                x + 52,
                y + height - 20,
                FPSMaster.theme.getCategoryTextSelected().getRGB()
        );

        if (Render2DUtils.isHoveredWithoutScale(
                x + 40,
                y + height - 22,
                34f,
                14f,
                mouseX,
                mouseY
        )) {
            modeColor.base(FPSMaster.theme.getPrimary());
        } else {
            modeColor.base(FPSMaster.theme.getTypeSelectionBackground());
        }

        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        Render2DUtils.doGlScissor(
                x, y, width,
                (height - 4),
                scaleFactor
        );

        moduleListAlpha = (float) AnimationUtils.base(moduleListAlpha, 255.0, 0.1f);

        if (curType == Category.Music) {
            MusicPanel.draw(x + leftWidth, y, width - leftWidth, height, mouseX, mouseY, scaleFactor);
        } else {
            modHeight = 20f;
            float containerWidth = width - leftWidth - 2;
            int finalMouseY = mouseY;
            modsContainer.draw(x + leftWidth, y + 10f, containerWidth, height - 20f, mouseX, mouseY, () -> {
                float modsY = y + 10f;

                for (ModuleRenderer m : mods) {
                    if (m.mod.category == curType) {
                        float moduleY = modsY + modsContainer.getScroll();
                        if (moduleY + 40 + m.height > y && moduleY < y + height) {
                            m.render(
                                    x + leftWidth,
                                    moduleY,
                                    containerWidth,
                                    40f,
                                    mouseX,
                                    finalMouseY,
                                    curModule == m.mod
                            );
                        }
                        modsY += 40 + m.height;
                        modHeight += 40 + m.height;
                    }
                }
                modsContainer.setHeight(modHeight);
            });
        }

        GL11.glEnable(GL11.GL_BLEND);
        Render2DUtils.drawRect(
                x + leftWidth, y,
                width - leftWidth, height,
                Render2DUtils.reAlpha(FPSMaster.theme.getBackground(), Render2DUtils.limit(255 - moduleListAlpha))
        );

        GL11.glDisable(GL11.GL_SCISSOR_TEST);
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
    }

    @Override
    public void initGui() {
        super.initGui();
        ScaledResolution sr = new ScaledResolution(mc);
        int scaledWidth = sr.getScaledWidth();
        int scaledHeight = sr.getScaledHeight();
        scaleAnimation.value = 0.9;
        close = false;

        if (width == 0f || height == 0f) {
            width = scaledWidth / 2f;
            height = scaledHeight / 2f;
        }

        if (x == -1 || y == -1) {
            x = (int) ((scaledWidth - width) / 2);
            y = (int) ((scaledHeight - height) / 2);
        }

        categories.clear();
        for (Category c : Category.values()) {
            categories.add(new CategoryComponent(c));
        }

        selection = y + 70f;
    }

    @Override
    public void onGuiClosed() {
        super.onGuiClosed();
        FPSMaster.configManager.saveConfig("default");
    }

    @Override
    public void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == 1) {
            if (scaleAnimation.end != 0.1) {
                close = true;
                scaleAnimation.fstart(scaleAnimation.value, 0.1, 0.2f, Type.EASE_IN_BACK);
            }
            return;
        }

        for (ModuleRenderer m : mods) {
            if (m.mod.category == curType) {
                m.keyTyped(typedChar, keyCode);
            }
        }

        MusicPanel.keyTyped(typedChar, keyCode);
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (!Render2DUtils.isHoveredWithoutScale(x, y, width, height, mouseX, mouseY)) return;

        if (mouseButton == 0 && Render2DUtils.isHoveredWithoutScale(
                x + 40, y + height - 22, 34f, 14f, mouseX, mouseY
        )) {
            if ("dark".equals(FPSMaster.themeSlot)) {
                FPSMaster.themeSlot = "light";
                FPSMaster.theme = new LightTheme();
            } else {
                FPSMaster.themeSlot = "dark";
                FPSMaster.theme = new DarkTheme();
            }
        }

        if (mouseButton == 0 && Render2DUtils.isHoveredWithoutScale(
                x, y, leftWidth, 34f, mouseX, mouseY
        )) {
            drag = true;
            dragX = mouseX - x;
            dragY = mouseY - y;
        }

        if (mouseButton == 0 && Render2DUtils.isHoveredWithoutScale(
                x + width - 20, y + height - 20, 20f, 20f, mouseX, mouseY
        ) && "null".equals(dragLock)) {
            sizeDrag = true;
            dragLock = "sizeDrag";
            sizeDragX = x + width - mouseX;
            sizeDragY = y + height - mouseY;
        }

        float my = y + 60f;
        for (Category c : Category.values()) {
            if (Render2DUtils.isHoveredWithoutScale(x, my - 8, leftWidth, 24f, mouseX, mouseY)) {
                wheelTemp = 0f;
                modsWheel = 0f;
                if (curType != c) {
                    moduleListAlpha = 0f;
                }
                curType = c;
            }
            my += 24f;
        }

        if (curType == Category.Music) {
            MusicPanel.mouseClicked(mouseX, mouseY, mouseButton);
        } else {
            float modsY = y + 10 + modsContainer.getRealScroll();
            for (ModuleRenderer m : mods) {
                if (m.mod.category == curType) {
                    m.mouseClick(
                            x + leftWidth,
                            modsY,
                            width - leftWidth,
                            40f,
                            mouseX,
                            mouseY,
                            mouseButton
                    );
                    modsY += 40 + m.height;
                }
            }
        }
    }
}

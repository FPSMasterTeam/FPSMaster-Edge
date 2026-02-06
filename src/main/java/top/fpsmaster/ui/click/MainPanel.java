package top.fpsmaster.ui.click;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;
import top.fpsmaster.FPSMaster;
import top.fpsmaster.exception.FileException;
import top.fpsmaster.features.manager.Category;
import top.fpsmaster.features.manager.Module;
import top.fpsmaster.ui.click.component.ScrollContainer;
import top.fpsmaster.ui.click.modules.ModuleRenderer;
import top.fpsmaster.utils.math.animation.Animation;
import top.fpsmaster.utils.math.animation.AnimationUtils;
import top.fpsmaster.utils.math.animation.Type;
import top.fpsmaster.utils.render.Render2DUtils;
import top.fpsmaster.utils.render.ScaledGuiScreen;

import java.awt.*;
import java.io.IOException;
import java.util.LinkedList;

public class MainPanel extends ScaledGuiScreen {
    boolean drag = false;
    float dragX = 0f;
    float dragY = 0f;
    Category curType = Category.OPTIMIZE;
    LinkedList<CategoryComponent> categories = new LinkedList<>();
    float modsWheel = 0f;
    float wheelTemp = 0f;

    Animation scaleAnimation = new Animation();

    float selection = 0f;


    float categoryAnimation = 30;

    boolean close = false;

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
    public static String dragLock = "null";

    public MainPanel() {
        super();
    }

    @Override
    public void render(int mouseX, int mouseY, float partialTicks) {
        //aiChatPanel.render(mouseX, mouseY, scaleFactor);

        if (!Mouse.isButtonDown(0)) {
            dragLock = "null";
            drag = false;
        }

        if (drag) {
            mouseY -= (int) dragY;
            x = (int) (mouseX - dragX);
            y = mouseY;
        }

        x = (int) Math.max(0, Math.min(guiWidth - (int) width, x));
        y = (int) Math.max(0, Math.min(guiHeight - (int) height, y));

        if (close) {
            if (scaleAnimation.value <= 0.7) {
                mc.displayGuiScreen(null);
                if (mc.currentScreen == null) {
                    mc.setIngameFocus();
                }
            }
        }
        scaleAnimation.update();

        GlStateManager.translate(guiWidth / 2.0, height / 2.0, 0.0);
        GL11.glScaled(scaleAnimation.value, scaleAnimation.value, 0.0);
        GlStateManager.translate(-guiWidth / 2.0, -height / 2.0, 0.0);


        Render2DUtils.drawImage(new ResourceLocation("client/gui/settings/window/panel.png"),
                x + leftWidth - 8,
                y - 2,
                width - leftWidth + 16,
                height + 12,
                -1
        );

        moduleListAlpha = (float) AnimationUtils.base(moduleListAlpha, 255.0, 0.1f);

        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        Render2DUtils.doGlScissor(
                x, y + 10, width,
                (height - 18),
                scaleFactor
        );
        modHeight = 20f;
        float containerWidth = width - leftWidth - 10;
        int finalMouseY = mouseY;
        modsContainer.draw(x + leftWidth, y + 25f, containerWidth, height - 20f, mouseX, mouseY, () -> {
            float modsY = y + 22f;
            for (ModuleRenderer m : mods) {
                if (m.mod.category == curType) {
                    float moduleY = modsY + modsContainer.getScroll();
                    if (moduleY + 40 + m.height > y && moduleY < y + height) {
                        m.render(
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
            modsContainer.setHeight(modHeight);
        });
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glDisable(GL11.GL_SCISSOR_TEST);


        if (Render2DUtils.isHovered(x, (int) (y + height / 2 - 70), categoryAnimation, 140, mouseX, mouseY)) {
            categoryAnimation = (float) AnimationUtils.base(categoryAnimation, 100f, 0.15f);
        } else {
            categoryAnimation = (float) AnimationUtils.base(categoryAnimation, 30f, 0.15f);
        }

        Render2DUtils.drawRoundedRectImage(
                x + categoryAnimation / 50f,
                y + height / 2 - 74,
                categoryAnimation,
                140,
                20,
                new Color(0, 0, 0, 200)
        );

        Render2DUtils.drawRoundedRectImage(
                x + 5,
                y + height - 25,
                20,
                20,
                20,
                new Color(0, 0, 0, 200)
        );

        Render2DUtils.drawImage(
                new ResourceLocation("client/gui/screen/theme.png"),
                x + 11,
                y + height - 19,
                8,
                8,
                -1);

        float my = y + 60;
        Render2DUtils.drawRoundedRectImage(
                x + 4 + categoryAnimation / 50f,
                selection - 6,
                categoryAnimation - 8,
                22f,
                20,
                new Color(255, 255, 255)
        );


        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        Render2DUtils.doGlScissor(
                x, y, categoryAnimation,
                (height - 4),
                scaleFactor
        );

        for (CategoryComponent m : categories) {
            if (Render2DUtils.isHovered(x, my - 6, leftWidth - 10, 20f, mouseX, mouseY)) {
                m.categorySelectionColor.base(new Color(70, 70, 70));
            } else {
                m.categorySelectionColor.base(Render2DUtils.reAlpha(new Color(70, 70, 70), 0));
            }

            if (m.category == curType) {
                selection = drag
                        ? my
                        : (float) AnimationUtils.base(selection, my, 0.2);
            }

            m.render(
                    x + categoryAnimation / 50f,
                    my,
                    leftWidth - 10,
                    20f,
                    mouseX,
                    mouseY,
                    curType == m.category
            );
            my += 27f;
        }
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        x = (int) ((guiWidth - width) / 2);
        y = (int) ((guiHeight - height) / 2);
    }

    @Override
    public void initGui() {
        super.initGui();
//        aiChatPanel.init();
        scaleAnimation.fstart(0.8, 1.0, 0.2f, Type.EASE_IN_OUT_QUAD);
        close = false;

//        if (width == 0f || height == 0f) {
//            width = scaledWidth / 2f;
//            height = scaledHeight / 2f;
//        }


        categories.clear();
        for (Category c : Category.values()) {
            categories.add(new CategoryComponent(c));
        }

        selection = y + 70f;
    }

    @Override
    public void onResize(Minecraft mcIn, int w, int h) {
        super.onResize(mcIn, w, h);
    }

    @Override
    public void onGuiClosed() {
        super.onGuiClosed();
        try {
            FPSMaster.configManager.saveConfig("default");
        } catch (FileException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void keyTyped(char typedChar, int keyCode) throws IOException {
//        aiChatPanel.keyTyped(typedChar, keyCode);

        if (keyCode == 1) {
            if (scaleAnimation.end != 0.1) {
                close = true;
                scaleAnimation.fstart(scaleAnimation.value, 0.7, 0.1f, Type.EASE_IN_OUT_QUAD);
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
    public void onClick(int mouseX, int mouseY, int mouseButton) {
//        aiChatPanel.click(mouseX, mouseY, mouseButton);
        if (!Render2DUtils.isHovered(x, y, width, height, mouseX, mouseY)) return;

//        if (mouseButton == 0 && Render2DUtils.isHoveredWithoutScale(
//                x + leftWidth, y, width - leftWidth, 20f, mouseX, mouseY
//        )) {
//            drag = true;
//            dragX = mouseX - x;
//            dragY = mouseY - y;
//        }

//        if (mouseButton == 0 && Render2DUtils.isHoveredWithoutScale(
//                x + width - 20, y + height - 20, 20f, 20f, mouseX, mouseY
//        ) && "null".equals(dragLock)) {
//            sizeDrag = true;
//            dragLock = "sizeDrag";
//            sizeDragX = x + width - mouseX;
//            sizeDragY = y + height - mouseY;
//        }
        if (!dragLock.equals("null"))
            return;
        float my = y + 60f;
        for (Category c : Category.values()) {
            if (Render2DUtils.isHovered(x, my - 8, leftWidth, 24f, mouseX, mouseY)) {
                wheelTemp = 0f;
                modsWheel = 0f;
                if (curType != c) {
                    moduleListAlpha = 0f;
                }
                curType = c;
            }
            my += 27f;
        }
        float modsY = y + 22f + modsContainer.getRealScroll();
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
                modsY += 45 + m.height;
            }
        }
    }
}

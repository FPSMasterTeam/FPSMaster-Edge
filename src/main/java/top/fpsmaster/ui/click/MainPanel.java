package top.fpsmaster.ui.click;

import top.fpsmaster.utils.render.state.Alpha;
import top.fpsmaster.utils.render.draw.Images;
import top.fpsmaster.utils.render.draw.Hover;
import top.fpsmaster.utils.render.draw.Colors;
import top.fpsmaster.utils.render.draw.Rects;

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
import top.fpsmaster.utils.math.animation.AnimationUtils;
import top.fpsmaster.utils.math.anim.AnimClock;
import top.fpsmaster.utils.math.anim.Animator;
import top.fpsmaster.utils.math.anim.BezierEasing;
import top.fpsmaster.utils.math.anim.Easings;
import top.fpsmaster.utils.render.gui.ScaledGuiScreen;
import top.fpsmaster.utils.render.gui.Scissor;

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

    private final Animator scaleAnimation = new Animator();
    private final Animator alphaAnimation = new Animator();
    private final Animator maskAlpha = new Animator();
    private final AnimClock animClock = new AnimClock();
    private static final BezierEasing CLICKGUI_EASE = BezierEasing.of(0.25, 0.1, 0.25, 1.0);
    private static final int MASK_MAX_ALPHA = 110;

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
        Rects.fill(0f, 0f, guiWidth, guiHeight, new Color(0, 0, 0, (int) maskAlpha.get()));
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

        moduleListAlpha = (float) AnimationUtils.base(moduleListAlpha, 255.0, 0.1f);

        float scale = (float) scaleAnimation.get();
        float centerX = guiWidth / 2f;
        float centerY = guiHeight / 2f;
        float scissorX = centerX + (x - centerX) * scale;
        float scissorY = centerY + (y + 10 - centerY) * scale;
        float scissorW = width * scale;
        float scissorH = (height - 18) * scale;

        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        Scissor.apply(
                scissorX, scissorY, scissorW,
                scissorH,
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


        float categoryBgHeight = getCategoryBgHeight();
        float categoryBgY = getCategoryBgY();
        float categoryStartY = getCategoryStartY();

        if (Hover.is(x, (int) categoryBgY, categoryAnimation, categoryBgHeight, mouseX, mouseY)) {
            categoryAnimation = (float) AnimationUtils.base(categoryAnimation, 100f, 0.15f);
        } else {
            categoryAnimation = (float) AnimationUtils.base(categoryAnimation, 30f, 0.15f);
        }

        Rects.roundedImage(
                x + categoryAnimation / 50f,
                categoryBgY,
                categoryAnimation,
                categoryBgHeight,
                20,
                new Color(0, 0, 0, 200)
        );

        float my = categoryStartY;
        Rects.roundedImage(
                x + 4 + categoryAnimation / 50f,
                selection - 6,
                categoryAnimation - 8,
                22f,
                20,
                new Color(255, 255, 255)
        );


        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        float categoryScissorX = centerX + (x - centerX) * scale;
        float categoryScissorY = centerY + (categoryBgY - centerY) * scale;
        float categoryScissorW = categoryAnimation * scale;
        float categoryScissorH = categoryBgHeight * scale;
        Scissor.apply(
                categoryScissorX, categoryScissorY, categoryScissorW,
                categoryScissorH,
                scaleFactor
        );

        for (CategoryComponent m : categories) {
            if (Hover.is(x, my - 6, leftWidth - 10, 20f, mouseX, mouseY)) {
                m.categorySelectionColor.animateTo(new Color(70, 70, 70), 0.15f, Easings.QUAD_OUT);
            } else {
                m.categorySelectionColor.animateTo(Colors.alpha(new Color(70, 70, 70), 0), 0.15f, Easings.QUAD_OUT);
            }
            m.categorySelectionColor.update(dt);

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
                    curType == m.category,
                    dt
            );
            my += 27f;
        }
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
        Alpha.set(1f);
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
        animClock.reset();
        scaleAnimation.start(0.8, 1.0, 0.2f, CLICKGUI_EASE);
        alphaAnimation.start(0.0, 255.0, 0.2f, CLICKGUI_EASE);
        maskAlpha.start(0.0, MASK_MAX_ALPHA, 0.2f, CLICKGUI_EASE);
        close = false;

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
            if (scaleAnimation.isRunning() || scaleAnimation.get() != 0.7) {
                close = true;
                scaleAnimation.animateTo(0.7, 0.1f, CLICKGUI_EASE);
                alphaAnimation.animateTo(0.0, 0.1f, CLICKGUI_EASE);
                maskAlpha.animateTo(0.0, 0.1f, CLICKGUI_EASE);
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
        if (!Hover.is(x, y, width, height, mouseX, mouseY)) return;

//        if (mouseButton == 0 && Hover.isWithoutScale(
//                x + leftWidth, y, width - leftWidth, 20f, mouseX, mouseY
//        )) {
//            drag = true;
//            dragX = mouseX - x;
//            dragY = mouseY - y;
//        }

//        if (mouseButton == 0 && Hover.isWithoutScale(
//                x + width - 20, y + height - 20, 20f, 20f, mouseX, mouseY
//        ) && "null".equals(dragLock)) {
//            sizeDrag = true;
//            dragLock = "sizeDrag";
//            sizeDragX = x + width - mouseX;
//            sizeDragY = y + height - mouseY;
//        }
        if (!dragLock.equals("null"))
            return;
        float my = getCategoryStartY();
        for (Category c : Category.values()) {
            if (Hover.is(x, my - 8, leftWidth, 24f, mouseX, mouseY)) {
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





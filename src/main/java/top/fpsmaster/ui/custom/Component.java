package top.fpsmaster.ui.custom;

import top.fpsmaster.utils.render.draw.Hover;
import top.fpsmaster.utils.render.draw.Rects;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.ScaledResolution;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;
import top.fpsmaster.FPSMaster;
import top.fpsmaster.features.impl.InterfaceModule;
import top.fpsmaster.features.impl.interfaces.ClientSettings;
import top.fpsmaster.features.manager.Category;
import top.fpsmaster.features.manager.Module;
import top.fpsmaster.font.impl.UFontRenderer;
import top.fpsmaster.modules.logger.ClientLogger;
import top.fpsmaster.ui.click.MainPanel;
import top.fpsmaster.utils.core.Utility;
import top.fpsmaster.utils.math.anim.AnimMath;

import java.awt.*;

public class Component {
    private static final Color STENCIL_MASK_COLOR = new Color(255, 255, 255, 255);

    private static final float MIN_SCALE = 0.5f;
    private static final float MAX_SCALE = 4.5f;
    private static final float HANDLE_SIZE = 6f;
    /** Cursor travel, in screen pixels, needed to change the scale by 1x. */
    private static final float RESIZE_SENSITIVITY = 1f / 60f;

    private float dragX = 0f;

    private float dragY = 0f;

    private float resizeStartScale = 1f;

    private int resizeStartMouseX;

    private int resizeStartMouseY;

    public InterfaceModule mod;

    public float x = 0f;

    public float y = 0f;

    public float width = 0f;

    public float height = 0f;

    public float scale = 1f;

    public boolean allowScale = false;

    /** Consecutive render failures; reset on any successful frame. See ComponentsManager. */
    public int renderFailures = 0;

    public Position position = Position.LT;

    @SuppressWarnings("unchecked")
    public Component(Class<?> clazz) {
        Module module;
        try {
            module = FPSMaster.moduleManager.getModule((Class<? extends Module>) clazz);
        } catch (IllegalStateException exception) {
            ClientLogger.warn("Missing interface module for component: " + clazz.getName());
            this.mod = new InterfaceModule(clazz.getSimpleName(), Category.Interface);
            this.mod.set(false);
            return;
        }
        if (module instanceof InterfaceModule) {
            this.mod = (InterfaceModule) module;
            return;
        }

        ClientLogger.warn("Missing interface module for component: " + clazz.getName());
        this.mod = new InterfaceModule(clazz.getSimpleName(), Category.Interface);
        this.mod.set(false);
    }

    public void draw(float x, float y) {
    }

    /**
     * Computes {@link #width}/{@link #height} for this frame, before anything reads them.
     *
     * <p>Historically every component assigned its size inside {@code draw()}, but anchoring
     * ({@link #getRealPosition}), hover testing, drag clamping and the blur mask all run <em>before</em>
     * {@code draw()} — so they used the previous frame's size. A component whose size changes each
     * frame (FPS going {@code 99fps} → {@code 100fps}, a potion expiring) visibly jitters, and on its
     * very first frame {@code width} is still 0.
     *
     * <p>Default implementation does nothing, so components that have not been migrated keep their old
     * behaviour. Override it, move the sizing math here, and {@code draw()} becomes pure rendering.
     */
    public void measure() {
    }

    /** Receives the rectangles that make up a component's background. */
    public interface ShapeSink {
        /**
         * @param x absolute left edge, already scaled by the caller (same convention as
         *          {@link #drawRect}: positions are the caller's job, sizes are the base class's)
         * @param y absolute top edge
         * @param width  logical width; the base class multiplies by {@link #scale}
         * @param height logical height
         */
        void rect(float x, float y, float width, float height);
    }

    /**
     * Declares the geometry of this component's background so the blur mask can reproduce it exactly
     * instead of guessing.
     *
     * <p>The mask used to assume every component draws one rectangle at {@code (rX - 2, rY)} sized
     * {@code width × height}. That holds for about two thirds of them; the rest either use a different
     * origin (Keystrokes, PotionDisplay) or draw several disjoint boxes (ArmorDisplay), so the blur
     * leaked into the gaps or sat a couple of pixels off.
     *
     * <p>Whatever a component declares here must match what it actually paints in {@code draw()}.
     */
    public void backgroundShape(ShapeSink sink, float originX, float originY) {
        sink.rect(originX - 2f, originY, width, height);
    }

    public float alpha = 0f;

    public boolean shouldDisplay() {
        return mod.isEnabled();
    }

    public float[] getRealPosition() {
        return getRealPosition(new ScaledResolution(Minecraft.getMinecraft()));
    }

    public float[] getRealPosition(ScaledResolution sr) {
        float rX = 0f;
        float rY = 0f;
        x = Math.max(0f, Math.min(1f, x));
        y = Math.max(0f, Math.min(1f, y));

        float scaleFactor = (float) ClientSettings.getUiScale();
        if (scaleFactor <= 0) {
            scaleFactor = 1.0f;
        }
        float guiWidth = sr.getScaledWidth() / 2f * scaleFactor;
        float guiHeight = sr.getScaledHeight() / 2f * scaleFactor;

        // Anchors offset by the component's *rendered* size. width/height are logical units; everything
        // that touches the drawn box (drawRect, hover, drag clamping, blur mask) multiplies by scale,
        // so these must too — otherwise a scaled-up right-anchored component overflows the screen edge
        // by width * (scale - 1).
        float scaledWidth = width * scale;
        float scaledHeight = height * scale;

        switch (position) {
            case LT:
                rX = x * guiWidth / 2f;
                rY = y * guiHeight / 2f;
                break;
            case RT:
                rX = guiWidth - (x * guiWidth / 2f + scaledWidth);
                rY = y * guiHeight / 2f;
                break;
            case LB:
                rX = x * guiWidth / 2f;
                rY = guiHeight - (y * guiHeight / 2f + scaledHeight);
                break;
            case RB:
                rX = guiWidth - (x * guiWidth / 2f + scaledWidth);
                rY = guiHeight - (y * guiHeight / 2f + scaledHeight);
                break;
            case CT:
                rX = guiWidth / 2f - scaledWidth / 2f;
                rY = y * guiHeight / 2f;
                break;
        }
        return new float[]{rX, rY};
    }

    public void drawBlurMask(ScaledResolution sr) {
        if (!hasBackground() || width <= 0f || height <= 0f) {
            return;
        }
        float[] pos = getRealPosition(sr);
        boolean round = mod.rounded.getValue();
        int radius = mod.roundRadius.getValue().intValue();
        // Same geometry the component paints, so the mask can never drift from the background.
        backgroundShape((rectX, rectY, rectWidth, rectHeight) -> {
            float scaledWidth = rectWidth * scale;
            float scaledHeight = rectHeight * scale;
            if (scaledWidth <= 0f || scaledHeight <= 0f) {
                return;
            }
            if (round) {
                Rects.roundedImage(Math.round(rectX), Math.round(rectY), Math.round(scaledWidth), Math.round(scaledHeight), radius, STENCIL_MASK_COLOR);
            } else {
                Rects.fill(rectX, rectY, scaledWidth, scaledHeight, STENCIL_MASK_COLOR);
            }
        }, pos[0], pos[1]);
    }

    public void display(ScaledResolution sr, int mouseX, int mouseY) {
        float[] pos = getRealPosition(sr);
        float rX = pos[0];
        float rY = pos[1];
        if ((Utility.mc.currentScreen instanceof GuiChat || Utility.mc.currentScreen instanceof MainPanel)) {
            float scaledWidth = width * scale;
            float scaledHeight = height * scale;
            ComponentsManager manager = FPSMaster.componentsManager;
            boolean drag = manager.dragTarget == this;
            boolean hovered = Hover.is(rX, rY, scaledWidth, scaledHeight, mouseX, mouseY);
            boolean overHandle = allowScale && isOverResizeHandle(rX, rY, scaledWidth, scaledHeight, mouseX, mouseY);

            alpha = (float) ((hovered || overHandle || drag) ?
                    AnimMath.base(alpha, 1f, 0.2f) : AnimMath.base(alpha, 0.0f, 0.2f));

            Rects.fill(rX - 2, rY - 2, scaledWidth + 4, scaledHeight + 4, new Color(0, 0, 0, (int) (alpha * 80)));
            draw(rX, rY);
            GL11.glColor4f(1, 1, 1, 1);

            if (alpha > 0.01f && hasResizeHandle(scaledWidth, scaledHeight)) {
                drawResizeHandle(rX, rY, scaledWidth, scaledHeight,
                        overHandle || (drag && manager.dragMode == ComponentsManager.DragMode.RESIZE));
            }

            if (hovered || overHandle || drag) {
                if (Utility.mc.currentScreen instanceof MainPanel && ((MainPanel) Utility.mc.currentScreen).hasPointerCapture())
                    return;
                if (allowScale && ClientSettings.isZoomBindDown()) {
                    int dWheel = Mouse.getDWheel();
                    if (dWheel > 0) scaleUp();
                    else if (dWheel < 0) scaleDown();
                }
                FPSMaster.fontManager.s14.drawString(FPSMaster.i18n.get(mod.name.toLowerCase()) + " " + Math.round(scale * 10) / 10f + "x", rX, rY - 10, new Color(255, 255, 255, (int) (alpha * 255)).getRGB());

                if (!Mouse.isButtonDown(0)) return;

                if (manager.dragTarget == null) {
                    manager.dragTarget = this;
                    if (overHandle) {
                        manager.dragMode = ComponentsManager.DragMode.RESIZE;
                        resizeStartScale = scale;
                        resizeStartMouseX = mouseX;
                        resizeStartMouseY = mouseY;
                    } else {
                        manager.dragMode = ComponentsManager.DragMode.MOVE;
                        dragX = mouseX - rX;
                        dragY = mouseY - rY;
                    }
                }

                if (manager.dragTarget == this) {
                    if (manager.dragMode == ComponentsManager.DragMode.RESIZE) {
                        resizeTo(mouseX, mouseY);
                    } else {
                        move(mouseX, mouseY);
                    }
                }
            }
        } else {
            draw(rX, rY);
        }
    }

    public void scaleUp() {
        if (scale < MAX_SCALE) scale = (int) (scale * 10 + 1) / 10f;
    }

    public void scaleDown() {
        if (scale > MIN_SCALE) scale = (int) (scale * 10 - 1) / 10f;
    }

    /**
     * The handle is suppressed on components smaller than this, where its grab area would swallow most
     * of the body and leave no room to start a move.
     */
    private boolean hasResizeHandle(float scaledWidth, float scaledHeight) {
        return allowScale && scaledWidth >= HANDLE_SIZE * 3f && scaledHeight >= HANDLE_SIZE * 3f;
    }

    /** Bottom-right corner box the user grabs to resize. Sized in screen pixels, deliberately not
     *  scaled — otherwise a component shrunk to 0.5x would have a handle too small to hit. */
    private boolean isOverResizeHandle(float rX, float rY, float scaledWidth, float scaledHeight, int mouseX, int mouseY) {
        if (!hasResizeHandle(scaledWidth, scaledHeight)) {
            return false;
        }
        return Hover.is(rX + scaledWidth - HANDLE_SIZE, rY + scaledHeight - HANDLE_SIZE,
                HANDLE_SIZE * 2f, HANDLE_SIZE * 2f, mouseX, mouseY);
    }

    private void drawResizeHandle(float rX, float rY, float scaledWidth, float scaledHeight, boolean active) {
        float handleX = rX + scaledWidth - HANDLE_SIZE;
        float handleY = rY + scaledHeight - HANDLE_SIZE;
        int shade = (int) (alpha * (active ? 255 : 160));
        Rects.rounded(handleX, handleY, HANDLE_SIZE, HANDLE_SIZE, 2, new Color(255, 255, 255, shade).getRGB());
        Rects.rounded(handleX + 1f, handleY + 1f, HANDLE_SIZE - 2f, HANDLE_SIZE - 2f, 1,
                new Color(0, 0, 0, (int) (alpha * 140)).getRGB());
    }

    /**
     * Resizes from how far the cursor has travelled since the grab, not from its distance to the
     * component's origin: for right/bottom-anchored components the origin itself moves as the scale
     * changes, which would feed back into the next frame's delta and run away.
     */
    private void resizeTo(int mouseX, int mouseY) {
        float delta = ((mouseX - resizeStartMouseX) + (mouseY - resizeStartMouseY)) / 2f;
        float target = resizeStartScale + delta * RESIZE_SENSITIVITY;
        // Quantise to the same 0.1 steps the scroll wheel uses so both paths agree.
        target = Math.round(target * 10f) / 10f;
        scale = Math.max(MIN_SCALE, Math.min(MAX_SCALE, target));
    }

    private void move(int x, int y) {
        ScaledResolution sr = new ScaledResolution(Utility.mc);
        float scaleFactor = (float) ClientSettings.getUiScale();
        if (scaleFactor <= 0) {
            scaleFactor = 1.0f;
        }
        float guiWidth = sr.getScaledWidth() / 2f * scaleFactor;
        float guiHeight = sr.getScaledHeight() / 2f * scaleFactor;
        float changeX = 0f;
        float changeY = 0f;
        if (x > guiWidth / 2f) {
            if (y >= guiHeight / 2f)
                position = Position.RB;
            else if (y < guiHeight / 2f)
                position = Position.RT;
        } else {
            if (y >= guiHeight / 2f)
                position = Position.LB;
            else if (y < guiHeight / 2f)
                position = Position.LT;
        }

        switch (position) {
            case LT: {
                changeX = x - dragX;
                changeY = y - dragY;
                break;
            }
            case RT: {
                changeX = guiWidth - x - width + dragX;
                changeY = y - dragY;
                break;
            }

            case LB: {
                changeX = x - dragX;
                changeY = guiHeight - y - height + dragY;
                break;
            }

            case RB: {
                changeX = guiWidth - x - width + dragX;
                changeY = guiHeight - y - height + dragY;
                break;
            }

            case CT:
                position = Position.CT;
                break;
        }

        if (changeX < 0f || changeX + width * scale > guiWidth) {
            changeX = Math.min(Math.max(changeX, 0f), guiWidth - width * scale);
        }
        if (changeY < 0f || changeY + height * scale > guiHeight) {
            changeY = Math.min(Math.max(changeY, 0f), guiHeight - height * scale);
        }

        this.x = changeX / guiWidth * 2f;
        this.y = changeY / guiHeight * 2f;
    }

    /**
     * A module only has a background if its traits say so. Checking {@code bg} alone is not enough:
     * the field exists on every InterfaceModule and defaults to {@code true}, so a text-only module
     * such as Sprint would otherwise have a blur mask stamped for a panel it never draws.
     */
    public boolean hasBackground() {
        return mod.has(InterfaceModule.Trait.BACKGROUND) && mod.bg.getValue();
    }

    public void drawRect(float x, float y, float width, float height, Color color) {
        float scaledWidth = width * scale;
        float scaledHeight = height * scale;

        if (hasBackground()) {
            if (mod.rounded.getValue()) {
                Rects.roundedImage(Math.round(x), Math.round(y), Math.round(scaledWidth), Math.round(scaledHeight), mod.roundRadius.getValue().intValue(), color);
            } else {
                Rects.fill(x, y, scaledWidth, scaledHeight, color);
            }
        }
    }

    public void drawString(int fontSize, String text, float x, float y, int color) {
        drawString(fontSize, false, text, x, y, color);
    }

    public void drawString(int fontSize, boolean bold, String text, float x, float y, int color) {
        double scaled = (int) (scale * 100) / 100.0;
        fontSize = (int) (fontSize * scale);
        UFontRenderer font = FPSMaster.fontManager.getFont(fontSize);
        if (mod.betterFont.getValue()) {
            if (mod.fontShadow.getValue()) font.drawStringWithShadow(text, x, y, color);
            else font.drawString(text, x, y, color);
        } else {
            GL11.glPushMatrix();
            GL11.glTranslated(x, y, 0.0);
            GL11.glScaled(scaled, scaled, 1.0);
                if (mod.fontShadow.getValue()) {
                    Minecraft.getMinecraft().fontRendererObj.drawStringWithShadow(text, 0, 0, color);
                } else {
                    GL11.glColor4f(1, 1, 1, 1);
                    Minecraft.getMinecraft().fontRendererObj.drawString(text, 0, 0, color);
                }
            GL11.glPopMatrix();
        }
    }

    public float getStringWidth(int fontSize, String name) {
        UFontRenderer font = FPSMaster.fontManager.getFont(fontSize);
        return mod.betterFont.getValue() ? font.getStringWidth(name) : (Minecraft.getMinecraft().fontRendererObj.getStringWidth(name));
    }

    public float getStringHeight(int fontSize) {
        UFontRenderer font = FPSMaster.fontManager.getFont(fontSize);
        return mod.betterFont.getValue() ? font.getHeight() : (Minecraft.getMinecraft().fontRendererObj.FONT_HEIGHT);
    }
}





package top.fpsmaster.utils.render.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;
import top.fpsmaster.features.impl.interfaces.ClientSettings;

import java.io.IOException;

public class ScaledGuiScreen extends GuiScreen {
    public static final class PointerEvent {
        public final int x;
        public final int y;
        public final int button;

        public PointerEvent(int x, int y, int button) {
            this.x = x;
            this.y = y;
            this.button = button;
        }
    }

    public float scaleFactor = 1.0f;
    public float guiWidth;
    public float guiHeight;

    private float renderScale = 1.0f;
    private float vanillaScaleFactor = 1.0f;
    private final GuiInputState inputState = new GuiInputState();
    private final GuiDragState dragState = new GuiDragState();

    private static ScaledGuiScreen activeScreen;

    public static ScaledGuiScreen getActiveScreen() {
        return activeScreen;
    }

    public static boolean isScaledGuiActive() {
        return activeScreen != null;
    }

    public static float getActiveRenderScale() {
        return activeScreen == null ? 1.0f : activeScreen.renderScale;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        refreshScaleAndMetrics();
        inputState.updateMousePosition(getLogicalMouseX(mouseX), getLogicalMouseY(mouseY));
        if (dragState.isDragging() && !inputState.isButtonDown(dragState.getButton())) {
            dragState.clear();
        }

        UiScale.begin(
                scaleFactor,
                vanillaScaleFactor,
                guiWidth,
                guiHeight,
                mc.displayWidth,
                mc.displayHeight
        );
        GL11.glPushMatrix();
        try {
            activeScreen = this;
            GL11.glScalef(renderScale, renderScale, 1f);
            render(inputState.getMouseX(), inputState.getMouseY(), partialTicks);
            // Reported after render() so subclasses that compute their layout there (MainPanel centres
            // itself every frame) have already done so. The HUD editor reads it on the next frame,
            // which is fine — it runs ahead of this one within a frame anyway.
            publishOcclusion();
        } finally {
            inputState.finishFrame();
            activeScreen = null;
            GL11.glPopMatrix();
            UiScale.end();
        }
    }

    /**
     * The rectangle this screen paints over, in logical coordinates, or {@code null} for "the whole
     * screen". Override in screens that only cover part of the display so the HUD stays interactive
     * around them.
     *
     * @return {@code {x, y, width, height}}
     */
    protected float[] getOccludingBounds() {
        return null;
    }

    private void publishOcclusion() {
        float[] bounds = getOccludingBounds();
        if (bounds == null) {
            GuiOcclusion.set(0f, 0f, mc.displayWidth, mc.displayHeight);
            return;
        }
        GuiOcclusion.set(bounds[0] * scaleFactor, bounds[1] * scaleFactor,
                bounds[2] * scaleFactor, bounds[3] * scaleFactor);
    }

    @Override
    public void onResize(Minecraft mcIn, int w, int h) {
        super.onResize(mcIn, w, h);
        refreshScaleAndMetrics();
    }

    @Override
    public void initGui() {
        refreshScaleAndMetrics();
        inputState.reset();
        dragState.clear();
        super.initGui();
    }

    @Override
    public void onGuiClosed() {
        // Nothing is covering the HUD any more; leaving this set would freeze the editor under a
        // rectangle that is no longer painted.
        GuiOcclusion.clear();
        super.onGuiClosed();
    }

    @Override
    public void handleMouseInput() throws IOException {
        refreshScaleAndMetrics();

        int logicalMouseX = getLogicalMouseX();
        int logicalMouseY = getLogicalMouseY();
        inputState.updateMousePosition(logicalMouseX, logicalMouseY);

        int eventButton = Mouse.getEventButton();
        if (eventButton != -1) {
            if (Mouse.getEventButtonState()) {
                inputState.pressButton(eventButton, logicalMouseX, logicalMouseY);
                mousePressed(logicalMouseX, logicalMouseY, eventButton);
            } else {
                inputState.releaseButton(eventButton);
                mouseReleased(logicalMouseX, logicalMouseY, eventButton);
                if (dragState.isDragging() && dragState.getButton() == eventButton) {
                    dragState.clear();
                }
            }
        }

        int wheelDelta = Mouse.getEventDWheel();
        if (wheelDelta != 0) {
            inputState.addWheelDelta(wheelDelta);
            mouseScrolled(logicalMouseX, logicalMouseY, wheelDelta);
        }
    }

    protected void mousePressed(int mouseX, int mouseY, int mouseButton) throws IOException {
    }

    protected void mouseReleased(int mouseX, int mouseY, int mouseButton) {
    }

    protected void mouseScrolled(int mouseX, int mouseY, int wheelDelta) {
    }

    private void refreshScaleAndMetrics() {
        updateBaseMetrics();
    }

    private void updateBaseMetrics() {
        Minecraft mc = Minecraft.getMinecraft();
        ScaledResolution scaledResolution = new ScaledResolution(mc);
        vanillaScaleFactor = Math.max(1, scaledResolution.getScaleFactor());
        scaleFactor = (float) ClientSettings.getUiScale();
        renderScale = ClientSettings.getUiRenderScale();
        if (renderScale <= 0f) {
            renderScale = 1.0f;
        }
        if (scaleFactor <= 0f) {
            scaleFactor = 1.0f;
        }
        guiWidth = mc.displayWidth / scaleFactor;
        guiHeight = mc.displayHeight / scaleFactor;
    }

    public void render(int mouseX, int mouseY, float partialTicks) {
    }

    public boolean isMouseDown(int button) {
        return inputState.isButtonDown(button);
    }

    public int getMouseX() {
        return inputState.getMouseX();
    }

    public int getMouseY() {
        return inputState.getMouseY();
    }

    public int consumeWheelDelta() {
        return inputState.consumeWheelDelta();
    }

    public int consumeWheelDelta(float x, float y, float width, float height) {
        if (!top.fpsmaster.utils.render.draw.Hover.is(x, y, width, height, getMouseX(), getMouseY())) {
            return 0;
        }
        return consumeWheelDelta();
    }

    public int getWheelDelta() {
        return inputState.getWheelDelta();
    }

    public boolean hasAnyClickThisFrame() {
        return inputState.hasPressEvent();
    }

    public int getLatestClickX() {
        GuiInputState.MouseButtonEvent latestPress = inputState.getLatestPress();
        return latestPress == null ? 0 : latestPress.getX();
    }

    public int getLatestClickY() {
        GuiInputState.MouseButtonEvent latestPress = inputState.getLatestPress();
        return latestPress == null ? 0 : latestPress.getY();
    }

    public boolean beginDrag(Object owner, float x, float y, float width, float height) {
        return beginDrag(owner, 0, x, y, width, height);
    }

    public boolean beginDrag(Object owner, int button, float x, float y, float width, float height) {
        if (dragState.isDragging(owner)) {
            return true;
        }
        PointerEvent click = consumeClickInBounds(x, y, width, height, button);
        if (click == null) {
            return false;
        }
        return dragState.acquire(owner, button);
    }

    public boolean isDragging(Object owner) {
        return dragState.isDragging(owner) && isMouseDown(dragState.getButton());
    }

    public boolean hasActiveDrag() {
        return dragState.isDragging() && isMouseDown(dragState.getButton());
    }

    public boolean hasPointerCapture() {
        return hasActiveDrag();
    }

    public void releaseDrag(Object owner) {
        dragState.release(owner);
    }

    public boolean beginPointerCapture(Object owner, int button, float x, float y, float width, float height) {
        return beginDrag(owner, button, x, y, width, height);
    }

    public boolean isPointerCapturedBy(Object owner, int button) {
        return dragState.isDragging(owner) && dragState.getButton() == button && isMouseDown(button);
    }

    public void releasePointerCapture(Object owner) {
        releaseDrag(owner);
    }

    public PointerEvent peekAnyPress() {
        GuiInputState.MouseButtonEvent latestPress = inputState.getLatestPress();
        if (latestPress == null) {
            return null;
        }
        return new PointerEvent(latestPress.getX(), latestPress.getY(), latestPress.getButton());
    }

    public PointerEvent consumePressInBounds(float x, float y, float width, float height) {
        return consumePressInBounds(x, y, width, height, -1);
    }

    public PointerEvent consumePressInBounds(float x, float y, float width, float height, int button) {
        GuiInputState.MouseButtonEvent event = inputState.consumePressInBounds(x, y, width, height, button);
        if (event == null) {
            return null;
        }
        return new PointerEvent(event.getX(), event.getY(), event.getButton());
    }

    public PointerEvent consumeClickInBounds(float x, float y, float width, float height) {
        return consumePressInBounds(x, y, width, height);
    }

    public PointerEvent consumeClickInBounds(float x, float y, float width, float height, int button) {
        return consumePressInBounds(x, y, width, height, button);
    }

    /**
     * Claims a click only if this widget was the topmost one under the cursor.
     *
     * <p>{@link #consumePressInBounds} alone is first-come-first-served, and widgets call it right
     * after painting themselves — so the <em>first painted</em>, i.e. bottom-most, widget wins a
     * contested click. That is the inverse of what z-order requires. This variant adds the missing
     * ordering: {@code markHovered} records the topmost widget as the frame is walked, and the
     * previous frame's answer gates the click here.
     *
     * <p>Widgets that still call {@code consumePressInBounds} keep the old behaviour, so this can be
     * adopted one widget at a time.
     *
     * @param id stable identity for this widget across frames — the setting/module object itself works
     *           well; avoid values that are rebuilt every frame
     */
    public PointerEvent consumePressAsHovered(Object id, float x, float y, float width, float height, int button) {
        inputState.markHovered(id, x, y, width, height);
        if (!inputState.wasHovered(id)) {
            return null;
        }
        return consumePressInBounds(x, y, width, height, button);
    }

    public PointerEvent consumePressAsHovered(Object id, float x, float y, float width, float height) {
        return consumePressAsHovered(id, x, y, width, height, -1);
    }

    /**
     * Claims a press that landed outside the given rectangle — "click away to dismiss" for popups.
     * The press is consumed so it does not also reach whatever the popup was covering.
     */
    public PointerEvent consumePressOutside(float x, float y, float width, float height) {
        GuiInputState.MouseButtonEvent event = inputState.consumePressOutside(x, y, width, height);
        return event == null ? null : new PointerEvent(event.getX(), event.getY(), event.getButton());
    }

    /** Hover feedback should follow the same z-order rule as clicks, or highlights double up. */
    public boolean isHovered(Object id, float x, float y, float width, float height) {
        inputState.markHovered(id, x, y, width, height);
        return inputState.wasHovered(id);
    }

    private int getLogicalMouseX() {
        return clampLogicalMouseX((int) (Mouse.getX() / scaleFactor));
    }

    private int getLogicalMouseY() {
        return clampLogicalMouseY((int) ((Minecraft.getMinecraft().displayHeight - Mouse.getY() - 1) / scaleFactor));
    }

    private int getLogicalMouseX(int projectedMouseX) {
        return clampLogicalMouseX((int) (projectedMouseX / Math.max(renderScale, 1.0E-6f)));
    }

    private int getLogicalMouseY(int projectedMouseY) {
        return clampLogicalMouseY((int) (projectedMouseY / Math.max(renderScale, 1.0E-6f)));
    }

    private int clampLogicalMouseX(int mouseX) {
        return Math.max(0, Math.min(Math.max(0, Math.round(guiWidth)), mouseX));
    }

    private int clampLogicalMouseY(int mouseY) {
        return Math.max(0, Math.min(Math.max(0, Math.round(guiHeight)), mouseY));
    }
}

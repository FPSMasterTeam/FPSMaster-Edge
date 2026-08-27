package top.fpsmaster.ui.kit;

import top.fpsmaster.utils.render.gui.ScaledGuiScreen;
import top.fpsmaster.utils.render.gui.Scissor;
import top.fpsmaster.prism.input.FrameInput;
import top.fpsmaster.prism.input.Input;
import top.fpsmaster.prism.input.PointerEvent;

final class EdgeInput implements Input {
    private final ScaledGuiScreen screen;
    private final FrameInput fallback;
    private final EdgeCanvas canvas;

    EdgeInput(ScaledGuiScreen screen, FrameInput fallback, EdgeCanvas canvas) {
        this.screen = screen;
        this.fallback = fallback;
        this.canvas = canvas;
    }

    public int mouseX() {
        return screen != null ? screen.getMouseX() : fallback.mouseX();
    }

    public int mouseY() {
        return screen != null ? screen.getMouseY() : fallback.mouseY();
    }

    public boolean isButtonDown(int button) {
        return screen != null ? screen.isMouseDown(button) : fallback.isButtonDown(button);
    }

    public PointerEvent consumePressInBounds(float x, float y, float w, float h, int button) {
        float[] hit = clippedHit(x, y, w, h);
        if (!Scissor.hasArea(hit)) {
            return null;
        }
        if (screen == null) {
            return fallback.consumePressInBounds(hit[0], hit[1], hit[2], hit[3], button);
        }
        ScaledGuiScreen.PointerEvent event = screen.consumePressInBounds(hit[0], hit[1], hit[2], hit[3], button);
        return event == null ? null : new PointerEvent(event.x, event.y, event.button);
    }

    public PointerEvent consumePressOutside(float x, float y, float w, float h) {
        if (screen == null) {
            return fallback.consumePressOutside(x, y, w, h);
        }
        ScaledGuiScreen.PointerEvent event = screen.consumePressOutside(x, y, w, h);
        return event == null ? null : new PointerEvent(event.x, event.y, event.button);
    }

    public boolean hasPressOutside(float x, float y, float w, float h) {
        if (screen == null) {
            return fallback.hasPressOutside(x, y, w, h);
        }
        ScaledGuiScreen.PointerEvent event = screen.peekAnyPress();
        if (event == null) {
            return false;
        }
        return event.x < x || event.x > x + w || event.y < y || event.y > y + h;
    }

    public int consumeWheelDelta(float x, float y, float w, float h) {
        float[] hit = clippedHit(x, y, w, h);
        if (!Scissor.hasArea(hit)) {
            return 0;
        }
        return screen != null
                ? screen.consumeWheelDelta(hit[0], hit[1], hit[2], hit[3])
                : fallback.consumeWheelDelta(hit[0], hit[1], hit[2], hit[3]);
    }

    public void markHovered(Object id, float x, float y, float w, float h) {
        float[] hit = clippedHit(x, y, w, h);
        if (!Scissor.hasArea(hit)) {
            return;
        }
        if (screen != null) {
            screen.isHovered(id, hit[0], hit[1], hit[2], hit[3]);
        } else {
            fallback.markHovered(id, hit[0], hit[1], hit[2], hit[3]);
        }
    }

    public boolean wasHovered(Object id) {
        return screen != null ? screen.isHovered(id, 0, 0, 0, 0) : fallback.wasHovered(id);
    }

    public boolean beginDrag(Object owner, int button, float x, float y, float w, float h) {
        float[] hit = clippedHit(x, y, w, h);
        if (!Scissor.hasArea(hit)) {
            return screen != null ? screen.isDragging(owner) : fallback.isDragging(owner);
        }
        return screen != null
                ? screen.beginDrag(owner, button, hit[0], hit[1], hit[2], hit[3])
                : fallback.beginDrag(owner, button, hit[0], hit[1], hit[2], hit[3]);
    }

    public boolean isDragging(Object owner) {
        return screen != null ? screen.isDragging(owner) : fallback.isDragging(owner);
    }

    public void releaseDrag(Object owner) {
        if (screen != null) {
            screen.releaseDrag(owner);
        } else {
            fallback.releaseDrag(owner);
        }
    }

    public boolean isKeyDown(int keyCode) {
        return fallback.isKeyDown(keyCode);
    }

    public boolean consumeKey(int keyCode) {
        return fallback.consumeKey(keyCode);
    }

    public int consumeRawKey() {
        return fallback.consumeRawKey();
    }

    public String typedChars() {
        return fallback.typedChars();
    }

    public String clipboard() {
        return fallback.clipboard();
    }

    public void setClipboard(String text) {
        fallback.setClipboard(text);
    }

    /** Intersect the widget with the active canvas clip so off-screen rows cannot steal clicks. */
    private float[] clippedHit(float x, float y, float w, float h) {
        return Scissor.constrainHit(canvas.currentClip(), x, y, w, h);
    }
}

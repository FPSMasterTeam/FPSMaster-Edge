package top.fpsmaster.ui.kit;

import top.fpsmaster.utils.render.gui.ScaledGuiScreen;
import top.fpsmaster.uikit.input.FrameInput;
import top.fpsmaster.uikit.input.Input;
import top.fpsmaster.uikit.input.PointerEvent;

final class EdgeInput implements Input {
    private final ScaledGuiScreen screen;
    private final FrameInput fallback;

    EdgeInput(ScaledGuiScreen screen, FrameInput fallback) {
        this.screen = screen;
        this.fallback = fallback;
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
        if (screen == null) {
            return fallback.consumePressInBounds(x, y, w, h, button);
        }
        ScaledGuiScreen.PointerEvent event = screen.consumePressInBounds(x, y, w, h, button);
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
        return screen != null ? screen.consumeWheelDelta(x, y, w, h) : fallback.consumeWheelDelta(x, y, w, h);
    }

    public void markHovered(Object id, float x, float y, float w, float h) {
        if (screen != null) {
            screen.isHovered(id, x, y, w, h);
        } else {
            fallback.markHovered(id, x, y, w, h);
        }
    }

    public boolean wasHovered(Object id) {
        return screen != null ? screen.isHovered(id, 0, 0, 0, 0) : fallback.wasHovered(id);
    }

    public boolean beginDrag(Object owner, int button, float x, float y, float w, float h) {
        return screen != null ? screen.beginDrag(owner, button, x, y, w, h)
                : fallback.beginDrag(owner, button, x, y, w, h);
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

    public String typedChars() {
        return fallback.typedChars();
    }

    public String clipboard() {
        return fallback.clipboard();
    }

    public void setClipboard(String text) {
        fallback.setClipboard(text);
    }
}

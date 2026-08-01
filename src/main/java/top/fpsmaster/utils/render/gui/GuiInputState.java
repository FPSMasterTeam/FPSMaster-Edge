package top.fpsmaster.utils.render.gui;

import java.util.ArrayList;
import java.util.List;

public final class GuiInputState {
    public static final class MouseButtonEvent {
        private final int x;
        private final int y;
        private final int button;
        private boolean consumed;

        private MouseButtonEvent(int x, int y, int button) {
            this.x = x;
            this.y = y;
            this.button = button;
        }

        public int getX() {
            return x;
        }

        public int getY() {
            return y;
        }

        public int getButton() {
            return button;
        }

        public boolean isConsumed() {
            return consumed;
        }

        private void consume() {
            this.consumed = true;
        }
    }

    private final List<MouseButtonEvent> pressEvents = new ArrayList<>();
    private final boolean[] buttonsDown = new boolean[8];

    private MouseButtonEvent latestPress;
    private int mouseX;
    private int mouseY;
    private int wheelDelta;

    private Object hoveredId;
    private Object lastHoveredId;

    public void updateMousePosition(int mouseX, int mouseY) {
        this.mouseX = mouseX;
        this.mouseY = mouseY;
    }

    public void pressButton(int button, int mouseX, int mouseY) {
        if (button >= 0 && button < buttonsDown.length) {
            buttonsDown[button] = true;
        }
        MouseButtonEvent event = new MouseButtonEvent(mouseX, mouseY, button);
        pressEvents.add(event);
        latestPress = event;
    }

    public void releaseButton(int button) {
        if (button >= 0 && button < buttonsDown.length) {
            buttonsDown[button] = false;
        }
    }

    public boolean isButtonDown(int button) {
        return button >= 0 && button < buttonsDown.length && buttonsDown[button];
    }

    public void addWheelDelta(int wheelDelta) {
        this.wheelDelta += wheelDelta;
    }

    public int getWheelDelta() {
        return wheelDelta;
    }

    public int consumeWheelDelta() {
        int currentWheelDelta = wheelDelta;
        wheelDelta = 0;
        return currentWheelDelta;
    }

    public boolean hasPressEvent() {
        return !pressEvents.isEmpty();
    }

    /**
     * The most recent press that nobody has claimed yet.
     *
     * <p>This used to return the press regardless of consumption, so a widget peeking at it could act
     * on a click another widget had already handled — which is why {@code MainPanel} needed a
     * {@code hasPointerCapture()} guard to undo the double handling.
     */
    public MouseButtonEvent getLatestPress() {
        return latestPress != null && latestPress.isConsumed() ? null : latestPress;
    }

    /**
     * Records that the cursor is inside this widget, overwriting whatever was recorded earlier in the
     * frame.
     *
     * <p>This is how z-order falls out of an immediate-mode GUI: widgets are visited in paint order,
     * so the <em>last</em> one to claim the cursor is the one drawn on top. No comparison is needed —
     * later simply wins. The result is only usable next frame, once the whole frame has been walked,
     * which is why {@link #wasHovered} reads the previous frame's value. GUI layout barely moves
     * between frames, so the one-frame lag is not observable.
     */
    public void markHovered(Object id, float x, float y, float width, float height) {
        if (mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height) {
            hoveredId = id;
        }
    }

    /** Whether {@code id} was the topmost widget under the cursor at the end of the previous frame. */
    public boolean wasHovered(Object id) {
        return id != null && id.equals(lastHoveredId);
    }

    /** True until any widget has claimed the cursor, i.e. nothing is known to be on top yet. */
    public boolean hasHoveredId() {
        return lastHoveredId != null;
    }

    public int getMouseX() {
        return mouseX;
    }

    public int getMouseY() {
        return mouseY;
    }

    public MouseButtonEvent consumePressInBounds(float x, float y, float width, float height) {
        return consumePressInBounds(x, y, width, height, -1);
    }

    public MouseButtonEvent consumePressInBounds(float x, float y, float width, float height, int button) {
        for (MouseButtonEvent event : pressEvents) {
            if (event.isConsumed()) {
                continue;
            }
            if (button >= 0 && event.getButton() != button) {
                continue;
            }
            if (event.getX() < x || event.getX() > x + width || event.getY() < y || event.getY() > y + height) {
                continue;
            }
            event.consume();
            return event;
        }
        return null;
    }

    /**
     * Claims a press that landed <em>outside</em> the given rectangle — the "click away to dismiss"
     * gesture for popups.
     *
     * <p>Consuming it is the whole point: a popup that merely notices the outside click and closes
     * itself lets that same click through to whatever it was covering, so dismissing a dropdown also
     * activates the button behind it.
     */
    public MouseButtonEvent consumePressOutside(float x, float y, float width, float height) {
        for (MouseButtonEvent event : pressEvents) {
            if (event.isConsumed()) {
                continue;
            }
            boolean inside = event.getX() >= x && event.getX() <= x + width
                    && event.getY() >= y && event.getY() <= y + height;
            if (inside) {
                continue;
            }
            event.consume();
            return event;
        }
        return null;
    }

    public void finishFrame() {
        // Hand this frame's topmost-under-cursor result to the next frame, where it arbitrates clicks.
        lastHoveredId = hoveredId;
        hoveredId = null;
        pressEvents.clear();
        latestPress = null;
        wheelDelta = 0;
    }

    public void reset() {
        for (int i = 0; i < buttonsDown.length; i++) {
            buttonsDown[i] = false;
        }
        mouseX = 0;
        mouseY = 0;
        lastHoveredId = null;
        finishFrame();
    }
}

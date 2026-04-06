package top.fpsmaster.ui.click.component;

import lombok.Getter;
import lombok.Setter;
import top.fpsmaster.utils.math.anim.AnimClock;
import top.fpsmaster.utils.math.anim.Animator;
import top.fpsmaster.utils.math.anim.Easings;
import top.fpsmaster.utils.render.draw.Hover;
import top.fpsmaster.utils.render.draw.Rects;
import top.fpsmaster.utils.render.gui.ScaledGuiScreen;

import java.awt.*;

public class ScrollContainer {
    private static final float WHEEL_STEP = 50f;
    private static final float MAX_OVERSCROLL = 45f;
    private static final float OVERSCROLL_RESISTANCE = 0.38f;
    private static final long SNAP_BACK_DELAY_NS = 50_000_000L;
    private static final float TRACK_MIN_HEIGHT = 18f;
    private static final float TRACK_SHRINK_FACTOR = 0.45f;

    private float wheel = 0f;
    private float wheelAnim = 0f;
    @Getter
    @Setter
    private float height = 0f;
    private final Animator wheelAnimator = new Animator();
    private final AnimClock animClock = new AnimClock();
    private float lastTarget = Float.NaN;

    private double scrollExpand = 0.0;
    private float scrollStart = 0f;
    private boolean isScrolling = false;
    private final String captureId = getClass().getName() + "@" + Integer.toHexString(System.identityHashCode(this));
    private long wheelDelay = 0L;

    public void draw(ScaledGuiScreen screen, float x, float y, float width, float height, int mouseX, int mouseY, Runnable runnable) {
        double dt = animClock.tick();
        runnable.run();

        float maxUp = Math.max(0f, this.height - height);
        float minScroll = -maxUp;

        if (this.height > height) {
            float percent = height / this.height;
            float baseHeight = Math.max(TRACK_MIN_HEIGHT, percent * height);
            float visibleScroll = clamp(wheel, minScroll, 0f);
            float scrollPercent = maxUp <= 0f ? 0f : visibleScroll / maxUp;
            float trackHeight = baseHeight;
            float trackY = y - scrollPercent * (height - baseHeight);

            float overscrollTop = Math.max(0f, wheel);
            float overscrollBottom = Math.max(0f, minScroll - wheel);
            if (overscrollTop > 0f) {
                trackHeight = shrinkTrack(baseHeight, overscrollTop);
                trackY = y;
            } else if (overscrollBottom > 0f) {
                trackHeight = shrinkTrack(baseHeight, overscrollBottom);
                trackY = y + height - trackHeight;
            }

            float trackX = x + width + 1 - (float) scrollExpand;
            Rects.rounded(
                    Math.round(trackX),
                    Math.round(trackY),
                    Math.round(1f + (float) scrollExpand),
                    Math.round(trackHeight),
                    1,
                    new Color(255, 255, 255, 100).getRGB()
            );

            if (Hover.is(trackX - 1, trackY, 2f + (float) scrollExpand, trackHeight, mouseX, mouseY)) {
                scrollExpand = 1.0;
                if (screen.beginPointerCapture(captureId, 0, trackX - 1, trackY, 2f + (float) scrollExpand, trackHeight)) {
                    isScrolling = true;
                    scrollStart = mouseY - trackY;
                }
            } else if (!isScrolling) {
                scrollExpand = 0.0;
            }

            if (isScrolling) {
                if (screen.isPointerCapturedBy(captureId, 0)) {
                    float dragScroll = -((mouseY - scrollStart - y) / height) * this.height;
                    wheelAnim = applyOverscrollResistance(dragScroll, minScroll, 0f);
                    wheelDelay = System.nanoTime();
                } else {
                    isScrolling = false;
                    screen.releasePointerCapture(captureId);
                }
            }
        } else {
            wheelAnim = 0f;
        }

        if (Hover.is(x, y, width, height, mouseX, mouseY) && this.height > height) {
            int mouseDWheel = screen.consumeWheelDelta(x, y, width, height);
            if (mouseDWheel != 0) {
                wheelDelay = System.nanoTime();
                if (mouseDWheel > 0) {
                    wheelAnim = applyOverscrollResistance(wheelAnim + WHEEL_STEP, minScroll, 0f);
                } else {
                    wheelAnim = applyOverscrollResistance(wheelAnim - WHEEL_STEP, minScroll, 0f);
                }
            }
        }

        if (Float.isNaN(lastTarget) || wheelAnim != lastTarget) {
            wheelAnimator.animateTo(wheelAnim, 0.12f, Easings.CUBIC_OUT);
            lastTarget = wheelAnim;
        } else if (System.nanoTime() - wheelDelay > SNAP_BACK_DELAY_NS) {
            float bounded = clamp(wheelAnim, minScroll, 0f);
            if (bounded != wheelAnim) {
                wheelAnim = bounded;
            }
        }

        wheelAnimator.update(dt);
        wheel = (float) wheelAnimator.get();
    }

    public int getScroll() {
        return (int) wheel;
    }

    public float getRealScroll() {
        return wheelAnim;
    }

    private float applyOverscrollResistance(float value, float min, float max) {
        if (value > max) {
            return Math.min(max + MAX_OVERSCROLL, max + (value - max) * OVERSCROLL_RESISTANCE);
        }
        if (value < min) {
            return Math.max(min - MAX_OVERSCROLL, min + (value - min) * OVERSCROLL_RESISTANCE);
        }
        return value;
    }

    private float shrinkTrack(float baseHeight, float overscroll) {
        float shrink = Math.min(baseHeight - TRACK_MIN_HEIGHT, overscroll * TRACK_SHRINK_FACTOR);
        return Math.max(TRACK_MIN_HEIGHT, baseHeight - shrink);
    }

    private float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}

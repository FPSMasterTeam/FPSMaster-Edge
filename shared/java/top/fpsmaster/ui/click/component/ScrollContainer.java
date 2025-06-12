package top.fpsmaster.ui.click.component;

import org.lwjgl.input.Mouse;
import top.fpsmaster.ui.click.MainPanel;
import top.fpsmaster.utils.math.animation.AnimationUtils;
import top.fpsmaster.utils.render.Render2DUtils;

import java.awt.Color;

public class ScrollContainer {
    private float wheel = 0f;
    private float wheel_anim = 0f;
    private float height = 0f;

    private double scrollExpand = 0.0;
    private float scrollStart = 0f;
    private boolean isScrolling = false;

    public void draw(float x, float y, float width, float height, int mouseX, int mouseY, Runnable runnable) {
        runnable.run();

        // if the scroll bar needs to be render
        if (this.height > height) {
            // calc scroll bar height
            float percent = (height / this.height);
            float sHeight = percent * height;
            float scrollPercent = (getScroll() / (this.height - height));
            float sY = y - scrollPercent * (height - sHeight);
            float sX = x + width + 1 - (float) scrollExpand;
            Render2DUtils.drawOptimizedRoundedRect(
                sX,
                sY,
                1f + (float) scrollExpand,
                sHeight,
                1,
                new Color(255, 255, 255, 100).getRGB()
            );
            if (Render2DUtils.isHovered(
                    sX - 1,
                    sY,
                    2f + (float) scrollExpand,
                    sHeight, mouseX, mouseY
                )
            ) {
                scrollExpand = 1.0;
                if (Mouse.isButtonDown(0)) {
                    if (!isScrolling && "null".equals(MainPanel.dragLock)) {
                        isScrolling = true;
                        MainPanel.dragLock = this.getClass().getSimpleName();
                        scrollStart = mouseY - sY;
                    }
                }
            } else if (!isScrolling) {
                scrollExpand = 0.0;
            }

            if (isScrolling) {
                if (Mouse.isButtonDown(0)) {
                    wheel_anim = -((mouseY - scrollStart - y) / height) * this.height;
                } else {
                    isScrolling = false;
                    MainPanel.bindLock = "null";
                }
            }
        } else {
            wheel_anim = 0f;
        }

        if (Render2DUtils.isHovered(x,y,width,height, mouseX, mouseY)) {
            if (this.height > height) {
                // mods list scroll
                int mouseDWheel = Mouse.getDWheel();
                if (mouseDWheel > 0) {
                    wheel_anim += 20f;
                } else if (mouseDWheel < 0) {
                    wheel_anim -= 20f;
                }
                float maxUp = this.height - height;
                wheel_anim = Math.min(Math.max(wheel_anim, -maxUp), 0f);
            }
            wheel = (float) AnimationUtils.base((double) wheel, (double) wheel_anim, 0.2);
        }
    }

    public void setHeight(float height) {
        this.height = height;
    }

    public float getHeight() {
        return this.height;
    }

    public float getScroll() {
        return wheel;
    }

    public float getRealScroll() {
        return wheel_anim;
    }
}

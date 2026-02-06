package top.fpsmaster.ui.common;

import top.fpsmaster.utils.render.draw.Hover;
import top.fpsmaster.utils.render.draw.Rects;

import top.fpsmaster.FPSMaster;
import top.fpsmaster.utils.math.animation.ColorAnimation;

import java.awt.*;

public class GuiButton {

    private final String text;
    private final Runnable runnable;
    private float x = 0f;
    private float y = 0f;
    private float width = 0f;
    private float height = 0f;
    Color color;
    Color hoverColor;
    private final ColorAnimation btnColor = new ColorAnimation(new Color(113, 127, 254));


    public GuiButton(String text, Runnable runnable, Color color, Color hoverColor) {
        this.text = text;
        this.runnable = runnable;
        this.color = color;
        this.hoverColor = hoverColor;
    }

    public GuiButton(String text, Runnable runnable) {
        this(text, runnable, new Color(113, 127, 254), new Color(135, 147, 255));
    }


    public void render(float x, float y, float width, float height, float mouseX, float mouseY) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;

        if (Hover.is(x, y, width, height, (int) mouseX, (int) mouseY)) {
            btnColor.base(hoverColor);
        } else {
            btnColor.base(color);
        }

        Rects.rounded(x, y, width, height, btnColor.getColor());
        FPSMaster.fontManager.s18.drawCenteredString(
                FPSMaster.i18n.get(text),
                x + width / 2,
                y + height / 2 - 4,
                new Color(255, 255, 255).getRGB()
        );
    }

    public void mouseClick(float mouseX, float mouseY, int btn) {
        if (Hover.is(x, y, width, height, (int) mouseX, (int) mouseY) && btn == 0) {
            runnable.run();
        }
    }
}





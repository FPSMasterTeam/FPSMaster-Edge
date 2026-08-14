package top.fpsmaster.ui.screens.oobe;

import top.fpsmaster.FPSMaster;
import top.fpsmaster.ui.click.ClickGuiTheme;
import top.fpsmaster.ui.common.control.UiControl;
import top.fpsmaster.utils.math.anim.AnimClock;
import top.fpsmaster.utils.math.anim.AnimMath;
import top.fpsmaster.utils.math.anim.ColorAnimator;
import top.fpsmaster.utils.render.draw.Hover;
import top.fpsmaster.utils.render.draw.Rects;
import top.fpsmaster.utils.render.gui.ScaledGuiScreen;

import java.awt.Color;

public class OobeButton implements UiControl {
    private final Runnable onClick;
    private final ColorAnimator colorAnimator;
    private final AnimClock clock = new AnimClock();

    private String text;
    private float x;
    private float y;
    private float width;
    private float height;
    private boolean primary;
    private boolean enabled = true;
    private float hoverAnim;
    private float pressAnim;

    public OobeButton(String text, boolean primary, Runnable onClick) {
        this.text = text;
        this.primary = primary;
        this.onClick = onClick;
        this.colorAnimator = new ColorAnimator(primary ? ClickGuiTheme.accent() : ClickGuiTheme.layer());
    }

    public OobeButton setText(String text) {
        this.text = text;
        return this;
    }

    public OobeButton setPrimary(boolean primary) {
        this.primary = primary;
        return this;
    }

    public OobeButton setEnabled(boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    @Override
    public void render(float x, float y, float width, float height, float mouseX, float mouseY) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        float dt = (float) clock.tick();
        colorAnimator.update(dt);
        boolean hovered = enabled && Hover.is(x, y, width, height, (int) mouseX, (int) mouseY);
        hoverAnim = (float) AnimMath.base(hoverAnim, hovered ? 1.0 : 0.0, 0.22);
        pressAnim = (float) AnimMath.base(pressAnim, 0.0, 0.30);
        Color target = primary
                ? (hovered ? ClickGuiTheme.accentHover() : ClickGuiTheme.accent())
                : (hovered ? ClickGuiTheme.layerHover() : ClickGuiTheme.layer());
        colorAnimator.base(target);
        float inset = pressAnim * 1.5f;
        float drawX = x + inset;
        float drawY = y + inset;
        float drawWidth = Math.max(4f, width - inset * 2f);
        float drawHeight = Math.max(4f, height - inset * 2f);
        Color fillColor = colorAnimator.getColor();
        Rects.rounded(drawX, drawY, drawWidth, drawHeight, 5, fillColor.getRGB(), false);
        int textColor = primary ? Color.WHITE.getRGB() : ClickGuiTheme.textPrimary().getRGB();
        FPSMaster.fontManager.s14.drawCenteredString(text, x + width / 2f, y + height / 2f - 3.5f + pressAnim * 0.5f, textColor);
    }

    @Override
    public void renderInScreen(ScaledGuiScreen screen, float x, float y, float width, float height, float mouseX, float mouseY) {
        render(x, y, width, height, mouseX, mouseY);
        if (!enabled) {
            return;
        }
        ScaledGuiScreen.PointerEvent click = screen.consumePressInBounds(x, y, width, height, 0);
        if (click != null) {
            mouseClicked(click.x, click.y, click.button);
        }
    }

    @Override
    public void mouseClicked(float mouseX, float mouseY, int button) {
        if (button == 0 && enabled && Hover.is(x, y, width, height, (int) mouseX, (int) mouseY)) {
            pressAnim = 1.0f;
            onClick.run();
        }
    }
}

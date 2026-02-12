package top.fpsmaster.ui.common;

import net.minecraft.util.ResourceLocation;
import org.lwjgl.input.Mouse;
import top.fpsmaster.FPSMaster;
import top.fpsmaster.ui.common.control.UiControl;
import top.fpsmaster.utils.math.anim.ColorAnimator;
import top.fpsmaster.utils.render.draw.Hover;
import top.fpsmaster.utils.render.draw.Rects;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Iterator;

public class GuiButton implements UiControl {
    public enum ClickEffect {
        NONE,
        STACK
    }

    private static final class PressAnim {
        private float progress;
        private float alpha = 1f;
    }

    private String text;
    private boolean i18n = true;
    private ResourceLocation icon;
    private float iconWidth = 12f;
    private float iconHeight = 12f;
    private final Runnable runnable;

    private float x;
    private float y;
    private float width;
    private float height;

    private Color backgroundColor;
    private Color hoverColor;
    private Color pressedColor;
    private int roundRadius = 4;

    private ClickEffect clickEffect = ClickEffect.NONE;
    private Color clickEffectColor = new Color(255, 255, 255, 80);
    private float clickEffectDuration = 0.25f;
    private final ArrayList<PressAnim> pressAnims = new ArrayList<>();
    private long lastRenderNanos = System.nanoTime();

    private final ColorAnimator buttonAnimator;

    public GuiButton(String text, Runnable runnable, Color backgroundColor, Color hoverColor) {
        this.text = text;
        this.runnable = runnable;
        this.backgroundColor = backgroundColor;
        this.hoverColor = hoverColor;
        this.pressedColor = hoverColor;
        this.buttonAnimator = new ColorAnimator(backgroundColor);
    }

    public GuiButton(String text, Runnable runnable) {
        this(text, runnable, new Color(113, 127, 254), new Color(135, 147, 255));
    }

    public GuiButton setBackgroundColors(Color backgroundColor, Color hoverColor, Color pressedColor) {
        this.backgroundColor = backgroundColor;
        this.hoverColor = hoverColor;
        this.pressedColor = pressedColor;
        return this;
    }

    public GuiButton setRoundRadius(int roundRadius) {
        this.roundRadius = Math.max(0, roundRadius);
        return this;
    }

    public GuiButton setText(String text, boolean i18n) {
        this.text = text;
        this.i18n = i18n;
        return this;
    }

    public GuiButton setIcon(ResourceLocation icon, float iconWidth, float iconHeight) {
        this.icon = icon;
        this.iconWidth = iconWidth;
        this.iconHeight = iconHeight;
        return this;
    }

    public GuiButton setClickEffect(ClickEffect clickEffect, Color clickEffectColor, float clickEffectDuration) {
        this.clickEffect = clickEffect;
        this.clickEffectColor = clickEffectColor;
        this.clickEffectDuration = Math.max(0.05f, clickEffectDuration);
        return this;
    }

    @Override
    public void render(float x, float y, float width, float height, float mouseX, float mouseY) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;

        updateAnimations();

        boolean hovered = Hover.is(x, y, width, height, (int) mouseX, (int) mouseY);
        Color target;
        if (hovered && Mouse.isButtonDown(0)) {
            target = pressedColor;
        } else if (hovered) {
            target = hoverColor;
        } else {
            target = backgroundColor;
        }
        buttonAnimator.base(target);

        Rects.rounded(Math.round(x), Math.round(y), Math.round(width), Math.round(height), roundRadius, buttonAnimator.getColor().getRGB());
        drawStackEffect();
        drawContent();
    }

    @Override
    public void mouseClicked(float mouseX, float mouseY, int button) {
        if (!Hover.is(x, y, width, height, (int) mouseX, (int) mouseY) || button != 0) {
            return;
        }

        if (clickEffect == ClickEffect.STACK) {
            pressAnims.add(new PressAnim());
        }
        runnable.run();
    }

    private void updateAnimations() {
        long now = System.nanoTime();
        float dt = (float) ((now - lastRenderNanos) / 1_000_000_000.0);
        lastRenderNanos = now;
        if (dt <= 0f) {
            return;
        }

        Iterator<PressAnim> iterator = pressAnims.iterator();
        while (iterator.hasNext()) {
            PressAnim anim = iterator.next();
            anim.progress += dt / clickEffectDuration;
            if (anim.progress >= 1.0f) {
                anim.alpha -= dt * 2.0f;
                if (anim.alpha <= 0f) {
                    iterator.remove();
                }
            }
        }
    }

    private void drawStackEffect() {
        if (clickEffect != ClickEffect.STACK || pressAnims.isEmpty()) {
            return;
        }

        for (PressAnim anim : pressAnims) {
            float progress = Math.min(anim.progress, 1.0f);
            float sizeW = width * progress;
            float sizeH = height * progress;
            int alpha = Math.max(0, Math.min(255, (int) (clickEffectColor.getAlpha() * anim.alpha)));
            Color c = new Color(clickEffectColor.getRed(), clickEffectColor.getGreen(), clickEffectColor.getBlue(), alpha);
            Rects.rounded(
                    Math.round(x + width / 2f - sizeW / 2f),
                    Math.round(y + height / 2f - sizeH / 2f),
                    Math.round(sizeW),
                    Math.round(sizeH),
                    roundRadius,
                    c.getRGB()
            );
        }
    }

    private void drawContent() {
        int textColor = new Color(255, 255, 255).getRGB();
        String display = text == null ? "" : (i18n ? FPSMaster.i18n.get(text) : text);
        boolean hasText = display != null && !display.isEmpty();

        if (icon != null && hasText) {
            float textW = FPSMaster.fontManager.s18.getStringWidth(display);
            float totalW = iconWidth + 6f + textW;
            float startX = x + (width - totalW) / 2f;
            top.fpsmaster.utils.render.draw.Images.draw(icon, startX, y + (height - iconHeight) / 2f, iconWidth, iconHeight, textColor);
            FPSMaster.fontManager.s18.drawString(display, startX + iconWidth + 6f, y + height / 2f - 4f, textColor);
            return;
        }

        if (icon != null) {
            top.fpsmaster.utils.render.draw.Images.draw(icon, x + width / 2f - iconWidth / 2f, y + height / 2f - iconHeight / 2f, iconWidth, iconHeight, textColor);
            return;
        }

        FPSMaster.fontManager.s18.drawCenteredString(display, x + width / 2f, y + height / 2f - 4f, textColor);
    }
}

package top.fpsmaster.ui.custom.impl;

import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;
import top.fpsmaster.features.impl.interfaces.CPSDisplay;
import top.fpsmaster.features.impl.interfaces.Keystrokes;
import top.fpsmaster.ui.custom.Component;
import top.fpsmaster.utils.math.anim.AnimClock;
import top.fpsmaster.utils.math.anim.ColorAnimator;
import top.fpsmaster.utils.render.StencilUtil;
import top.fpsmaster.utils.render.draw.Circles;
import top.fpsmaster.utils.render.draw.Colors;
import top.fpsmaster.utils.render.draw.Rects;
import top.fpsmaster.utils.render.state.Alpha;

import java.awt.*;
import java.util.ArrayList;
import java.util.Iterator;

public class KeystrokesComponent extends Component {

    private final ArrayList<Key> keys = new ArrayList<>();
    private final AnimClock animClock = new AnimClock();

    public KeystrokesComponent() {
        super(Keystrokes.class);
        keys.add(new Key("W", Keyboard.KEY_W, 20, 0, 18f, 18f));
        keys.add(new Key("A", Keyboard.KEY_A, 0, 20, 18f, 18f));
        keys.add(new Key("S", Keyboard.KEY_S, 20, 20, 18f, 18f));
        keys.add(new Key("D", Keyboard.KEY_D, 40, 20, 18f, 18f));
        keys.add(new Key("LMB", -1, 0, 40, 28f, 18f));
        keys.add(new Key("RMB", -2, 30, 40, 28f, 18f));
        keys.add(new Key("SPACE", Keyboard.KEY_SPACE, 0, 60, 58f, 12f));
        allowScale = true;
    }

    @Override
    public void draw(float x, float y) {
        super.draw(x, y);
        double dt = animClock.tick();
        for (Key key : keys) {
            switch (key.keyCode) {
                case Keyboard.KEY_W:
                    key.yOffset = key.defaultYOffset - mod.spacing.getValue().intValue();
                    break;
                case Keyboard.KEY_A:
                    key.xOffset = key.defaultXOffset - mod.spacing.getValue().intValue();
                    break;
                case Keyboard.KEY_D:
                    key.xOffset = key.defaultXOffset + mod.spacing.getValue().intValue();
                    break;
                case -1:
                    key.xOffset = key.defaultXOffset - mod.spacing.getValue().intValue();
                    key.yOffset = key.defaultYOffset + mod.spacing.getValue().intValue();
                    break;
                case -2:
                    key.xOffset = key.defaultXOffset + mod.spacing.getValue().intValue();
                    key.yOffset = key.defaultYOffset + mod.spacing.getValue().intValue();
                    break;
                case Keyboard.KEY_SPACE:
                    key.xOffset = key.defaultXOffset - mod.spacing.getValue().intValue();
                    key.yOffset = key.defaultYOffset + mod.spacing.getValue().intValue() * 2;
                    break;
            }
            key.render(x, y, (float) dt, mod.backgroundColor.getColor(), Keystrokes.pressedColor.getColor());
        }
        float spacing = mod.spacing.getValue().floatValue();
        width = 60f + spacing * 2f;
        height = (Keystrokes.showSpace.getValue() ? 72f : 60f) + spacing * 2f;
    }

    public class Key {
        private final String name;
        private final int keyCode;
        private final int defaultXOffset;
        private final int defaultYOffset;
        private final float baseWidth;
        private final float baseHeight;
        private int xOffset;
        private int yOffset;
        private final ColorAnimator color;
        private final ArrayList<PressAnim> pressAnims = new ArrayList<>();
        private boolean lastPressed = false;

        public Key(String name, int keyCode, int xOffset, int yOffset, float width, float height) {
            this.name = name;
            this.keyCode = keyCode;
            this.defaultXOffset = xOffset;
            this.defaultYOffset = yOffset;
            this.baseWidth = width;
            this.baseHeight = height;
            this.color = new ColorAnimator();
            this.xOffset = defaultXOffset;
            this.yOffset = defaultYOffset;
        }

        public void render(float x, float y, float speed, Color color, Color color1) {
            if (keyCode == Keyboard.KEY_SPACE && !Keystrokes.showSpace.getValue()) {
                pressAnims.clear();
                lastPressed = false;
                return;
            }
            boolean pressed;
            float keyW = baseWidth;
            float keyH = baseHeight;
            float keyX;
            float keyY;
            float spacing = mod.spacing.getValue().floatValue();
            
            if (keyCode == -1) {
                pressed = Mouse.isButtonDown(0);
                keyX = x + xOffset * scale;
                keyY = y + yOffset * scale;
            } else if (keyCode == -2) {
                pressed = Mouse.isButtonDown(1);
                keyX = x + xOffset * scale;
                keyY = y + yOffset * scale;
            } else {
                pressed = Keyboard.isKeyDown(keyCode);
                keyX = x + xOffset * scale;
                keyY = y + yOffset * scale;
            }
            if (keyCode == Keyboard.KEY_SPACE) {
                keyW = baseWidth + spacing * 2f;
                keyX = x + xOffset * scale;
            }

            this.color.base(pressed ? color1 : color);

            if (pressed && !lastPressed) {
                if (Keystrokes.pressAnimMode.isMode("Bloom") || Keystrokes.pressAnimMode.isMode("Stack")) {
                    pressAnims.add(new PressAnim());
                }
            }

            if (!pressed && (Keystrokes.pressAnimMode.isMode("Pulse") || Keystrokes.pressAnimMode.isMode("Ripple"))) {
                pressAnims.clear();
            }

            updatePressAnims(speed, keyW, keyH, pressed);

            drawKeyBase(keyX, keyY, keyW, keyH, this.color.getColor());
            drawPressAnimation(keyX, keyY, keyW, keyH, pressed);

            int textColor = resolveTextColor(keyX, keyY, pressed);
            if (keyCode == -1 || keyCode == -2) {
                long cps = keyCode == -1 ? CPSDisplay.lcps : CPSDisplay.rcps;
                if (Keystrokes.cpsMode.isMode("Below")) {
                    float labelY = keyY + 2 * scale;
                    float cpsY = keyY + (keyH - 8f) * scale;
                    drawCenteredString(12, name, keyX, keyW, labelY, textColor);
                    drawCenteredString(12, cps + " CPS", keyX, keyW, cpsY, textColor);
                } else if (Keystrokes.cpsMode.isMode("ClickOnly") && cps > 0) {
                    drawCenteredString(16, cps + " CPS", keyX, keyW, keyY + 4 * scale, textColor);
                } else {
                    if (keyCode == -1) {
                        drawString(16, name, keyX + 7 * scale, keyY + 4 * scale, textColor);
                    } else {
                        drawString(16, name, keyX + 6 * scale, keyY + 4 * scale, textColor);
                    }
                }
            } else if (keyCode == Keyboard.KEY_SPACE && Keystrokes.spaceStyle.isMode("Bar")) {
                drawSpaceBar(keyX, keyY, keyW, keyH, textColor);
            } else if (isWASDKey() && Keystrokes.wasdStyle.isMode("Triangle")) {
                drawTriangleGlyph(keyX, keyY, keyW, keyH, resolveTriangleDirection(), textColor);
            } else {
                drawString(16, name, keyX + ((keyW - getStringWidth(16, name)) / 2f) * scale, keyY + 4 * scale, textColor);
            }

            lastPressed = pressed;
        }

        private void drawCenteredString(int fontSize, String text, float keyX, float keyW, float drawY, int color) {
            float textX = keyX + ((keyW - getStringWidth(fontSize, text)) / 2f) * scale;
            drawString(fontSize, text, textX, drawY, color);
        }

        private boolean isWASDKey() {
            return keyCode == Keyboard.KEY_W || keyCode == Keyboard.KEY_A || keyCode == Keyboard.KEY_S || keyCode == Keyboard.KEY_D;
        }

        private TriangleDirection resolveTriangleDirection() {
            if (keyCode == Keyboard.KEY_W) {
                return TriangleDirection.UP;
            }
            if (keyCode == Keyboard.KEY_A) {
                return TriangleDirection.LEFT;
            }
            if (keyCode == Keyboard.KEY_S) {
                return TriangleDirection.DOWN;
            }
            return TriangleDirection.RIGHT;
        }

        private void drawTriangleGlyph(float keyX, float keyY, float keyW, float keyH, TriangleDirection direction, int color) {
            float size = Math.min(keyW, keyH) * 0.5f;
            float triX = keyX + ((keyW - size) / 2f) * scale;
            float triY = keyY + ((keyH - size) / 2f) * scale;
            drawTriangle(triX, triY, size, size, direction, color);
        }

        private void drawSpaceBar(float keyX, float keyY, float keyW, float keyH, int color) {
            float barW = keyW * 0.6f;
            float barH = Math.max(2f, keyH * 0.2f);
            float barX = keyX + ((keyW - barW) / 2f) * scale;
            float barY = keyY + ((keyH - barH) / 2f) * scale;
            Rects.fill(barX, barY, barW * scale, barH * scale, color);
        }

        private void drawKeyBase(float x, float y, float width, float height, Color bg) {
            if (!mod.bg.getValue()) {
                return;
            }
            float borderWidth = Keystrokes.borderWidth.getValue().floatValue();
            if (borderWidth > 0) {
                drawBorderRect(x, y, width, height, borderWidth);
            }
            drawRect(x, y, width, height, bg);
        }

        private int resolveTextColor(float x, float y, boolean pressed) {
            float offset = (x + y) * 0.002f;
            return pressed ? Keystrokes.pressedFontColor.updateAndGetRGB(offset) : Keystrokes.fontColor.updateAndGetRGB(offset);
        }

        private Color resolveBorderColor(float x, float y) {
            float offset = (x + y) * 0.002f;
            return Keystrokes.borderColor.updateAndGetColor(offset);
        }

        private void drawBorderRect(float x, float y, float width, float height, float borderWidth) {
            float bw = borderWidth * scale;
            float scaledW = width * scale;
            float scaledH = height * scale;
            float outerX = x - bw;
            float outerY = y - bw;
            float outerW = scaledW + bw * 2f;
            float outerH = scaledH + bw * 2f;
            int radius = mod.rounded.getValue() ? mod.roundRadius.getValue().intValue() : 0;
            Color border = resolveBorderColor(x, y);

            StencilUtil.initStencilToWrite();
            Rects.rounded(Math.round(outerX), Math.round(outerY), Math.round(outerW), Math.round(outerH), radius, new Color(0, 0, 0, 255));
            org.lwjgl.opengl.GL11.glStencilFunc(org.lwjgl.opengl.GL11.GL_ALWAYS, 0, 0xFF);
            org.lwjgl.opengl.GL11.glStencilOp(org.lwjgl.opengl.GL11.GL_REPLACE, org.lwjgl.opengl.GL11.GL_REPLACE, org.lwjgl.opengl.GL11.GL_REPLACE);
            Rects.rounded(Math.round(x), Math.round(y), Math.round(scaledW), Math.round(scaledH), radius, new Color(0, 0, 0, 255));
            org.lwjgl.opengl.GL11.glColorMask(true, true, true, true);
            org.lwjgl.opengl.GL11.glStencilFunc(org.lwjgl.opengl.GL11.GL_EQUAL, 1, 0xFF);
            org.lwjgl.opengl.GL11.glStencilOp(org.lwjgl.opengl.GL11.GL_KEEP, org.lwjgl.opengl.GL11.GL_KEEP, org.lwjgl.opengl.GL11.GL_KEEP);
            Rects.rounded(Math.round(outerX), Math.round(outerY), Math.round(outerW), Math.round(outerH), radius, border);
            StencilUtil.uninitStencilBuffer();
        }

        private void updatePressAnims(float dt, float width, float height, boolean pressed) {
            if (dt <= 0) return;
            if (Keystrokes.pressAnimMode.isMode("Bloom") || Keystrokes.pressAnimMode.isMode("Stack")) {
                Iterator<PressAnim> iterator = pressAnims.iterator();
                while (iterator.hasNext()) {
                    PressAnim anim = iterator.next();
                    anim.progress += dt / Keystrokes.pressAnimDuration.getValue().floatValue();
                    if (anim.progress >= 1.0f) {
                        anim.alpha -= dt * 2.0f;
                        if (anim.alpha <= 0) {
                            iterator.remove();
                        }
                    }
                }
            } else if (Keystrokes.pressAnimMode.isMode("Pulse") || Keystrokes.pressAnimMode.isMode("Ripple")) {
                if (!pressed) {
                    pressAnims.clear();
                    return;
                }
                if (!pressAnims.isEmpty()) {
                    PressAnim anim = pressAnims.get(0);
                    anim.progress = Math.min(1.0f, anim.progress + dt / Keystrokes.pressAnimDuration.getValue().floatValue());
                } else {
                    pressAnims.add(new PressAnim());
                }
            } else {
                pressAnims.clear();
            }
        }

        private void drawPressAnimation(float x, float y, float width, float height, boolean pressed) {
            String mode = Keystrokes.pressAnimMode.getModeName();
            if (mode.equals("Color")) {
                return;
            }
            float scaledW = width * scale;
            float scaledH = height * scale;
            if (mode.equals("Pulse") || mode.equals("Bloom")) {
                StencilUtil.initStencilToWrite();
                Rects.rounded(Math.round(x), Math.round(y), Math.round(scaledW), Math.round(scaledH), mod.rounded.getValue() ? mod.roundRadius.getValue().intValue() : 0, new Color(0, 0, 0, 255));
                StencilUtil.readStencilBuffer(1);
                for (PressAnim anim : pressAnims) {
                    float progress = Math.min(anim.progress, 1.0f);
                    float size = Math.max(scaledW, scaledH) * progress * 1.5f;
                    float alpha = anim.alpha;
                    Color c = new Color(
                            Keystrokes.pressAnimColor.getColor().getRed(),
                            Keystrokes.pressAnimColor.getColor().getGreen(),
                            Keystrokes.pressAnimColor.getColor().getBlue(),
                            (int) (Keystrokes.pressAnimColor.getColor().getAlpha() * alpha)
                    );

                    Circles.fill(x + scaledW / 2f, y + scaledH / 2f, size / 2f, c);
                }
                StencilUtil.uninitStencilBuffer();
                return;
            }
            if (mode.equals("Ripple") || mode.equals("Stack")) {
                for (PressAnim anim : pressAnims) {
                    float progress = Math.min(anim.progress, 1.0f);
                    float sizeW = scaledW * progress;
                    float sizeH = scaledH * progress;
                    float alpha = anim.alpha;
                    Color c = new Color(
                            Keystrokes.pressAnimColor.getColor().getRed(),
                            Keystrokes.pressAnimColor.getColor().getGreen(),
                            Keystrokes.pressAnimColor.getColor().getBlue(),
                            (int) (Keystrokes.pressAnimColor.getColor().getAlpha() * alpha)
                    );
                    Rects.rounded(Math.round(x + scaledW / 2f - sizeW / 2f), Math.round(y + scaledH / 2f - sizeH / 2f), Math.round(sizeW), Math.round(sizeH), mod.rounded.getValue() ? mod.roundRadius.getValue().intValue() : 0, c);
                }
            }
        }
    }

    private static class PressAnim {
        float progress = 0f;
        float alpha = 1f;
    }

    private enum TriangleDirection {
        UP,
        LEFT,
        DOWN,
        RIGHT
    }

    private void drawTriangle(float x, float y, float width, float height, TriangleDirection direction, int color) {
        float scaledW = width * scale;
        float scaledH = height * scale;
        Color c = Colors.toColor(Alpha.apply(color));
        boolean cullEnabled = GL11.glIsEnabled(GL11.GL_CULL_FACE);
        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.enableAlpha();
        if (cullEnabled) {
            GL11.glDisable(GL11.GL_CULL_FACE);
        }
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glColor4f(c.getRed() / 255.0F, c.getGreen() / 255.0F, c.getBlue() / 255.0F, c.getAlpha() / 255.0F);
        GL11.glBegin(GL11.GL_TRIANGLES);
        switch (direction) {
            case UP:
                GL11.glVertex2f(x, y + scaledH);
                GL11.glVertex2f(x + scaledW, y + scaledH);
                GL11.glVertex2f(x + scaledW / 2f, y);
                break;
            case DOWN:
                GL11.glVertex2f(x, y);
                GL11.glVertex2f(x + scaledW / 2f, y + scaledH);
                GL11.glVertex2f(x + scaledW, y);
                break;
            case LEFT:
                GL11.glVertex2f(x + scaledW, y);
                GL11.glVertex2f(x, y + scaledH / 2f);
                GL11.glVertex2f(x + scaledW, y + scaledH);
                break;
            case RIGHT:
                GL11.glVertex2f(x, y);
                GL11.glVertex2f(x + scaledW, y + scaledH / 2f);
                GL11.glVertex2f(x, y + scaledH);
                break;
        }
        GL11.glEnd();
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
        if (cullEnabled) {
            GL11.glEnable(GL11.GL_CULL_FACE);
        }
    }
}

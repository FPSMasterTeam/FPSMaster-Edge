package top.fpsmaster.ui.kit;

import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.opengl.GL11;
import top.fpsmaster.utils.render.draw.Circles;
import top.fpsmaster.utils.render.draw.Images;
import top.fpsmaster.utils.render.draw.Rects;
import top.fpsmaster.utils.render.gui.Scissor;
import top.fpsmaster.prism.canvas.Canvas;
import top.fpsmaster.prism.canvas.FontHandle;
import top.fpsmaster.prism.canvas.ImageHandle;
import top.fpsmaster.prism.theme.Argb;
import top.fpsmaster.utils.imaging.AWTUtils;
import top.fpsmaster.utils.render.gui.UiScale;

import java.util.ArrayDeque;
import java.util.Deque;

final class EdgeCanvas implements Canvas {
    private final Deque<float[]> clips = new ArrayDeque<float[]>();
    private final Deque<Float> alpha = new ArrayDeque<Float>();

    EdgeCanvas() {
        alpha.push(Float.valueOf(1f));
    }

    public void fillRect(float x, float y, float w, float h, int argb) {
        Rects.fill(x, y, w, h, tint(argb));
    }

    public void fillRoundRect(float x, float y, float w, float h, float radius, int argb) {
        if (radius >= Math.min(w, h) * 0.5f - 0.01f) {
            net.minecraft.util.ResourceLocation mask = AWTUtils.generateRoundImage(
                    Math.max(1, Math.round(w)), Math.max(1, Math.round(h)), Math.max(1, Math.round(radius)),
                    UiScale.isActive() ? UiScale.getPixelScale() : 1.0f
            );
            if (mask != null) {
                Images.drawSmooth(mask, x, y, w, h, tint(argb));
                return;
            }
        }
        Rects.rounded(x, y, w, h, Math.max(1, Math.round(radius)), tint(argb), false);
    }

    public void strokeRoundRect(float x, float y, float w, float h, float radius, float strokeWidth, int argb) {
        float s = Math.max(0.5f, strokeWidth);
        fillRoundRect(x - s, y - s, w + s * 2f, h + s * 2f, radius + s, argb);
        fillRoundRect(x, y, w, h, radius, Argb.of(255, 14, 14, 14));
    }

    public void fillCircle(float cx, float cy, float radius, int argb) {
        Circles.fill(cx, cy, radius, tint(argb));
    }

    public void line(float x1, float y1, float x2, float y2, float width, int argb) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        if (Math.abs(dx) < 0.25f || Math.abs(dy) < 0.25f) {
            Rects.fill(Math.min(x1, x2), Math.min(y1, y2), Math.max(width, Math.abs(dx)), Math.max(width, Math.abs(dy)), tint(argb));
            return;
        }
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        float px = -dy / len * (width * 0.5f);
        float py = dx / len * (width * 0.5f);
        int c = tint(argb);
        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glColor4f(Argb.red(c) / 255f, Argb.green(c) / 255f, Argb.blue(c) / 255f, Argb.alpha(c) / 255f);
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glVertex2d(x1 + px, y1 + py);
        GL11.glVertex2d(x1 - px, y1 - py);
        GL11.glVertex2d(x2 - px, y2 - py);
        GL11.glVertex2d(x2 + px, y2 + py);
        GL11.glEnd();
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
    }

    public void fillGradientH(float x, float y, float w, float h, int argbLeft, int argbRight) {
        gradient(x, y, w, h, tint(argbLeft), tint(argbRight), true);
    }

    public void fillGradientV(float x, float y, float w, float h, int argbTop, int argbBottom) {
        gradient(x, y, w, h, tint(argbTop), tint(argbBottom), false);
    }

    public void drawString(FontHandle font, String text, float x, float y, int argb) {
        if (font instanceof EdgeFont) {
            ((EdgeFont) font).renderer.drawString(text, x, y, tint(argb));
        }
    }

    public void drawImage(ImageHandle image, float x, float y, float w, float h, int tintArgb) {
        if (image instanceof EdgeImage) {
            Images.drawSmooth(((EdgeImage) image).location, x, y, w, h, tint(tintArgb));
        }
    }

    public void pushClip(float x, float y, float w, float h) {
        float[] parent = clips.peek();
        float[] next = parent == null
                ? new float[] {x, y, w, h}
                : Scissor.intersect(parent[0], parent[1], parent[2], parent[3], x, y, w, h);
        clips.push(next);
        applyClip(next);
    }

    public void popClip() {
        if (clips.isEmpty()) {
            return;
        }
        clips.pop();
        if (clips.isEmpty()) {
            GL11.glDisable(GL11.GL_SCISSOR_TEST);
            return;
        }
        applyClip(clips.peek());
    }

    /**
     * Current composed clip, or {@code null} when scissor is off. Used by
     * {@link EdgeInput} so hit-tests match what {@link #pushClip} actually draws.
     */
    float[] currentClip() {
        float[] clip = clips.peek();
        if (clip == null) {
            return null;
        }
        return new float[] {clip[0], clip[1], clip[2], clip[3]};
    }

    private static void applyClip(float[] clip) {
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        if (clip[2] <= 0f || clip[3] <= 0f) {
            GL11.glScissor(0, 0, 0, 0);
            return;
        }
        Scissor.apply(clip[0], clip[1], clip[2], clip[3]);
    }

    public void pushAlpha(float a) {
        alpha.push(Float.valueOf(alpha.peek().floatValue() * a));
    }

    public void popAlpha() {
        if (alpha.size() > 1) {
            alpha.pop();
        }
    }

    public void pushTransform() {
        GL11.glPushMatrix();
    }

    public void popTransform() {
        GL11.glPopMatrix();
    }

    public void translate(float x, float y) {
        GL11.glTranslatef(x, y, 0f);
    }

    public void scale(float s) {
        GL11.glScalef(s, s, 1f);
    }

    private int tint(int argb) {
        return Argb.mulAlpha(argb, alpha.peek().floatValue());
    }

    private static void gradient(float x, float y, float w, float h, int a, int b, boolean horizontal) {
        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glBegin(GL11.GL_QUADS);
        color(a);
        GL11.glVertex2d(x, y);
        if (horizontal) {
            color(a);
            GL11.glVertex2d(x, y + h);
            color(b);
            GL11.glVertex2d(x + w, y + h);
            color(b);
            GL11.glVertex2d(x + w, y);
        } else {
            color(b);
            GL11.glVertex2d(x, y + h);
            color(b);
            GL11.glVertex2d(x + w, y + h);
            color(a);
            GL11.glVertex2d(x + w, y);
        }
        GL11.glEnd();
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
    }

    private static void color(int argb) {
        GL11.glColor4f(Argb.red(argb) / 255f, Argb.green(argb) / 255f, Argb.blue(argb) / 255f, Argb.alpha(argb) / 255f);
    }
}

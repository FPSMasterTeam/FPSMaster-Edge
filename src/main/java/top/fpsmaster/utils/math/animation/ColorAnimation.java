package top.fpsmaster.utils.math.animation;

import top.fpsmaster.utils.render.Render2DUtils;

import java.awt.*;

public class ColorAnimation {
    private final Animation r = new Animation();
    private final Animation g = new Animation();
    private final Animation b = new Animation();
    private final Animation a = new Animation();
    private boolean first = true;
    Color color;
    private Color end;

    public ColorAnimation() {}

    public ColorAnimation(Color color) {
        this.color = color;
    }

    public ColorAnimation(int red, int green, int blue, int alpha) {
        color = new Color(red, green, blue, alpha);
    }

    public void start(Color start, Color end, float duration, Type type) {
        this.end = end;
        r.start(start.getRed(), end.getRed(), duration, type);
        g.start(start.getGreen(), end.getGreen(), duration, type);
        b.start(start.getBlue(), end.getBlue(), duration, type);
        a.start(start.getAlpha(), end.getAlpha(), duration, type);
    }

    public void update() {
        if (end != null) {
            if (first) {
                color = end;
                first = false;
                return;
            }
            r.update();
            g.update();
            b.update();
            a.update();
        }
    }

    public void reset() {
        r.reset();
        g.reset();
        b.reset();
        a.reset();
    }

    public Color getColor() {
        return new Color(
                Render2DUtils.limit(r.value),
        Render2DUtils.limit(g.value),
        Render2DUtils.limit(b.value),
        Render2DUtils.limit(a.value)
        );
    }

    public void setColor(Color color) {
        r.value = color.getRed();
        g.value = color.getGreen();
        b.value = color.getBlue();
        a.value = color.getAlpha();
    }

    public void fstart(Color color, Color color1, float duration, Type type) {
        end = color1;
        r.fstart(color.getRed(), color1.getRed(), duration, type);
        g.fstart(color.getGreen(), color1.getGreen(), duration, type);
        b.fstart(color.getBlue(), color1.getBlue(), duration, type);
        a.fstart(color.getAlpha(), color1.getAlpha(), duration, type);
    }

    public void base(Color color) {
        r.value = AnimationUtils.base(r.value, color.getRed(), 0.1);
        g.value = AnimationUtils.base(g.value, color.getGreen(), 0.1);
        b.value = AnimationUtils.base(b.value, color.getBlue(), 0.1);
        a.value = AnimationUtils.base(a.value, color.getAlpha(), 0.1);
    }
}

package top.fpsmaster.utils.math.anim;

import top.fpsmaster.utils.render.draw.Colors;

import java.awt.*;

public class ColorAnimator {
    private final Animator r = new Animator();
    private final Animator g = new Animator();
    private final Animator b = new Animator();
    private final Animator a = new Animator();

    public void set(Color color) {
        r.set(color.getRed());
        g.set(color.getGreen());
        b.set(color.getBlue());
        a.set(color.getAlpha());
    }

    public void start(Color from, Color to, double durationSec, Easing easing) {
        r.start(from.getRed(), to.getRed(), durationSec, easing);
        g.start(from.getGreen(), to.getGreen(), durationSec, easing);
        b.start(from.getBlue(), to.getBlue(), durationSec, easing);
        a.start(from.getAlpha(), to.getAlpha(), durationSec, easing);
    }

    public void animateTo(Color to, double durationSec, Easing easing) {
        r.animateTo(to.getRed(), durationSec, easing);
        g.animateTo(to.getGreen(), durationSec, easing);
        b.animateTo(to.getBlue(), durationSec, easing);
        a.animateTo(to.getAlpha(), durationSec, easing);
    }

    public void update(double deltaSec) {
        r.update(deltaSec);
        g.update(deltaSec);
        b.update(deltaSec);
        a.update(deltaSec);
    }

    public Color get() {
        return new Color(
                Colors.clamp(r.get()),
                Colors.clamp(g.get()),
                Colors.clamp(b.get()),
                Colors.clamp(a.get())
        );
    }
}

package top.fpsmaster.utils.math.anim;

public final class AnimClock {
    private long lastNanos = System.nanoTime();

    public double tick() {
        long now = System.nanoTime();
        long delta = now - lastNanos;
        lastNanos = now;
        double seconds = delta / 1_000_000_000.0;
        if (seconds < 0) return 0;
        if (seconds > 0.1) return 0.1;
        return seconds;
    }

    public void reset() {
        lastNanos = System.nanoTime();
    }
}

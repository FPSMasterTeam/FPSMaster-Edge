package top.fpsmaster.utils.math.anim;

public final class Animator {
    private double start;
    private double end;
    private double value;
    private double durationSec;
    private double elapsedSec;
    private Easing easing = Easings.LINEAR;
    private boolean running;
    private boolean paused;

    public void start(double from, double to, double durationSec, Easing easing) {
        this.start = from;
        this.end = to;
        this.value = from;
        this.durationSec = Math.max(0, durationSec);
        this.elapsedSec = 0;
        this.easing = easing == null ? Easings.LINEAR : easing;
        this.running = true;
        this.paused = false;
        if (this.durationSec == 0) {
            this.value = this.end;
            this.running = false;
        }
    }

    public void animateTo(double target, double durationSec, Easing easing) {
        Easing use = easing == null ? Easings.LINEAR : easing;
        double d = Math.max(0, durationSec);
        if (running) {
            if (approx(end, target) && approx(this.durationSec, d) && this.easing == use) {
                return;
            }
        } else if (approx(value, target)) {
            return;
        }
        start(value, target, d, use);
    }

    public void update(double deltaSec) {
        if (!running || paused) return;
        if (durationSec <= 0) {
            value = end;
            running = false;
            return;
        }
        elapsedSec += Math.max(0, deltaSec);
        double t = elapsedSec / durationSec;
        if (t >= 1) {
            value = end;
            running = false;
            return;
        }
        double eased = easing.ease(t);
        value = start + (end - start) * eased;
    }

    public void finish() {
        value = end;
        running = false;
    }

    public void cancel() {
        running = false;
    }

    public void pause() {
        paused = true;
    }

    public void resume() {
        paused = false;
    }

    public boolean isRunning() {
        return running;
    }

    public boolean isFinished() {
        return !running && value == end;
    }

    public double progress() {
        if (durationSec <= 0) return 1;
        return Math.min(1.0, elapsedSec / durationSec);
    }

    public double get() {
        return value;
    }

    public void set(double value) {
        this.value = value;
    }

    private static boolean approx(double a, double b) {
        return Math.abs(a - b) < 1e-6;
    }
}

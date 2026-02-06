package top.fpsmaster.utils.math.animation;

public class Animation {
    private long duration = 0;
    private long startTime = 0;
    private double start = 0.0;
    public double value = 0.0;
    public double end = 0.0;
    private Type type = Type.LINEAR;
    private boolean isStarted = false;

    public void start(double start, double end, float duration, Type type) {
        if (!isStarted) {
            if (start != this.start || end != this.end || (long) (duration * 1000) != this.duration || type != this.type) {
                this.duration = (long) (duration * 1000);
                this.start = start;
                this.startTime = System.currentTimeMillis();
                this.value = start;
                this.end = end;
                this.type = type;
                this.isStarted = true;
            }
        }
    }

    public void update() {
        if (!isStarted) return;
        double result = 0.0;
        long elapsedTime = System.currentTimeMillis() - startTime;

        switch (type) {
            case LINEAR:
                result = AnimationUtils.linear(startTime, duration, start, end);
                break;
            case EASE_IN_QUAD:
                result = AnimationUtils.easeInQuad(startTime, duration, start, end);
                break;
            case EASE_OUT_QUAD:
                result = AnimationUtils.easeOutQuad(startTime, duration, start, end);
                break;
            case EASE_IN_OUT_QUAD:
                result = AnimationUtils.easeInOutQuad(startTime, duration, start, end);
                break;
            case EASE_IN_ELASTIC:
                result = AnimationUtils.easeInElastic(elapsedTime, start, end - start, (double) duration);
                break;
            case EASE_OUT_ELASTIC:
                result = AnimationUtils.easeOutElastic(elapsedTime, start, end - start, (double) duration);
                break;
            case EASE_IN_OUT_ELASTIC:
                result = AnimationUtils.easeInOutElastic(elapsedTime, start, end - start, (double) duration);
                break;
            case EASE_IN_BACK:
                result = AnimationUtils.easeInBack(elapsedTime, start, end - start, (double) duration);
                break;
            case EASE_OUT_BACK:
                result = AnimationUtils.easeOutBack(elapsedTime, start, end - start, (double) duration);
                break;
            case EASE_OUT_QUINT:
                result = AnimationUtils.easeOutQuint(elapsedTime, start, end - start, (double) duration);
            default:
                break;
        }

        value = result;

        if (System.currentTimeMillis() - startTime > duration) {
            isStarted = false;
            value = end;
        }
    }

    public void reset() {
        value = 0.0;
        start = 0.0;
        end = 0.0;
        startTime = System.currentTimeMillis();
        isStarted = false;
    }

    public void fstart(double start, double end, float duration, Type type) {
        isStarted = false;
        start(start, end, duration, type);
    }
    public boolean isFinished() {
        return this.value == this.end;
    }
}



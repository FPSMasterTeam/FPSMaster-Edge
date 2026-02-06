package top.fpsmaster.utils.math;

public class MathTimer {
    private long start;

    public MathTimer() {
        start = System.currentTimeMillis();
    }

    public boolean delay(long delay) {
        if (System.currentTimeMillis() - start > delay) {
            reset();
            return true;
        }
        return false;
    }

    public void reset() {
        start = System.currentTimeMillis();
    }
}



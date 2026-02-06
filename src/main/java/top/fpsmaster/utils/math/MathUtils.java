package top.fpsmaster.utils.math;

public class MathUtils {

    public static int clamp(int cursorPosition, int min, int max) {
        return Math.max(min, Math.min(max, cursorPosition));
    }

    public static float increasedSpeed(float current, float start, float target, float speed) {
        if (start == target) {
            return start;
        } else if (start < target) {
            if (current < start) return start;
            if (current > target) return target;
        } else {
            if (current > start) return start;
            if (current < target) return target;
        }
        float k = speed / (target - start);
        return current + (k * (current - start) + speed) * (start > target ? -1 : 1);
    }

    public static float decreasedSpeed(float current, float start, float target, float speed) {
        float k = speed / (start - target);
        return current + (k * (current - start) + speed) * (start > target ? -1 : 1);
    }
}



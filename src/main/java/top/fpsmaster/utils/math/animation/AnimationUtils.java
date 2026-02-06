package top.fpsmaster.utils.math.animation;

import net.minecraft.client.Minecraft;
import top.fpsmaster.utils.Utility;

public class AnimationUtils extends Utility {

    private static float debugFPS() {
        return Minecraft.getDebugFPS() <= 5 ? 60f : (float) Minecraft.getDebugFPS();
    }

    public static double base(double current, double target, double speed) {
        return Double.isNaN(current + (target - current) * speed / (debugFPS() / 60)) ? 0.0 : current + (target - current) * speed / (debugFPS() / 60);
    }

    public static double linear(long startTime, long duration, double start, double end) {
        return (end - start) * ((System.currentTimeMillis() - startTime) * 1.0 / duration) + start;
    }

    public static double easeInQuad(long startTime, long duration, double start, double end) {
        return (end - start) * Math.pow((System.currentTimeMillis() - startTime) * 1.0 / duration, 2.0) + start;
    }

    public static double easeOutQuad(long startTime, long duration, double start, double end) {
        float x = (System.currentTimeMillis() - startTime) * 1.0f / duration;
        float y = -x * x + 2 * x;
        return start + (end - start) * y;
    }

    public static double easeInOutQuad(long startTime, long duration, double start, double end) {
        float t = (System.currentTimeMillis() - startTime) * 1.0f / duration;
        t *= 2f;
        if (t < 1) {
            return (end - start) / 2 * t * t + start;
        } else {
            t--;
            return -(end - start) / 2 * (t * (t - 2) - 1) + start;
        }
    }

    public static double easeInElastic(double t, double b, double c, double d) {
        double s = 1.70158;
        double p = 0.0;
        double a = c;

        if (t == 0.0) return b;
        t /= d;
        if (t == 1.0) return b + c;

        p = d * 0.3;
        if (a < Math.abs(c)) {
            a = c;
            s = p / 4.0;
        } else {
            s = p / (2 * Math.PI) * Math.asin(c / a);
        }
        t--;
        return -(a * Math.pow(2.0, 10 * t) * Math.sin((t * d - s) * (2 * Math.PI) / p)) + b;
    }

    public static double easeOutElastic(double t, double b, double c, double d) {
        double s = 1.70158;
        double p = 0.0;
        double a = c;

        if (t == 0.0) return b;
        t /= d;
        if (t == 1.0) return b + c;

        p = d * 0.3;
        if (a < Math.abs(c)) {
            a = c;
            s = p / 4.0;
        } else {
            s = p / (2 * Math.PI) * Math.asin(c / a);
        }
        return a * Math.pow(2.0, -10 * t) * Math.sin((t * d - s) * (2 * Math.PI) / p) + c + b;
    }

    public static double easeInOutElastic(double t, double b, double c, double d) {
        double s = 1.70158;
        double p = 0.0;
        double a = c;

        if (t == 0.0) return b;
        t /= d / 2;
        if (t == 2.0) return b + c;

        p = d * (0.3 * 1.5);
        if (a < Math.abs(c)) {
            a = c;
            s = p / 4.0;
        } else {
            s = p / (2 * Math.PI) * Math.asin(c / a);
        }
        if (t < 1) {
            t--;
            return -0.5 * (a * Math.pow(2.0, 10 * t) * Math.sin((t * d - s) * (2 * Math.PI) / p)) + b;
        } else {
            t--;
            return a * Math.pow(2.0, -10 * t) * Math.sin((t * d - s) * (2 * Math.PI) / p) * 0.5 + c + b;
        }
    }

    public static double easeInBack(double t, double b, double c, double d) {
        double s = 1.70158;
        t /= d;
        return c * t * t * ((s + 1) * t - s) + b;
    }

    public static double easeOutBack(double t, double b, double c, double d) {
        double s = 1.70158;
        t = t / d - 1;
        return c * (t * t * ((s + 1) * t + s) + 1) + b;
    }

    public static double easeOutQuint(long elapsedTime, double start, double rest, double duration) {
        double percent = 1.0 - Math.pow(1.0 - elapsedTime / duration, 5);
        return start + (rest * percent);
    }
}

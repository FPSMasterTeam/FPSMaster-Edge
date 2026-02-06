package top.fpsmaster.utils.math.anim;

public final class BezierEasing implements Easing {
    private final double x1;
    private final double y1;
    private final double x2;
    private final double y2;

    private BezierEasing(double x1, double y1, double x2, double y2) {
        this.x1 = x1;
        this.y1 = y1;
        this.x2 = x2;
        this.y2 = y2;
    }

    public static BezierEasing of(double x1, double y1, double x2, double y2) {
        return new BezierEasing(x1, y1, x2, y2);
    }

    @Override
    public double ease(double t) {
        if (t <= 0) return 0;
        if (t >= 1) return 1;
        double x = t;
        double s = solveForX(x);
        return sampleCurveY(s);
    }

    private double solveForX(double x) {
        double s = x;
        for (int i = 0; i < 8; i++) {
            double xEstimate = sampleCurveX(s) - x;
            double d = sampleCurveDerivativeX(s);
            if (Math.abs(xEstimate) < 1e-6) {
                return s;
            }
            if (Math.abs(d) < 1e-6) {
                break;
            }
            s = s - xEstimate / d;
        }
        double lo = 0;
        double hi = 1;
        s = x;
        for (int i = 0; i < 12; i++) {
            double xEstimate = sampleCurveX(s);
            if (Math.abs(xEstimate - x) < 1e-6) {
                return s;
            }
            if (xEstimate < x) {
                lo = s;
            } else {
                hi = s;
            }
            s = (lo + hi) * 0.5;
        }
        return s;
    }

    private double sampleCurveX(double t) {
        double inv = 1 - t;
        return 3 * inv * inv * t * x1
                + 3 * inv * t * t * x2
                + t * t * t;
    }

    private double sampleCurveY(double t) {
        double inv = 1 - t;
        return 3 * inv * inv * t * y1
                + 3 * inv * t * t * y2
                + t * t * t;
    }

    private double sampleCurveDerivativeX(double t) {
        double inv = 1 - t;
        return 3 * inv * inv * x1
                + 6 * inv * t * (x2 - x1)
                + 3 * t * t * (1 - x2);
    }
}

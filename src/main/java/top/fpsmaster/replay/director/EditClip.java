package top.fpsmaster.replay.director;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * One clip on an edit project's timeline: a range of the source replay, played at a speed.
 *
 * <p>Clips are ordered in <em>output</em> time, so the same source moment can appear twice and
 * later source can play before earlier source. That is the difference from
 * {@link TimelineSegment}, which partitions the recording in source order.
 *
 * <p>{@link #curve} is optional. When null the clip plays at the constant {@link #speed}. When
 * present, speed varies along the source range; output length is the integral of
 * {@code d(source) / speed}.
 */
public final class EditClip {
    public static final float SPEED_MIN = 0.25f;
    public static final float SPEED_MAX = 8f;

    public String name = "";
    public int srcIn;
    public int srcOut;
    public float speed = 1f;
    public List<SpeedPoint> curve;

    public EditClip() {
    }

    public EditClip(int srcIn, int srcOut) {
        this.srcIn = srcIn;
        this.srcOut = srcOut;
    }

    public EditClip copy() {
        EditClip copy = new EditClip(srcIn, srcOut);
        copy.name = name;
        copy.speed = speed;
        if (curve != null) {
            copy.curve = new ArrayList<SpeedPoint>(curve.size());
            for (SpeedPoint point : curve) {
                copy.curve.add(point.copy());
            }
        }
        return copy;
    }

    public int sourceLength() {
        return Math.max(0, srcOut - srcIn);
    }

    public float clampedSpeed() {
        return clampSpeed(speed);
    }

    public boolean hasCurve() {
        return curve != null && curve.size() >= 2;
    }

    public void clearCurve() {
        curve = null;
    }

    public void enableCurve() {
        if (hasCurve()) {
            return;
        }
        float s = clampedSpeed();
        curve = new ArrayList<SpeedPoint>(2);
        SpeedPoint a = new SpeedPoint(0f, s);
        SpeedPoint b = new SpeedPoint(1f, s);
        a.outDx = 0.33f;
        a.outDy = 0f;
        b.inDx = -0.33f;
        b.inDy = 0f;
        curve.add(a);
        curve.add(b);
    }

    public void sortCurve() {
        if (curve == null) {
            return;
        }
        Collections.sort(curve, new Comparator<SpeedPoint>() {
            @Override
            public int compare(SpeedPoint a, SpeedPoint b) {
                return Float.compare(a.p, b.p);
            }
        });
        if (!curve.isEmpty()) {
            curve.get(0).p = 0f;
            curve.get(curve.size() - 1).p = 1f;
        }
    }

    public SpeedPoint addCurvePoint(float p, float s) {
        enableCurve();
        p = clamp01(p);
        s = clampSpeed(s);
        for (SpeedPoint existing : curve) {
            if (Math.abs(existing.p - p) < 0.03f) {
                existing.s = s;
                return existing;
            }
        }
        SpeedPoint point = new SpeedPoint(p, s);
        curve.add(point);
        sortCurve();
        return point;
    }

    public void removeCurvePoint(int index) {
        if (!hasCurve() || index <= 0 || index >= curve.size() - 1) {
            return;
        }
        curve.remove(index);
        if (curve.size() < 2) {
            curve = null;
        }
    }

    /** Instantaneous rate at a source millisecond. */
    public float speedAtSource(int sourceMillis) {
        int length = sourceLength();
        if (length <= 0) {
            return clampedSpeed();
        }
        float u = (sourceMillis - srcIn) / (float) length;
        return speedAt(u);
    }

    public float speedAt(float u) {
        u = clamp01(u);
        if (!hasCurve()) {
            return clampedSpeed();
        }
        int i = 0;
        while (i < curve.size() - 1 && curve.get(i + 1).p < u) {
            i++;
        }
        SpeedPoint a = curve.get(i);
        SpeedPoint b = curve.get(Math.min(i + 1, curve.size() - 1));
        if (b.p <= a.p + 1e-5f) {
            return clampSpeed(a.s);
        }
        float t = tForX(u, a.p, a.p + a.outDx, b.p + b.inDx, b.p);
        float y = bezier(t, a.s, a.s + a.outDy, b.s + b.inDy, b.s);
        return clampSpeed(y);
    }

    /** Length this clip occupies in the output, after the speed stretch. */
    public long outputLength() {
        int length = sourceLength();
        if (length <= 0) {
            return 0L;
        }
        if (!hasCurve()) {
            return (long) (length / clampedSpeed());
        }
        return Math.max(1L, (long) (length * integralInvSpeed(0f, 1f)));
    }

    /** Source milliseconds from the clip in-point for a local output time. */
    public int sourceOffsetForOutput(long localOut) {
        int length = sourceLength();
        if (length <= 0) {
            return 0;
        }
        if (!hasCurve()) {
            return (int) (localOut * clampedSpeed());
        }
        long total = outputLength();
        if (total <= 0) {
            return 0;
        }
        float target = Math.max(0f, Math.min(1f, localOut / (float) total));
        float lo = 0f;
        float hi = 1f;
        for (int n = 0; n < 18; n++) {
            float mid = (lo + hi) * 0.5f;
            float got = (float) (integralInvSpeed(0f, mid) / integralInvSpeed(0f, 1f));
            if (got < target) {
                lo = mid;
            } else {
                hi = mid;
            }
        }
        return (int) (((lo + hi) * 0.5f) * length);
    }

    /** Output milliseconds from clip start for a source offset. */
    public long outputOffsetForSource(int sourceOffset) {
        int length = sourceLength();
        if (length <= 0) {
            return 0L;
        }
        float u = Math.max(0f, Math.min(1f, sourceOffset / (float) length));
        if (!hasCurve()) {
            return (long) (sourceOffset / clampedSpeed());
        }
        return (long) (length * integralInvSpeed(0f, u));
    }

    /**
     * Splits this clip's curve at source fraction {@code pCut}; {@code tail} receives the right
     * half. Uniform-speed clips have nothing to split.
     */
    public void splitCurveAt(float pCut, EditClip tail) {
        if (!hasCurve() || tail == null) {
            return;
        }
        pCut = clamp01(pCut);
        if (pCut < 0.04f || pCut > 0.96f) {
            return;
        }
        float sCut = speedAt(pCut);
        List<SpeedPoint> left = new ArrayList<SpeedPoint>();
        List<SpeedPoint> right = new ArrayList<SpeedPoint>();
        for (SpeedPoint point : curve) {
            if (point.p <= pCut + 1e-4f) {
                SpeedPoint copy = point.copy();
                copy.p = pCut <= 1e-4f ? 0f : copy.p / pCut;
                copy.inDx /= pCut;
                copy.outDx /= pCut;
                left.add(copy);
            }
            if (point.p >= pCut - 1e-4f) {
                SpeedPoint copy = point.copy();
                float span = 1f - pCut;
                copy.p = span <= 1e-4f ? 1f : (copy.p - pCut) / span;
                copy.inDx /= span;
                copy.outDx /= span;
                right.add(copy);
            }
        }
        if (left.isEmpty() || left.get(left.size() - 1).p < 0.999f) {
            left.add(new SpeedPoint(1f, sCut));
        }
        left.get(0).p = 0f;
        left.get(left.size() - 1).p = 1f;
        if (right.isEmpty() || right.get(0).p > 0.001f) {
            right.add(0, new SpeedPoint(0f, sCut));
        }
        right.get(0).p = 0f;
        right.get(right.size() - 1).p = 1f;
        curve = left;
        tail.curve = right;
        speed = sCut;
        tail.speed = sCut;
    }

    public static float clampSpeed(float s) {
        if (s < SPEED_MIN) {
            return SPEED_MIN;
        }
        if (s > SPEED_MAX) {
            return SPEED_MAX;
        }
        return s;
    }

    private static float clamp01(float t) {
        if (t < 0f) {
            return 0f;
        }
        if (t > 1f) {
            return 1f;
        }
        return t;
    }

    /** ∫ dp / speed(p) over [u0, u1]. */
    private double integralInvSpeed(float u0, float u1) {
        if (u1 <= u0) {
            return 0;
        }
        int steps = 24;
        double acc = 0;
        float last = u0;
        float lastInv = 1.0f / Math.max(0.05f, speedAt(u0));
        for (int i = 1; i <= steps; i++) {
            float u = u0 + (u1 - u0) * (i / (float) steps);
            float inv = 1.0f / Math.max(0.05f, speedAt(u));
            acc += (u - last) * (lastInv + inv) * 0.5;
            last = u;
            lastInv = inv;
        }
        return acc;
    }

    private static float bezier(float t, float a, float b, float c, float d) {
        float inv = 1f - t;
        return inv * inv * inv * a
                + 3f * inv * inv * t * b
                + 3f * inv * t * t * c
                + t * t * t * d;
    }

    private static float tForX(float x, float x0, float x1, float x2, float x3) {
        if (x <= x0) {
            return 0f;
        }
        if (x >= x3) {
            return 1f;
        }
        x1 = clampTo(x1, x0, x3);
        x2 = clampTo(x2, x0, x3);
        float s = (x - x0) / Math.max(1e-5f, x3 - x0);
        for (int i = 0; i < 8; i++) {
            float estimate = bezier(s, x0, x1, x2, x3) - x;
            float d = bezierDerivative(s, x0, x1, x2, x3);
            if (Math.abs(estimate) < 1e-5f) {
                return clamp01(s);
            }
            if (Math.abs(d) < 1e-5f) {
                break;
            }
            s -= estimate / d;
        }
        float lo = 0f;
        float hi = 1f;
        s = clamp01(s);
        for (int i = 0; i < 12; i++) {
            float at = bezier(s, x0, x1, x2, x3);
            if (at < x) {
                lo = s;
            } else {
                hi = s;
            }
            s = (lo + hi) * 0.5f;
        }
        return s;
    }

    private static float bezierDerivative(float t, float a, float b, float c, float d) {
        float inv = 1f - t;
        return 3f * inv * inv * (b - a)
                + 6f * inv * t * (c - b)
                + 3f * t * t * (d - c);
    }

    private static float clampTo(float v, float lo, float hi) {
        if (v < lo) {
            return lo;
        }
        if (v > hi) {
            return hi;
        }
        return v;
    }
}

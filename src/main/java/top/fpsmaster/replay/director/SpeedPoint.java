package top.fpsmaster.replay.director;

/**
 * One knot on a clip's speed curve. {@link #p} is 0–1 along the clip's source range;
 * {@link #s} is the playback rate at that knot. Incoming / outgoing handles are offsets in
 * the same {@code (p, s)} space so a cubic Bézier between neighbouring knots can be dragged
 * like After Effects.
 */
public final class SpeedPoint {
    public float p;
    public float s = 1f;
    public float inDx = -0.08f;
    public float inDy = 0f;
    public float outDx = 0.08f;
    public float outDy = 0f;

    public SpeedPoint() {
    }

    public SpeedPoint(float p, float s) {
        this.p = p;
        this.s = s;
    }

    public SpeedPoint copy() {
        SpeedPoint copy = new SpeedPoint(p, s);
        copy.inDx = inDx;
        copy.inDy = inDy;
        copy.outDx = outDx;
        copy.outDy = outDy;
        return copy;
    }
}

package top.fpsmaster.utils.render.types;

/** Minimal 4-vector for cape vertex transforms (replaces {@code javax.vecmath.Vector4f}). */
public final class Vector4f {
    public float x;
    public float y;
    public float z;
    public float w;

    public Vector4f() {
    }

    public Vector4f(float x, float y, float z, float w) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.w = w;
    }
}

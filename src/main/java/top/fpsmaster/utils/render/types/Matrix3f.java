package top.fpsmaster.utils.render.types;

/**
 * Minimal 3x3 matrix used by {@link PoseStack} for wavy-cape normals.
 * Kept in-tree so Forge-free AOT does not need {@code java3d:vecmath}.
 */
public final class Matrix3f {
    public float m00, m01, m02;
    public float m10, m11, m12;
    public float m20, m21, m22;

    public Matrix3f() {
    }

    public Matrix3f(Matrix3f other) {
        set(other);
    }

    public void setIdentity() {
        m00 = 1.0F;
        m01 = 0.0F;
        m02 = 0.0F;
        m10 = 0.0F;
        m11 = 1.0F;
        m12 = 0.0F;
        m20 = 0.0F;
        m21 = 0.0F;
        m22 = 1.0F;
    }

    public void set(Matrix3f other) {
        m00 = other.m00;
        m01 = other.m01;
        m02 = other.m02;
        m10 = other.m10;
        m11 = other.m11;
        m12 = other.m12;
        m20 = other.m20;
        m21 = other.m21;
        m22 = other.m22;
    }

    /** {@code this = this * right} (row-vector / javax.vecmath convention). */
    public void mul(Matrix3f right) {
        float n00 = m00 * right.m00 + m01 * right.m10 + m02 * right.m20;
        float n01 = m00 * right.m01 + m01 * right.m11 + m02 * right.m21;
        float n02 = m00 * right.m02 + m01 * right.m12 + m02 * right.m22;
        float n10 = m10 * right.m00 + m11 * right.m10 + m12 * right.m20;
        float n11 = m10 * right.m01 + m11 * right.m11 + m12 * right.m21;
        float n12 = m10 * right.m02 + m11 * right.m12 + m12 * right.m22;
        float n20 = m20 * right.m00 + m21 * right.m10 + m22 * right.m20;
        float n21 = m20 * right.m01 + m21 * right.m11 + m22 * right.m21;
        float n22 = m20 * right.m02 + m21 * right.m12 + m22 * right.m22;
        m00 = n00;
        m01 = n01;
        m02 = n02;
        m10 = n10;
        m11 = n11;
        m12 = n12;
        m20 = n20;
        m21 = n21;
        m22 = n22;
    }

    public void mul(float scalar) {
        m00 *= scalar;
        m01 *= scalar;
        m02 *= scalar;
        m10 *= scalar;
        m11 *= scalar;
        m12 *= scalar;
        m20 *= scalar;
        m21 *= scalar;
        m22 *= scalar;
    }
}

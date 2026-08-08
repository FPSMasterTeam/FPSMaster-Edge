package top.fpsmaster.utils.render.types;

/**
 * Minimal 4x4 matrix used by {@link PoseStack} / wavy cape.
 * Kept in-tree so Forge-free AOT does not need {@code java3d:vecmath}.
 */
public final class Matrix4f {
    public float m00, m01, m02, m03;
    public float m10, m11, m12, m13;
    public float m20, m21, m22, m23;
    public float m30, m31, m32, m33;

    public Matrix4f() {
    }

    public Matrix4f(Matrix4f other) {
        set(other);
    }

    public void setIdentity() {
        m00 = 1.0F;
        m01 = 0.0F;
        m02 = 0.0F;
        m03 = 0.0F;
        m10 = 0.0F;
        m11 = 1.0F;
        m12 = 0.0F;
        m13 = 0.0F;
        m20 = 0.0F;
        m21 = 0.0F;
        m22 = 1.0F;
        m23 = 0.0F;
        m30 = 0.0F;
        m31 = 0.0F;
        m32 = 0.0F;
        m33 = 1.0F;
    }

    public void set(Matrix4f other) {
        m00 = other.m00;
        m01 = other.m01;
        m02 = other.m02;
        m03 = other.m03;
        m10 = other.m10;
        m11 = other.m11;
        m12 = other.m12;
        m13 = other.m13;
        m20 = other.m20;
        m21 = other.m21;
        m22 = other.m22;
        m23 = other.m23;
        m30 = other.m30;
        m31 = other.m31;
        m32 = other.m32;
        m33 = other.m33;
    }

    /** {@code this = this * right} (row-vector / javax.vecmath convention). */
    public void mul(Matrix4f right) {
        float n00 = m00 * right.m00 + m01 * right.m10 + m02 * right.m20 + m03 * right.m30;
        float n01 = m00 * right.m01 + m01 * right.m11 + m02 * right.m21 + m03 * right.m31;
        float n02 = m00 * right.m02 + m01 * right.m12 + m02 * right.m22 + m03 * right.m32;
        float n03 = m00 * right.m03 + m01 * right.m13 + m02 * right.m23 + m03 * right.m33;
        float n10 = m10 * right.m00 + m11 * right.m10 + m12 * right.m20 + m13 * right.m30;
        float n11 = m10 * right.m01 + m11 * right.m11 + m12 * right.m21 + m13 * right.m31;
        float n12 = m10 * right.m02 + m11 * right.m12 + m12 * right.m22 + m13 * right.m32;
        float n13 = m10 * right.m03 + m11 * right.m13 + m12 * right.m23 + m13 * right.m33;
        float n20 = m20 * right.m00 + m21 * right.m10 + m22 * right.m20 + m23 * right.m30;
        float n21 = m20 * right.m01 + m21 * right.m11 + m22 * right.m21 + m23 * right.m31;
        float n22 = m20 * right.m02 + m21 * right.m12 + m22 * right.m22 + m23 * right.m32;
        float n23 = m20 * right.m03 + m21 * right.m13 + m22 * right.m23 + m23 * right.m33;
        float n30 = m30 * right.m00 + m31 * right.m10 + m32 * right.m20 + m33 * right.m30;
        float n31 = m30 * right.m01 + m31 * right.m11 + m32 * right.m21 + m33 * right.m31;
        float n32 = m30 * right.m02 + m31 * right.m12 + m32 * right.m22 + m33 * right.m32;
        float n33 = m30 * right.m03 + m31 * right.m13 + m32 * right.m23 + m33 * right.m33;
        m00 = n00;
        m01 = n01;
        m02 = n02;
        m03 = n03;
        m10 = n10;
        m11 = n11;
        m12 = n12;
        m13 = n13;
        m20 = n20;
        m21 = n21;
        m22 = n22;
        m23 = n23;
        m30 = n30;
        m31 = n31;
        m32 = n32;
        m33 = n33;
    }

    /** Transforms {@code vec} in place: {@code vec = this * vec}. */
    public void transform(Vector4f vec) {
        float x = m00 * vec.x + m01 * vec.y + m02 * vec.z + m03 * vec.w;
        float y = m10 * vec.x + m11 * vec.y + m12 * vec.z + m13 * vec.w;
        float z = m20 * vec.x + m21 * vec.y + m22 * vec.z + m23 * vec.w;
        float w = m30 * vec.x + m31 * vec.y + m32 * vec.z + m33 * vec.w;
        vec.x = x;
        vec.y = y;
        vec.z = z;
        vec.w = w;
    }
}

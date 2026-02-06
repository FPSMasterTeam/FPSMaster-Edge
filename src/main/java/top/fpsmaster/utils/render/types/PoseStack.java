package top.fpsmaster.utils.render.types;

import java.util.Deque;

import com.google.common.collect.Queues;
import org.lwjgl.util.vector.Quaternion;

import javax.vecmath.Matrix3f;
import javax.vecmath.Matrix4f;

public class PoseStack {
    private final Deque<Pose> poseStack;

    public PoseStack() {
        this.poseStack = Queues.newArrayDeque();
        Matrix4f poseMatrix = new Matrix4f();
        poseMatrix.setIdentity();
        Matrix3f normalMatrix = new Matrix3f();
        normalMatrix.setIdentity();
        poseStack.addLast(new Pose(poseMatrix, normalMatrix));
    }

    public void translate(double x, double y, double z) {
        Pose pose = poseStack.getLast();
        multiplyWithTranslation(pose.pose, (float) x, (float) y, (float) z);
    }

    public void multiplyWithTranslation(Matrix4f matrix, float x, float y, float z) {
        matrix.m03 += matrix.m00 * x + matrix.m01 * y + matrix.m02 * z;
        matrix.m13 += matrix.m10 * x + matrix.m11 * y + matrix.m12 * z;
        matrix.m23 += matrix.m20 * x + matrix.m21 * y + matrix.m22 * z;
        matrix.m33 = matrix.m30 * x + matrix.m31 * y + matrix.m32 * z + matrix.m33;
    }

    public void scale(float x, float y, float z) {
        Pose pose = poseStack.getLast();
        pose.pose.mul(createScaleMatrix4f(x, y, z));

        if (x == y && y == z) {
            if (x > 0.0F) return;
            pose.normal.mul(-1.0F); // Uniform negative scale
        }

        float invX = 1.0F / x;
        float invY = 1.0F / y;
        float invZ = 1.0F / z;
        float invDet = fastInvCubeRoot(invX * invY * invZ);
        pose.normal.mul(createScaleMatrix3f(invDet * invX, invDet * invY, invDet * invZ));
    }

    private Matrix3f createScaleMatrix3f(float x, float y, float z) {
        Matrix3f mat = new Matrix3f();
        mat.setIdentity();
        mat.m00 = x;
        mat.m11 = y;
        mat.m22 = z;
        return mat;
    }

    private Matrix4f createScaleMatrix4f(float x, float y, float z) {
        Matrix4f mat = new Matrix4f();
        mat.setIdentity();
        mat.m00 = x;
        mat.m11 = y;
        mat.m22 = z;
        mat.m33 = 1.0F;
        return mat;
    }

    public static float fastInvCubeRoot(float value) {
        int i = Float.floatToIntBits(value);
        i = 1419967116 - i / 3;
        float approx = Float.intBitsToFloat(i);
        approx = 0.6666667F * approx + (1.0F / 3.0F) * approx * approx * value;
        approx = 0.6666667F * approx + (1.0F / 3.0F) * approx * approx * value;
        return approx;
    }

    public void mulPose(Quaternion quaternion) {
        Pose pose = poseStack.getLast();
        pose.pose.mul(fromQuaternion4f(quaternion));
        pose.normal.mul(fromQuaternion3f(quaternion));
    }

    public void pushPose() {
        Pose current = poseStack.getLast();
        poseStack.addLast(new Pose(new Matrix4f(current.pose), new Matrix3f(current.normal)));
    }

    public void popPose() {
        if (poseStack.size() <= 1)
            throw new IllegalStateException("Cannot pop the root pose");
        poseStack.removeLast();
    }

    public Pose last() {
        return poseStack.getLast();
    }

    public boolean clear() {
        return poseStack.size() == 1;
    }

    public Matrix4f fromQuaternion4f(Quaternion q) {
        float[][] m = rotationMatrixFromQuaternion(q);
        Matrix4f mat = new Matrix4f();
        mat.setIdentity();
        mat.m00 = m[0][0]; mat.m01 = m[0][1]; mat.m02 = m[0][2];
        mat.m10 = m[1][0]; mat.m11 = m[1][1]; mat.m12 = m[1][2];
        mat.m20 = m[2][0]; mat.m21 = m[2][1]; mat.m22 = m[2][2];
        return mat;
    }

    public Matrix3f fromQuaternion3f(Quaternion q) {
        float[][] m = rotationMatrixFromQuaternion(q);
        Matrix3f mat = new Matrix3f();
        mat.setIdentity();
        mat.m00 = m[0][0]; mat.m01 = m[0][1]; mat.m02 = m[0][2];
        mat.m10 = m[1][0]; mat.m11 = m[1][1]; mat.m12 = m[1][2];
        mat.m20 = m[2][0]; mat.m21 = m[2][1]; mat.m22 = m[2][2];
        return mat;
    }


    private float[][] rotationMatrixFromQuaternion(Quaternion q) {
        float x = q.x, y = q.y, z = q.z, w = q.w;

        float xx = 2.0F * x * x;
        float yy = 2.0F * y * y;
        float zz = 2.0F * z * z;

        float xy = x * y;
        float yz = y * z;
        float zx = z * x;
        float xw = x * w;
        float yw = y * w;
        float zw = z * w;

        float[][] m = new float[3][3];
        m[0][0] = 1.0F - yy - zz;
        m[1][1] = 1.0F - zz - xx;
        m[2][2] = 1.0F - xx - yy;

        m[1][0] = 2.0F * (xy + zw);
        m[0][1] = 2.0F * (xy - zw);

        m[2][0] = 2.0F * (zx - yw);
        m[0][2] = 2.0F * (zx + yw);

        m[2][1] = 2.0F * (yz + xw);
        m[1][2] = 2.0F * (yz - xw);

        return m;
    }


    public static final class Pose {
        public final Matrix4f pose;
        public final Matrix3f normal;

        Pose(Matrix4f pose, Matrix3f normal) {
            this.pose = pose;
            this.normal = normal;
        }
    }
}




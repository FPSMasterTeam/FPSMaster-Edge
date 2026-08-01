package top.fpsmaster.forge.mixin;

import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.GLAllocation;
import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.fpsmaster.benchmark.BenchCounters;
import top.fpsmaster.benchmark.BenchmarkMode;
import top.fpsmaster.features.impl.optimizes.Performance;

import java.nio.FloatBuffer;
import java.util.List;

/**
 * Replaces a model box's chain of fixed-function transforms with one matrix.
 *
 * <p>Vanilla places each box with a translate for its offset, a translate for its rotation point
 * and up to three rotates, then draws it and undoes the offset — as many as six calls into the
 * fixed-function matrix stack per box, and a thousand boxes a frame on a crowded scene. Each
 * {@code glRotatef} builds a rotation matrix from a sine and a cosine and multiplies it in.
 *
 * <p>Measured before building, because the plan this replaces assumed the opposite. Two ceiling
 * probes on the entity-dense scenario: deleting the {@code callList} took the model bracket from
 * 135us to 99us, and deleting the transforms took it to 62us. <b>The transforms are the larger
 * half by two to one</b>, and the display list — which the standing hypothesis blamed — is the
 * smaller one.
 *
 * <p>So the geometry is left exactly where it is and only the placement changes: the whole chain
 * composes into a single matrix, multiplied in with one call. The matrix is cached on the values
 * it was built from, so a box whose pose has not changed since the last frame — which is every box
 * of every armour stand, and most of a standing player — does not recompute it.
 *
 * <p>Vanilla has three branches here (no rotation and no rotation point, rotation point only, and
 * rotation) which differ in whether they push the matrix stack. One push and pop covers all three:
 * the offset that vanilla undoes by hand at the end, and the rotation point it leaves applied for
 * the children, are both restored by the pop.
 */
@Mixin(ModelRenderer.class)
public abstract class ModelRendererMixin_ComposedTransform {

    @Shadow
    public float offsetX;
    @Shadow
    public float offsetY;
    @Shadow
    public float offsetZ;
    @Shadow
    public float rotationPointX;
    @Shadow
    public float rotationPointY;
    @Shadow
    public float rotationPointZ;
    @Shadow
    public float rotateAngleX;
    @Shadow
    public float rotateAngleY;
    @Shadow
    public float rotateAngleZ;
    @Shadow
    public boolean isHidden;
    @Shadow
    public boolean showModel;
    @Shadow
    public List childModels;
    @Shadow
    private boolean compiled;
    @Shadow
    private int displayList;

    @Shadow
    protected abstract void compileDisplayList(float scale);

    @Unique
    private FloatBuffer edge$matrix;
    @Unique
    private final float[] edge$pose = new float[10];
    @Unique
    private boolean edge$poseValid;

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void edge$renderComposed(float scale, CallbackInfo ci) {
        if (!Performance.using || !Performance.composedModelTransform.getValue()) {
            return;
        }
        if (isHidden || !showModel) {
            ci.cancel();
            return;
        }
        if (!compiled) {
            compileDisplayList(scale);
        }

        GlStateManager.pushMatrix();
        GL11.glMultMatrix(edge$compose(scale));
        GlStateManager.callList(displayList);
        if (childModels != null) {
            for (int i = 0; i < childModels.size(); i++) {
                ((ModelRenderer) childModels.get(i)).render(scale);
            }
        }
        GlStateManager.popMatrix();

        if (BenchmarkMode.ACTIVE) {
            BenchCounters.modelComposedTransforms++;
        }
        ci.cancel();
    }

    /**
     * The box's placement as one column-major matrix, rebuilt only when its pose changes.
     *
     * <p>Composed in vanilla's order — offset, then rotation point, then Z, Y, X — because each
     * {@code glRotatef} and {@code glTranslatef} multiplies on the right, and a matrix built in any
     * other order puts limbs somewhere else.
     */
    @Unique
    private FloatBuffer edge$compose(float scale) {
        if (edge$matrix == null) {
            edge$matrix = GLAllocation.createDirectFloatBuffer(16);
        }
        if (edge$poseValid
                && edge$pose[0] == offsetX && edge$pose[1] == offsetY && edge$pose[2] == offsetZ
                && edge$pose[3] == rotationPointX && edge$pose[4] == rotationPointY
                && edge$pose[5] == rotationPointZ
                && edge$pose[6] == rotateAngleX && edge$pose[7] == rotateAngleY
                && edge$pose[8] == rotateAngleZ && edge$pose[9] == scale) {
            edge$matrix.position(0);
            return edge$matrix;
        }
        edge$pose[0] = offsetX;
        edge$pose[1] = offsetY;
        edge$pose[2] = offsetZ;
        edge$pose[3] = rotationPointX;
        edge$pose[4] = rotationPointY;
        edge$pose[5] = rotationPointZ;
        edge$pose[6] = rotateAngleX;
        edge$pose[7] = rotateAngleY;
        edge$pose[8] = rotateAngleZ;
        edge$pose[9] = scale;
        edge$poseValid = true;

        // Rotation as Rz * Ry * Rx, expanded rather than multiplied out step by step: three 4x4
        // multiplies to produce nine numbers is work this runs often enough to care about.
        float sinX = (float) Math.sin(rotateAngleX);
        float cosX = (float) Math.cos(rotateAngleX);
        float sinY = (float) Math.sin(rotateAngleY);
        float cosY = (float) Math.cos(rotateAngleY);
        float sinZ = (float) Math.sin(rotateAngleZ);
        float cosZ = (float) Math.cos(rotateAngleZ);

        float m00 = cosZ * cosY;
        float m01 = cosZ * sinY * sinX - sinZ * cosX;
        float m02 = cosZ * sinY * cosX + sinZ * sinX;
        float m10 = sinZ * cosY;
        float m11 = sinZ * sinY * sinX + cosZ * cosX;
        float m12 = sinZ * sinY * cosX - cosZ * sinX;
        float m20 = -sinY;
        float m21 = cosY * sinX;
        float m22 = cosY * cosX;

        float translateX = offsetX + rotationPointX * scale;
        float translateY = offsetY + rotationPointY * scale;
        float translateZ = offsetZ + rotationPointZ * scale;

        edge$matrix.clear();
        // Column-major, which is what GL wants: the translation is the last column.
        edge$matrix.put(m00).put(m10).put(m20).put(0f);
        edge$matrix.put(m01).put(m11).put(m21).put(0f);
        edge$matrix.put(m02).put(m12).put(m22).put(0f);
        edge$matrix.put(translateX).put(translateY).put(translateZ).put(1f);
        edge$matrix.flip();
        return edge$matrix;
    }
}

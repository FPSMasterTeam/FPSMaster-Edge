package top.fpsmaster.replay.director;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.util.MathHelper;
import org.lwjgl.opengl.GL11;
import top.fpsmaster.event.EventDispatcher;
import top.fpsmaster.event.Subscribe;
import top.fpsmaster.event.events.EventRender3D;
import top.fpsmaster.forge.api.IRenderManager;
import top.fpsmaster.replay.ReplayPlayer;
import top.fpsmaster.ui.screens.replay.DirectorPanel;

import java.util.List;

/**
 * World-space camera gizmos for the director: a gold path through position keys, a frustum at
 * each key, and a white live marker.
 *
 * <p>Drawn in {@link EventRender3D} with the same Tessellator + unit-0 texture toggle as
 * {@code BlockOverlay}. The lightmap on texture unit 1 is never touched, and {@code glPushAttrib}
 * is not used — both have already desynced GlStateManager and blacked the world / HUD on this
 * client. Depth stays on so a frustum at the view camera cannot fill the screen.
 */
public final class DirectorWorldOverlay {

    private static final DirectorWorldOverlay INSTANCE = new DirectorWorldOverlay();
    private static final int PATH_SAMPLES = 80;
    private static final float GIZMO_DEPTH = 0.55f;
    private static final float GIZMO_HALF_W = 0.22f;
    private static final float GIZMO_HALF_H = 0.14f;
    /** Inside this radius the gizmo is the view camera itself. */
    private static final double NEAR_SKIP = 1.6d;

    private static final float[] FORWARD = new float[3];
    private static final float[] RIGHT = new float[3];
    private static final float[] UP = new float[3];

    private DirectorWorldOverlay() {
    }

    public static void init() {
        EventDispatcher.registerListener(INSTANCE);
    }

    @Subscribe
    public void onRender3D(EventRender3D event) {
        if (DirectorExporter.isRunning()) {
            return;
        }
        ReplayPlayer player = ReplayPlayer.instance();
        if (!player.isActive() || player.isPossessing()) {
            return;
        }
        if (!DirectorPanel.isOpen()) {
            return;
        }
        CameraTrack track = DirectorCamera.track();
        if (track == null || !track.drivesPosition()) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.getRenderManager() == null || mc.getRenderViewEntity() == null) {
            return;
        }
        List<PropKeyframe> keys = track.channel(CameraChannel.POSITION);
        if (keys == null || keys.isEmpty()) {
            return;
        }
        Entity view = mc.getRenderViewEntity();
        IRenderManager rm = (IRenderManager) mc.getRenderManager();
        double rx = rm.renderPosX();
        double ry = rm.renderPosY();
        double rz = rm.renderPosZ();
        float eye = view.getEyeHeight();
        double vx = view.lastTickPosX + (view.posX - view.lastTickPosX) * event.partialTicks;
        double vy = view.lastTickPosY + (view.posY - view.lastTickPosY) * event.partialTicks;
        double vz = view.lastTickPosZ + (view.posZ - view.lastTickPosZ) * event.partialTicks;
        PropKeyframe first = keys.get(0);
        CameraPose hold = new CameraPose(first.a, first.b, first.c, 0f, 0f, 70f, 0f);

        GL11.glPushMatrix();
        GlStateManager.enableAlpha();
        GlStateManager.enableBlend();
        GL11.glBlendFunc(770, 771);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glDepthMask(false);
        GL11.glLineWidth(2.2f);
        try {
            drawPath(track, keys, hold, eye, rx, ry, rz, vx, vy, vz);
            PropKeyframe selected = DirectorPanel.selectedChannel() == CameraChannel.POSITION
                    ? DirectorPanel.selectedKeyframe() : null;
            for (int i = 0; i < keys.size(); i++) {
                PropKeyframe key = keys.get(i);
                CameraPose pose = track.sample(key.timeMillis, hold);
                if (tooClose(pose, vx, vy, vz)) {
                    continue;
                }
                boolean on = key == selected;
                int r = on ? 91 : 242;
                int g = on ? 103 : 210;
                int b = on ? 242 : 122;
                drawFrustum(pose, eye, rx, ry, rz, r, g, b, on ? 230 : 200);
                drawDot(pose, eye, rx, ry, rz, r, g, b, 255);
            }
            CameraPose live = track.sample(player.visualElapsedMillis(), hold);
            if (live != null && !tooClose(live, vx, vy, vz)) {
                drawFrustum(live, eye, rx, ry, rz, 255, 255, 255, 220);
            }
        } finally {
            GL11.glLineWidth(1.0f);
            GL11.glDepthMask(true);
            GL11.glDisable(GL11.GL_LINE_SMOOTH);
            GL11.glEnable(GL11.GL_TEXTURE_2D);
            GlStateManager.disableBlend();
            GlStateManager.color(1f, 1f, 1f, 1f);
            GL11.glPopMatrix();
        }
    }

    private static void drawPath(CameraTrack track, List<PropKeyframe> keys, CameraPose hold,
                                 float eye, double rx, double ry, double rz,
                                 double vx, double vy, double vz) {
        int t0 = keys.get(0).timeMillis;
        int t1 = keys.get(keys.size() - 1).timeMillis;
        if (t1 <= t0) {
            return;
        }
        int samples = Math.min(PATH_SAMPLES, Math.max(8, (t1 - t0) / 40));
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer wr = tessellator.getWorldRenderer();
        GlStateManager.color(242 / 255f, 210 / 255f, 122 / 255f, 180 / 255f);
        wr.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION);
        CameraPose prev = null;
        for (int i = 0; i <= samples; i++) {
            int t = t0 + (int) ((t1 - t0) * (i / (float) samples));
            CameraPose pose = track.sample(t, hold);
            if (prev != null && !tooClose(prev, vx, vy, vz) && !tooClose(pose, vx, vy, vz)) {
                wr.pos(prev.x - rx, prev.y + eye - ry, prev.z - rz).endVertex();
                wr.pos(pose.x - rx, pose.y + eye - ry, pose.z - rz).endVertex();
            }
            prev = pose;
        }
        tessellator.draw();
    }

    private static void drawDot(CameraPose pose, float eye, double rx, double ry, double rz,
                                int r, int g, int b, int a) {
        double x = pose.x - rx;
        double y = pose.y + eye - ry;
        double z = pose.z - rz;
        float s = 0.06f;
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer wr = tessellator.getWorldRenderer();
        GlStateManager.color(r / 255f, g / 255f, b / 255f, a / 255f);
        wr.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION);
        wr.pos(x - s, y, z).endVertex();
        wr.pos(x + s, y, z).endVertex();
        wr.pos(x, y - s, z).endVertex();
        wr.pos(x, y + s, z).endVertex();
        wr.pos(x, y, z - s).endVertex();
        wr.pos(x, y, z + s).endVertex();
        tessellator.draw();
    }

    private static void drawFrustum(CameraPose pose, float eye, double rx, double ry, double rz,
                                    int r, int g, int b, int a) {
        lookBasis(pose.yaw, pose.pitch, pose.roll, FORWARD, RIGHT, UP);
        double ox = pose.x - rx;
        double oy = pose.y + eye - ry;
        double oz = pose.z - rz;
        double cx = ox + FORWARD[0] * GIZMO_DEPTH;
        double cy = oy + FORWARD[1] * GIZMO_DEPTH;
        double cz = oz + FORWARD[2] * GIZMO_DEPTH;
        double[][] corners = new double[4][3];
        int[] sx = {-1, 1, 1, -1};
        int[] sy = {1, 1, -1, -1};
        for (int i = 0; i < 4; i++) {
            corners[i][0] = cx + RIGHT[0] * GIZMO_HALF_W * sx[i] + UP[0] * GIZMO_HALF_H * sy[i];
            corners[i][1] = cy + RIGHT[1] * GIZMO_HALF_W * sx[i] + UP[1] * GIZMO_HALF_H * sy[i];
            corners[i][2] = cz + RIGHT[2] * GIZMO_HALF_W * sx[i] + UP[2] * GIZMO_HALF_H * sy[i];
        }
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer wr = tessellator.getWorldRenderer();
        GlStateManager.color(r / 255f, g / 255f, b / 255f, a / 255f);
        wr.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION);
        for (int i = 0; i < 4; i++) {
            wr.pos(ox, oy, oz).endVertex();
            wr.pos(corners[i][0], corners[i][1], corners[i][2]).endVertex();
            wr.pos(corners[i][0], corners[i][1], corners[i][2]).endVertex();
            int n = (i + 1) % 4;
            wr.pos(corners[n][0], corners[n][1], corners[n][2]).endVertex();
        }
        tessellator.draw();
    }

    private static boolean tooClose(CameraPose pose, double vx, double vy, double vz) {
        if (pose == null) {
            return true;
        }
        double dx = pose.x - vx;
        double dy = pose.y - vy;
        double dz = pose.z - vz;
        return dx * dx + dy * dy + dz * dz < NEAR_SKIP * NEAR_SKIP;
    }

    /**
     * Minecraft look basis: yaw 0 faces +Z (south). {@code forward} is where the camera looks,
     * {@code right} and {@code up} include roll.
     */
    static void lookBasis(float yaw, float pitch, float roll, float[] forward, float[] right, float[] up) {
        float yawR = -yaw * 0.017453292F - (float) Math.PI;
        float pitchR = -pitch * 0.017453292F;
        float cy = MathHelper.cos(yawR);
        float sy = MathHelper.sin(yawR);
        float cp = -MathHelper.cos(pitchR);
        float sp = MathHelper.sin(pitchR);
        forward[0] = sy * cp;
        forward[1] = sp;
        forward[2] = cy * cp;
        float[] worldUp = {0f, 1f, 0f};
        cross(forward, worldUp, right);
        if (lengthSq(right) < 1.0E-6f) {
            right[0] = 1f;
            right[1] = 0f;
            right[2] = 0f;
        } else {
            normalize(right);
        }
        cross(right, forward, up);
        normalize(up);
        if (roll != 0f) {
            float rollR = roll * 0.017453292F;
            float cr = MathHelper.cos(rollR);
            float sr = MathHelper.sin(rollR);
            float rx = right[0] * cr + up[0] * sr;
            float ry = right[1] * cr + up[1] * sr;
            float rz = right[2] * cr + up[2] * sr;
            float ux = up[0] * cr - right[0] * sr;
            float uy = up[1] * cr - right[1] * sr;
            float uz = up[2] * cr - right[2] * sr;
            right[0] = rx;
            right[1] = ry;
            right[2] = rz;
            up[0] = ux;
            up[1] = uy;
            up[2] = uz;
        }
    }

    private static void cross(float[] a, float[] b, float[] out) {
        out[0] = a[1] * b[2] - a[2] * b[1];
        out[1] = a[2] * b[0] - a[0] * b[2];
        out[2] = a[0] * b[1] - a[1] * b[0];
    }

    private static float lengthSq(float[] v) {
        return v[0] * v[0] + v[1] * v[1] + v[2] * v[2];
    }

    private static void normalize(float[] v) {
        float len = (float) Math.sqrt(lengthSq(v));
        if (len < 1.0E-6f) {
            return;
        }
        v[0] /= len;
        v[1] /= len;
        v[2] /= len;
    }
}

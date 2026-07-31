package top.fpsmaster.benchmark;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.client.entity.EntityPlayerSP;

import java.util.ArrayList;
import java.util.List;

/**
 * Deterministic scripted camera for a benchmark scenario.
 *
 * <p>The path is parameterised by wall-clock time and loops, so warmup and measurement traverse the
 * same geometry and a faster build renders more frames of the same workload rather than covering
 * the workload faster. Parameterising by frame index instead would mean the two sides of an A/B
 * were not looking at the same thing.
 *
 * <p>The player is repositioned every frame with {@code prev} coordinates pinned to the current
 * ones, so the renderer's partial-tick interpolation cannot reintroduce the 20 Hz quantisation that
 * makes tick-recorded camera paths unusable for frame-time analysis.
 */
public final class BenchCamera {

    private static final class Keyframe {
        final long timeMillis;
        final double x;
        final double y;
        final double z;
        final float yaw;
        final float pitch;

        Keyframe(long timeMillis, double x, double y, double z, float yaw, float pitch) {
            this.timeMillis = timeMillis;
            this.x = x;
            this.y = y;
            this.z = z;
            this.yaw = yaw;
            this.pitch = pitch;
        }
    }

    private final List<Keyframe> keyframes = new ArrayList<Keyframe>();
    private final long loopMillis;

    private BenchCamera(List<Keyframe> keyframes, long loopMillis, boolean walk) {
        this.keyframes.addAll(keyframes);
        this.loopMillis = loopMillis;
        this.walk = walk;
    }

    public static BenchCamera parse(JsonObject json) {
        if (json == null) {
            return null;
        }
        List<Keyframe> frames = new ArrayList<Keyframe>();
        JsonArray array = json.getAsJsonArray("keyframes");
        for (JsonElement element : array) {
            JsonObject frame = element.getAsJsonObject();
            frames.add(new Keyframe(
                    frame.get("t").getAsLong(),
                    frame.get("x").getAsDouble(),
                    frame.get("y").getAsDouble(),
                    frame.get("z").getAsDouble(),
                    frame.get("yaw").getAsFloat(),
                    frame.get("pitch").getAsFloat()));
        }
        if (frames.isEmpty()) {
            throw new IllegalArgumentException("camera has no keyframes");
        }
        long loop = json.has("loopMillis")
                ? json.get("loopMillis").getAsLong()
                : frames.get(frames.size() - 1).timeMillis;
        boolean walk = json.has("walk") && json.get("walk").getAsBoolean();
        return new BenchCamera(frames, Math.max(loop, 1L), walk);
    }

    /**
     * Whether the path is walked rather than teleported along.
     *
     * <p>Teleporting with {@code noClip} is what makes a camera path exactly reproducible, and it is
     * the right default — but it also means the player never calls {@code moveEntity}, so nothing in
     * a scenario built that way exercises block collision at all. That is not a small omission: it is
     * the half of collision that runs on every moving entity every tick, and no scenario here could
     * see it. A walked path gives it up in exchange: the player is steered toward the same keyframes
     * under real physics, which lands close to the path rather than on it.
     */
    private final boolean walk;

    /** Returns the position the camera should occupy at {@code elapsedMillis} into the path. */
    public void apply(EntityPlayerSP player, long elapsedMillis) {
        long t = keyframes.size() == 1 ? 0L : elapsedMillis % loopMillis;

        Keyframe from = keyframes.get(0);
        Keyframe to = keyframes.get(keyframes.size() - 1);
        for (int i = 0; i < keyframes.size() - 1; i++) {
            if (t >= keyframes.get(i).timeMillis && t <= keyframes.get(i + 1).timeMillis) {
                from = keyframes.get(i);
                to = keyframes.get(i + 1);
                break;
            }
        }

        double span = to.timeMillis - from.timeMillis;
        double alpha = span <= 0.0d ? 0.0d : (t - from.timeMillis) / span;

        double x = from.x + (to.x - from.x) * alpha;
        double y = from.y + (to.y - from.y) * alpha;
        double z = from.z + (to.z - from.z) * alpha;
        float yaw = (float) (from.yaw + wrapDegrees(to.yaw - from.yaw) * alpha);
        float pitch = (float) (from.pitch + (to.pitch - from.pitch) * alpha);

        if (walk) {
            walkToward(player, x, z, yaw, pitch);
            return;
        }

        // Creative flight plus noClip keeps physics, collision and fall damage out of the picture;
        // the camera is fully determined by the path.
        player.capabilities.allowFlying = true;
        player.capabilities.isFlying = true;
        player.noClip = true;
        player.onGround = false;
        player.motionX = 0.0d;
        player.motionY = 0.0d;
        player.motionZ = 0.0d;

        player.setPositionAndRotation(x, y, z, yaw, pitch);
        player.prevPosX = player.posX;
        player.prevPosY = player.posY;
        player.prevPosZ = player.posZ;
        player.lastTickPosX = player.posX;
        player.lastTickPosY = player.posY;
        player.lastTickPosZ = player.posZ;
        player.prevRotationYaw = player.rotationYaw;
        player.prevRotationPitch = player.rotationPitch;
        player.rotationYawHead = player.rotationYaw;
        player.prevRotationYawHead = player.rotationYaw;
    }

    /**
     * Steers the player at the next point on the path and lets its own physics get it there.
     *
     * <p>Only the horizontal direction is driven. Height is left to gravity, which is the point —
     * the ground is what block collision is resolved against, and a player held off it by flight
     * resolves nothing.
     */
    private static void walkToward(EntityPlayerSP player, double targetX, double targetZ,
                                   float yaw, float pitch) {
        player.capabilities.allowFlying = false;
        player.capabilities.isFlying = false;
        player.noClip = false;
        player.rotationYaw = yaw;
        player.rotationPitch = pitch;
        player.rotationYawHead = yaw;

        double dx = targetX - player.posX;
        double dz = targetZ - player.posZ;
        double distance = Math.sqrt(dx * dx + dz * dz);
        if (distance < 0.05d) {
            player.movementInput.moveForward = 0f;
            player.movementInput.moveStrafe = 0f;
            return;
        }
        // Resolved into the player's own facing, because that is the frame movementInput is in and
        // the facing is following the path's yaw rather than the direction of travel.
        double radians = Math.toRadians(yaw);
        double forwardX = -Math.sin(radians);
        double forwardZ = Math.cos(radians);
        player.movementInput.moveForward = (float) clamp((dx * forwardX + dz * forwardZ) / distance);
        player.movementInput.moveStrafe = (float) clamp((dx * forwardZ - dz * forwardX) / distance);
    }

    private static double clamp(double value) {
        return Math.max(-1.0d, Math.min(1.0d, value));
    }

    private static double wrapDegrees(double degrees) {
        double wrapped = degrees % 360.0d;
        if (wrapped >= 180.0d) {
            wrapped -= 360.0d;
        }
        if (wrapped < -180.0d) {
            wrapped += 360.0d;
        }
        return wrapped;
    }
}

package top.fpsmaster.forge.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import top.fpsmaster.benchmark.BenchCounters;
import top.fpsmaster.benchmark.BenchmarkMode;
import top.fpsmaster.features.impl.optimizes.Performance;

/**
 * Caches the sky colour, which is recomputed from scratch every frame and is the single most
 * expensive thing the sky pass does.
 *
 * <p>Measured on a recorded Hypixel lobby by timing each call the pass makes in place: one call to
 * getSkyColor costs 242us of the 264us the whole pass accounts for. Everything else together —
 * eighteen state changes, twelve matrix operations, two texture binds, the dome itself — comes to
 * about 20us. The cost is inside the biome lookup, which walks the generator's layer stack whenever
 * a column has no cached biome, and nothing about it is cheap.
 *
 * <p>The result only changes when the camera moves to another block, when time passes, or when the
 * weather does. Those are exactly the things this keys on, so a hit is exact rather than
 * approximate — the colour returned is the colour vanilla would have computed. On a server that
 * holds the time still, which is most of them, it is the same value all day.
 *
 * <p><b>What it gives back depends on what the frame is waiting for.</b> The 33.9% above was
 * measured where the frame was limited by the processor. On a machine where it is limited by the
 * graphics card the saving is real but mostly does not become frame rate: the same lobby measured
 * the sky pass falling from 1198us to 117us while terrainSetup rose from 2128us to 3091us, the two
 * together unchanged at about 3200us, for +4.2% on the frame rate. Nothing moved into terrainSetup
 * — the processor simply reached the graphics card sooner and waited there instead, in the next
 * section that touches GL. The giveaway is that across three runs of that variant terrainSetup fell
 * as the frame rate rose, 3669us at 61fps down to 2285us at 72fps, which no amount of real work
 * does. Section timings on a card-limited frame are not attributable; read the frame total.
 */
@Mixin(World.class)
public class WorldMixin_SkyColorCache {

    /** Time quantised finely enough that a full day still gets hundreds of distinct sky colours. */
    @Unique
    private static final float ANGLE_STEP = 1.0f / 2048.0f;

    @Unique
    private Vec3 edge$cachedSkyColor;
    @Unique
    private long edge$cachedPosition = Long.MIN_VALUE;
    @Unique
    private int edge$cachedAngle = Integer.MIN_VALUE;
    @Unique
    private int edge$cachedWeather = Integer.MIN_VALUE;

    @Inject(method = "getSkyColor", at = @At("HEAD"), cancellable = true)
    private void edge$reuseSkyColor(Entity entity, float partialTicks,
                                    CallbackInfoReturnable<Vec3> callback) {
        if (!Performance.using || !Performance.cacheSkyColor.getValue()) {
            return;
        }
        World self = (World) (Object) this;
        long position = edge$positionKey(entity);
        int angle = (int) (self.getCelestialAngle(partialTicks) / ANGLE_STEP);
        int weather = Float.floatToRawIntBits(self.getRainStrength(partialTicks))
                * 31 + Float.floatToRawIntBits(self.getThunderStrength(partialTicks));

        if (edge$cachedSkyColor != null && position == edge$cachedPosition
                && angle == edge$cachedAngle && weather == edge$cachedWeather) {
            if (BenchmarkMode.ACTIVE) {
                BenchCounters.skyColorCacheHits++;
            }
            callback.setReturnValue(edge$cachedSkyColor);
            return;
        }
        edge$cachedPosition = position;
        edge$cachedAngle = angle;
        edge$cachedWeather = weather;
        edge$cachedSkyColor = null;
    }

    @Inject(method = "getSkyColor", at = @At("RETURN"))
    private void edge$storeSkyColor(Entity entity, float partialTicks,
                                    CallbackInfoReturnable<Vec3> callback) {
        if (Performance.using && Performance.cacheSkyColor.getValue()) {
            edge$cachedSkyColor = callback.getReturnValue();
        }
    }

    /** The block the colour was computed for; vanilla floors the entity position the same way. */
    @Unique
    private long edge$positionKey(Entity entity) {
        long x = MathHelper.floor_double(entity.posX) & 0x3FFFFFFL;
        long y = MathHelper.floor_double(entity.posY) & 0xFFFL;
        long z = MathHelper.floor_double(entity.posZ) & 0x3FFFFFFL;
        return x << 38 | y << 26 | z;
    }
}

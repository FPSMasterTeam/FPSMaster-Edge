package top.fpsmaster.forge.mixin;

import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import top.fpsmaster.forge.api.ICullable;

/**
 * Carries occlusion-culling state on the entity, replacing a boxed-key map lookup with a field read.
 *
 * <p>See {@link ICullable} for why: the map version allocated roughly 94,000 Integers per second.
 */
@Mixin(Entity.class)
public abstract class EntityMixin_Cullable implements ICullable {

    @Unique
    private boolean fpsmaster$occluded;
    @Unique
    private int fpsmaster$queryId;
    @Unique
    private boolean fpsmaster$queryPending;
    @Unique
    private long fpsmaster$lastProbeMillis;

    @Override
    public boolean fpsmaster$isOccluded() {
        return fpsmaster$occluded;
    }

    @Override
    public void fpsmaster$setOccluded(boolean occluded) {
        this.fpsmaster$occluded = occluded;
    }

    @Override
    public int fpsmaster$getQueryId() {
        return fpsmaster$queryId;
    }

    @Override
    public void fpsmaster$setQueryId(int queryId) {
        this.fpsmaster$queryId = queryId;
    }

    @Override
    public boolean fpsmaster$isQueryPending() {
        return fpsmaster$queryPending;
    }

    @Override
    public void fpsmaster$setQueryPending(boolean pending) {
        this.fpsmaster$queryPending = pending;
    }

    @Override
    public long fpsmaster$getLastProbeMillis() {
        return fpsmaster$lastProbeMillis;
    }

    @Override
    public void fpsmaster$setLastProbeMillis(long millis) {
        this.fpsmaster$lastProbeMillis = millis;
    }
}

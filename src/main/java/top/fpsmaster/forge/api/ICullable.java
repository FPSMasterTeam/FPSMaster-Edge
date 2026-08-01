package top.fpsmaster.forge.api;

/**
 * Per-entity occlusion state, stored on the entity itself.
 *
 * <p>The first version kept this in a {@code Map<Integer, Probe>}. That boxes the entity id on every
 * lookup, and there are two lookups per entity per frame — about 94,000 Integer allocations per
 * second at the frame rates this runs at, which showed up as a 30% rise in GC collections. Entity
 * ids above 127 miss the Integer cache, so the boxing is real allocation rather than a shared
 * constant.
 *
 * <p>Attaching the state to the entity removes the map, the hashing and the boxing at once, and it
 * disappears with the entity instead of needing a sweep to expire stale keys.
 */
public interface ICullable {

    boolean fpsmaster$isOccluded();

    void fpsmaster$setOccluded(boolean occluded);

    /** Query object currently in flight for this entity, or 0 when none. */
    int fpsmaster$getQueryId();

    void fpsmaster$setQueryId(int queryId);

    boolean fpsmaster$isQueryPending();

    void fpsmaster$setQueryPending(boolean pending);

    long fpsmaster$getLastProbeMillis();

    void fpsmaster$setLastProbeMillis(long millis);

    /**
     * Whether the entity was inside the view frustum the last time culling looked at it.
     *
     * <p>A verdict is only true for the angle it was measured from. An entity that was behind a wall,
     * left the frustum and came back — the player turned away and walked round — is being judged on
     * where the wall used to be, and would stay invisible until its timer came round again. The
     * transition out-of-frustum to in-frustum is the moment that verdict stops being evidence.
     */
    boolean fpsmaster$wasInFrustum();

    void fpsmaster$setInFrustum(boolean inFrustum);
}

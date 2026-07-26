package top.fpsmaster.features.impl.optimizes;

import top.fpsmaster.features.manager.Category;
import top.fpsmaster.features.manager.Module;
import top.fpsmaster.features.settings.impl.BooleanSetting;
import top.fpsmaster.features.settings.impl.NumberSetting;
import top.fpsmaster.utils.render.culling.EntityCulling;


public class Performance extends Module {

    /**
     * Master switch, read from the mixins that implement the individual optimisations.
     *
     * <p>Every sub-feature checks this in addition to its own toggle: disabling the module has to
     * actually disable its optimisations, otherwise the module switch means nothing.
     */
    public static boolean using = false;

    public static BooleanSetting ignoreStands = new BooleanSetting("IgnoreStands", true);
    public static BooleanSetting fastLoad = new BooleanSetting("FastLoad", true);
    public static BooleanSetting fontOptimize = new BooleanSetting("FontOptimize", false);
    public static BooleanSetting staticParticleColor = new BooleanSetting("StaticParticleColor", true);
    public static BooleanSetting limitChunks = new BooleanSetting("LimitChunks", true);
    public static BooleanSetting batchModelRendering = new BooleanSetting("BatchModelRendering", true);
    public static BooleanSetting lowAnimationTick = new BooleanSetting("LowAnimationTick", true);
    public static BooleanSetting downscalePackIcons = new BooleanSetting("DownscalePackIcons", true);
    public static BooleanSetting particleCulling = new BooleanSetting("ParticleCulling", true);
    public static BooleanSetting entityCulling = new BooleanSetting("EntityCulling", false);

    /** Shared culling state; lives here so the mixins that drive it have one owner. */
    public static final EntityCulling ENTITY_CULLING = new EntityCulling();

    /**
     * Minimum of 1: at 0 the throttle's condition {@code renderChunksUpdated >= 0} is always true,
     * which parks the chunk builder thread in a 50ms sleep loop and stops terrain rebuilding
     * entirely.
     */
    public static NumberSetting chunkUpdateLimit = new NumberSetting("ChunkUpdateLimit", 50, 1, 250, 1);
    public static NumberSetting fpsLimit = new NumberSetting("FPSLimit", 30, 0, 360, 1);
    public static NumberSetting particlesLimit = new NumberSetting("ParticlesLimit", 400, 0, 2000, 1);

    /**
     * How long an entity keeps its visibility verdict before being probed again. Probing every
     * entity every frame would cost more in queries and draw calls than the culling saves.
     */
    public static NumberSetting entityCullingInterval =
            new NumberSetting("EntityCullingInterval", 50, 10, 500, 5, () -> entityCulling.getValue());

    public Performance() {
        super("Performance", Category.OPTIMIZE);
        addSettings(ignoreStands, fastLoad, batchModelRendering, lowAnimationTick, fpsLimit,
                particlesLimit, fontOptimize, staticParticleColor, limitChunks, chunkUpdateLimit,
                downscalePackIcons, particleCulling, entityCulling, entityCullingInterval);
    }


    @Override
    public void onEnable() {
        super.onEnable();
        using = true;
    }

    @Override
    public void onDisable() {
        super.onDisable();
        using = false;
    }
}

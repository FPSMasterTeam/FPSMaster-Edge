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
    public static BooleanSetting staticParticleColor = new BooleanSetting("StaticParticleColor", true);
    public static BooleanSetting limitChunks = new BooleanSetting("LimitChunks", true);
    public static BooleanSetting batchModelRendering = new BooleanSetting("BatchModelRendering", true);
    public static BooleanSetting lowAnimationTick = new BooleanSetting("LowAnimationTick", true);
    public static BooleanSetting downscalePackIcons = new BooleanSetting("DownscalePackIcons", true);
    public static BooleanSetting particleCulling = new BooleanSetting("ParticleCulling", true);
    public static BooleanSetting cacheSkyColor = new BooleanSetting("CacheSkyColor", true);
    public static BooleanSetting entityCulling = new BooleanSetting("EntityCulling", false);

    /**
     * Draws every string vanilla would draw with the client's own renderer instead.
     *
     * <p>Vanilla spends about half a microsecond per character inside its glyph loop, which on a
     * busy server is 362us a frame; the client's renderer submits a whole string at once and costs a
     * fraction of that. It is off by default because it changes how the game looks: the two faces
     * are not the same shape and the replacement is the narrower of them.
     */
    public static BooleanSetting customHudFont = new BooleanSetting("CustomHudFont", false);

    /**
     * Draws straight to the back buffer instead of through a framebuffer.
     *
     * <p>Saves a full-screen write and a full-screen textured draw every frame, and gives up
     * everything that reads the frame back — the client's blur, its motion blur, the minimap and
     * the shader helpers all stand down while it is on. Off by default, and not yet measurable:
     * it changes what the graphics card does, and the only machine available is limited by the
     * graphics card, so three paired runs put it anywhere between -3.9% and +5.3%.
     */
    public static BooleanSetting fastRender = new BooleanSetting("FastRender", false);
    public static BooleanSetting cullPlayers =
            new BooleanSetting("CullPlayers", false, () -> entityCulling.getValue());

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

    /**
     * How many entities have to be on screen before probing is worth doing at all.
     *
     * <p>Culling can only give back what the entities cost, and a scene with a handful of them has
     * nothing to give: a recorded Hypixel lobby draws fourteen, one of which is behind something,
     * and turning culling on there moved the frame rate by -0.2%. Below this count the probes are
     * skipped and everything renders. Zero means always probe.
     */
    public static NumberSetting entityCullingMinEntities =
            new NumberSetting("EntityCullingMinEntities", 24, 0, 256, 1, () -> entityCulling.getValue());

    /**
     * Point size the replacement font is rasterised at, chosen by height rather than width.
     *
     * <p>Matching vanilla's character advances would take 21, but vanilla puts its lines 9 pixels
     * apart and a face that wide is too tall for that — the scoreboard and the tab list draw over
     * themselves. Width does not need matching anyway: the replacement also answers
     * {@code getCharWidth}, so everything vanilla lays out from character widths is measured with
     * the same font it is drawn with. At 16 the ink rises about 7 pixels above the baseline, which
     * is where vanilla's does.
     */
    public static NumberSetting customHudFontSize =
            new NumberSetting("CustomHudFontSize", 16, 12, 28, 1, () -> customHudFont.getValue());

    public Performance() {
        super("Performance", Category.OPTIMIZE);
        addSettings(ignoreStands, fastLoad, batchModelRendering, lowAnimationTick, fpsLimit,
                particlesLimit, staticParticleColor, limitChunks, chunkUpdateLimit,
                downscalePackIcons, particleCulling, entityCulling, cullPlayers,
                entityCullingInterval, entityCullingMinEntities, cacheSkyColor, customHudFont, customHudFontSize, fastRender);
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

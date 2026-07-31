package top.fpsmaster.features.impl.optimizes;

import top.fpsmaster.features.manager.Category;
import top.fpsmaster.features.manager.Module;
import top.fpsmaster.features.settings.Setting;
import top.fpsmaster.features.settings.impl.BooleanSetting;
import top.fpsmaster.features.settings.impl.ModeSetting;
import top.fpsmaster.features.settings.impl.NumberSetting;
import top.fpsmaster.utils.render.TextureResolution;
import top.fpsmaster.utils.render.culling.EntityCulling;

import static top.fpsmaster.utils.core.Utility.mc;


public class Performance extends Module {

    /**
     * Master switch, read from the mixins that implement the individual optimisations.
     *
     * <p>Every sub-feature checks this in addition to its own toggle: disabling the module has to
     * actually disable its optimisations, otherwise the module switch means nothing.
     */
    public static boolean using = false;

    /**
     * Stops drawing the name label above armour stands.
     *
     * <p>It does not skip the stand itself, only its name — the injection is on
     * {@code Render.renderName}. That distinction matters on a server: a hologram is an invisible
     * armour stand whose entire content is its name, so this hides shop labels, kill feeds and
     * every other piece of floating text a server puts in the world.
     *
     * <p>Off by default for that reason. It removes content rather than making the same picture
     * cheaper, and a player who cannot see the shop labels has no way to guess which setting did it.
     */
    public static BooleanSetting ignoreStands = new BooleanSetting("IgnoreStands", false);
    public static BooleanSetting fastLoad = new BooleanSetting("FastLoad", true);
    /**
     * Renders every particle at full brightness instead of looking its light level up.
     *
     * <p>Off by default: it changes what is on screen rather than how fast the same picture is
     * drawn. Particles stop responding to the light around them, so they glow at night and in caves.
     */
    public static BooleanSetting staticParticleColor = new BooleanSetting("StaticParticleColor", false);
    public static BooleanSetting limitChunks = new BooleanSetting("LimitChunks", true);
    public static BooleanSetting batchModelRendering = new BooleanSetting("BatchModelRendering", true);
    /**
     * Cuts the sample count in {@code doVoidFogParticles} from 1000 to 100.
     *
     * <p>That call is what scatters ambient particles around the player — lava sparks, torch smoke,
     * portal haze — so a tenth of the samples is a tenth of the particles. Off by default for the
     * same reason as the rest of this group: the scene visibly thins out.
     */
    public static BooleanSetting lowAnimationTick = new BooleanSetting("LowAnimationTick", false);
    public static BooleanSetting downscalePackIcons = new BooleanSetting("DownscalePackIcons", true);
    public static BooleanSetting particleCulling = new BooleanSetting("ParticleCulling", true);
    public static BooleanSetting cacheSkyColor = new BooleanSetting("CacheSkyColor", true);
    public static BooleanSetting entityCulling = new BooleanSetting("EntityCulling", false);

    /**
     * Skips rebuilding a worn armour texture path that never changes.
     *
     * <p>Forge formats the path with {@code String.format} on every call and only then consults its
     * map, so the map saves an allocation and pays the formatting regardless. The path depends only
     * on the armour material, the slot and whether this is the overlay pass, and caching it on those
     * produces the same {@code ResourceLocation} vanilla would have.
     */
    public static BooleanSetting cacheArmorTextures = new BooleanSetting("CacheArmorTextures", true);

    /**
     * Stops drawing sign text past the distance at which a glyph falls below about a pixel.
     *
     * <p>The text is the entire cost of rendering a sign — splitting the lines into components,
     * measuring them and pushing them through the font renderer, every frame, for every sign in
     * render distance. The cutoff scales with window height and field of view rather than being a
     * fixed number of blocks, so it stays at the same apparent size whatever the display.
     */
    public static BooleanSetting signTextCulling = new BooleanSetting("SignTextCulling", true);

    /**
     * Stops drawing block entities past {@link #blockEntityDistance}.
     *
     * <p>Off by default because it is visible rather than free: unlike sign text, a chest or an
     * enchanting table does not become unreadable at a distance, it just disappears. Forge already
     * frustum-tests these and vanilla already caps them, so there is nothing invisible left to
     * reclaim — this only exists for worlds that put hundreds of them in view at once.
     */
    public static BooleanSetting blockEntityCulling = new BooleanSetting("BlockEntityCulling", false);

    public static NumberSetting blockEntityDistance =
            new NumberSetting("BlockEntityDistance", 32, 8, 64, 1, () -> blockEntityCulling.getValue());

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

    /**
     * Raises the chunk builder's allowance while the camera is still.
     *
     * <p>Moving, a rebuild competes with the frame drawing what is already built and the backlog
     * refills as fast as it drains, which is when a limit earns its keep. Standing still, the backlog
     * is finite and every rebuild still queued is a hole in the world — and a queued rebuild is not
     * free to leave queued, because a non-empty pending set forces a full terrain visibility walk on
     * every frame it stays occupied.
     *
     * <p>Separate from {@link #limitChunks} so the two can be told apart. Measuring the throttle
     * against no throttle while this was on measured a doubled throttle, which is a third thing.
     *
     * <p>Off by default because it has not beaten the fixed budget. Three interleaved passes put it
     * anywhere from the worst variant to the best — 285 to 417 fps for the same configuration — and
     * the roadmap's own acceptance for this item is to keep the fixed budget unless the adaptive one
     * wins. The switch stays so the question can be asked again on an instrument that can answer it.
     */
    public static BooleanSetting adaptiveChunkBudget =
            new BooleanSetting("AdaptiveChunkBudget", false, () -> limitChunks.getValue());
    /**
     * Frame rate to hold the game at while its window is not focused. Zero disables the cap —
     * {@code Display.sync} returns immediately for anything at or below zero.
     *
     * <p>Defaults to zero so the module changes nothing on its own. Note that vanilla only reaches
     * this at all when the player's own frame rate limit is below Unlimited: {@code runGameLoop}
     * guards the {@code Display.sync} call with {@code isFramerateLimitBelowMax}, which reads the
     * game setting rather than this one.
     */
    public static NumberSetting fpsLimit = new NumberSetting("FPSLimit", 0, 0, 360, 1);
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

    /**
     * Stops the six block switches below from being drawn into the chunk mesh.
     *
     * <p>These are content removal rather than optimisation — they trade something the player can
     * see for the geometry it costs, so all of them are off by default and none of them is counted
     * as a same-picture gain. What they are worth depends entirely on the world: a flower forest or
     * a fence-heavy build puts thousands of these in a chunk, and a Hypixel lobby has none.
     *
     * <p>Grass and flowers are separated from their double-height forms because the double plants
     * are one block class with a variant, and a player hiding decoration usually does not want the
     * two-block-tall ones to become one-block stumps.
     */
    public static BooleanSetting hideTallGrass = new BooleanSetting("HideTallGrass", false);
    public static BooleanSetting hideDoubleTallGrass = new BooleanSetting("HideDoubleTallGrass", false);
    public static BooleanSetting hideFlowers = new BooleanSetting("HideFlowers", false);
    public static BooleanSetting hideDoubleTallFlowers = new BooleanSetting("HideDoubleTallFlowers", false);
    public static BooleanSetting hideFences = new BooleanSetting("HideFences", false);
    public static BooleanSetting hideFenceGates = new BooleanSetting("HideFenceGates", false);

    /**
     * Skips drawing skull block entities — heads on the floor or the wall, and the player heads a
     * server uses as decoration. The block itself has no chunk geometry in 1.8.9, so this is the
     * whole of it.
     */
    public static BooleanSetting hideSkulls = new BooleanSetting("HideSkulls", false);

    /**
     * Skips the armour stand's model, its armour and whatever it is holding — but not its name.
     *
     * <p>The name is the point. A hologram is an invisible armour stand whose entire content is the
     * text above it, so a switch that took the text with the model would delete every shop label and
     * floating kill feed on the server, which is exactly what made {@link #ignoreStands} a bad
     * default. Those two are complementary rather than overlapping: this one drops the body and
     * keeps the label, {@code IgnoreStands} drops the label and keeps the body.
     */
    public static BooleanSetting hideArmorStands = new BooleanSetting("HideArmorStands", false);

    /** Skips the item frame's frame and contents, keeping its name label for the same reason. */
    public static BooleanSetting hideItemFrames = new BooleanSetting("HideItemFrames", false);

    /**
     * Skips only the map inside an item frame, leaving the frame itself.
     *
     * <p>A framed map is drawn as a full map render — the map texture plus every icon on it — where
     * an ordinary framed item is one small model. On a build with a map wall this is most of what
     * item frames cost, and unlike {@link #hideItemFrames} it leaves the frames visible.
     */
    public static BooleanSetting hideMapsInItemFrames = new BooleanSetting("HideMapsInItemFrames", false);

    /** Skips the arrows sticking out of a player who has been hit. */
    public static BooleanSetting hideStuckArrows = new BooleanSetting("HideStuckArrows", false);

    /** Skips arrows that have landed and stopped. Arrows still in flight are unaffected. */
    public static BooleanSetting hideGroundArrows = new BooleanSetting("HideGroundArrows", false);

    /**
     * Stops lava blocks throwing their ambient sparks.
     *
     * <p>Cut at the source — the block's own random display tick — rather than at the particle
     * renderer, so the particle is never constructed, ticked or stored.
     */
    public static BooleanSetting hideLavaParticles = new BooleanSetting("HideLavaParticles", false);

    /** Stops mob spawners smoking. The spawner still counts down and its mob still spins. */
    public static BooleanSetting hideSpawnerParticles = new BooleanSetting("HideSpawnerParticles", false);

    /**
     * Skips the miniature mob spinning inside a spawner.
     *
     * <p>The most expensive of this group by some way: that mob goes through the full entity
     * renderer — model, texture, layers and all — once per spawner per frame.
     */
    public static BooleanSetting hideMobInSpawner = new BooleanSetting("HideMobInSpawner", false);

    /**
     * Stops nether and end portals throwing particles. The portal's sound is unaffected.
     */
    public static BooleanSetting hidePortalParticles = new BooleanSetting("HidePortalParticles", false);

    /**
     * Per-category render distance, as a fraction of the distance the game would use anyway.
     *
     * <p>Vanilla has one rule for every entity: an entity is drawn while it is within its own
     * bounding box's average edge times 64 times its {@code renderDistanceWeight}. These scale that
     * limit for four groups separately, so dropped items can stop being drawn at a third of the
     * range while players are still visible to the horizon.
     *
     * <p>A multiplier rather than a distance in blocks, because the vanilla limit already varies per
     * entity — a fixed number would treat a dropped item and an ender dragon the same.
     *
     * <p>1.0 is vanilla and costs nothing: the check is skipped entirely when nothing is scaled.
     */
    public static NumberSetting playerRenderDistance =
            new NumberSetting("PlayerRenderDistance", 1.0, 0.1, 1.0, 0.05);
    public static NumberSetting passiveRenderDistance =
            new NumberSetting("PassiveRenderDistance", 1.0, 0.1, 1.0, 0.05);
    public static NumberSetting hostileRenderDistance =
            new NumberSetting("HostileRenderDistance", 1.0, 0.1, 1.0, 0.05);
    public static NumberSetting miscRenderDistance =
            new NumberSetting("MiscRenderDistance", 1.0, 0.1, 1.0, 0.05);

    /**
     * How far down the block atlas's mipmap chain to start sampling.
     *
     * <p>Each step halves the resolution in both axes, so the names are what the texture is worth
     * rather than a pixel count: a 16x pack at Quarter is being drawn at 4x. A pixel count would be
     * a lie anyway, since the same setting has to mean something on a 16x pack and a 512x one.
     *
     * <p>The index is the LOD floor itself, which is why it is a mode rather than a number: the
     * levels that exist are the levels the atlas was built with, and there is no meaning between
     * them. See {@link top.fpsmaster.utils.render.TextureResolution} for why this is a sampling
     * setting rather than an image one.
     */
    public static ModeSetting textureResolution = new ModeSetting("TextureResolution", 0,
            "Default", "Half", "Quarter", "Eighth", "Sixteenth");

    /**
     * Skips the entity query inside block collision when nothing loaded can answer it.
     *
     * <p>Two thirds of what collision costs on a busy recording is a query whose results only two
     * entity classes in the game can contribute to — boats and minecarts. See
     * {@code WorldMixin_FastCollision} for why that is a fact about the entity tree rather than a
     * guess about the map.
     */
    public static BooleanSetting fastCollision = new BooleanSetting("FastCollision", false);

    /**
     * True while any of the six block switches is on.
     *
     * <p>The injection that implements them is on {@code renderBlock}, which runs once per block per
     * render layer on every chunk rebuild — the busiest loop the chunk builder has. All six are off
     * by default, so that case has to cost one field read rather than a chain of instanceof tests.
     * Written from the settings' change listener, read from the chunk builder thread.
     */
    public static volatile boolean hidingBlocks = false;

    public Performance() {
        super("Performance", Category.OPTIMIZE);
        addSettings(ignoreStands, fastLoad, batchModelRendering, lowAnimationTick, fpsLimit,
                particlesLimit, staticParticleColor, limitChunks, chunkUpdateLimit,
                downscalePackIcons, particleCulling, entityCulling, cacheArmorTextures,
                signTextCulling, blockEntityCulling, blockEntityDistance, cullPlayers,
                entityCullingInterval, entityCullingMinEntities, cacheSkyColor, customHudFont,
                customHudFontSize, fastRender, hideTallGrass, hideDoubleTallGrass, hideFlowers,
                hideDoubleTallFlowers, hideFences, hideFenceGates, hideSkulls, hideArmorStands,
                hideItemFrames, hideMapsInItemFrames, hideStuckArrows, hideGroundArrows,
                hideLavaParticles, hideSpawnerParticles, hideMobInSpawner, hidePortalParticles,
                playerRenderDistance, passiveRenderDistance, hostileRenderDistance,
                miscRenderDistance, textureResolution, fastCollision, adaptiveChunkBudget);

        textureResolution.addChangeListener(
                (setting, oldValue, newValue) -> TextureResolution.apply());

        // Chunk meshes are built once and kept, so toggling one of these changes nothing that is
        // already on screen until every chunk is rebuilt. loadRenderers does that and is what
        // vanilla itself calls when a graphics setting changes the mesh.
        Setting.ChangeListener<Boolean> rebuildChunks = (setting, oldValue, newValue) -> {
            hidingBlocks = hideTallGrass.getValue() || hideDoubleTallGrass.getValue()
                    || hideFlowers.getValue() || hideDoubleTallFlowers.getValue()
                    || hideFences.getValue() || hideFenceGates.getValue();
            if (mc.renderGlobal != null && mc.theWorld != null) {
                mc.renderGlobal.loadRenderers();
            }
        };
        hideTallGrass.addChangeListener(rebuildChunks);
        hideDoubleTallGrass.addChangeListener(rebuildChunks);
        hideFlowers.addChangeListener(rebuildChunks);
        hideDoubleTallFlowers.addChangeListener(rebuildChunks);
        hideFences.addChangeListener(rebuildChunks);
        hideFenceGates.addChangeListener(rebuildChunks);
    }


    @Override
    public void onEnable() {
        super.onEnable();
        using = true;
        rebuildChunksIfHidingBlocks();
        TextureResolution.apply();
    }

    @Override
    public void onDisable() {
        super.onDisable();
        using = false;
        rebuildChunksIfHidingBlocks();
        TextureResolution.apply();
    }

    /**
     * The module switch gates the block hiding as well, so turning the module off has to put the
     * hidden blocks back and turning it on has to take them away again. Nothing to do when none of
     * the six is on, which is the default and the usual case.
     */
    private void rebuildChunksIfHidingBlocks() {
        if (hidingBlocks && mc.renderGlobal != null && mc.theWorld != null) {
            mc.renderGlobal.loadRenderers();
        }
    }
}

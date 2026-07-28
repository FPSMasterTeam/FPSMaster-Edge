package top.fpsmaster.benchmark;

/**
 * Ceiling probes: switches that delete work outright to find out what it was worth.
 *
 * <p>Deleting a whole pass is not an optimisation anyone can ship, but it answers the question that
 * decides whether building the real one is worth it. A profiler attributes cost to a section; a probe
 * proves the frame actually gets shorter when that section stops running. Those disagree more often
 * than they should — the first thing this project measured was a section whose removal changed
 * nothing at all.
 *
 * <p>Driven by system properties so nothing here can be reached from a config file or a menu, and
 * gated on the benchmark being active so it cannot affect a real session.
 *
 * <pre>
 *   -Dedge.exp.noSky=true         skip the sky pass entirely
 *   -Dedge.exp.noNameplates=true  skip every entity name label
 *   -Dedge.exp.noGlint=true       skip the enchantment glint on worn armour
 *   -Dedge.exp.noArmor=true       skip the worn-armour layer entirely
 *   -Dedge.exp.noHeldItem=true    skip the held-item layer
 *   -Dedge.exp.noSignText=true    skip the text drawn on signs
 *   -Dedge.exp.noSkyLists=true    skip the sky's compiled geometry
 *   -Dedge.exp.noSkyImmediate=true  skip the sky's per-frame vertex uploads
 *   -Dedge.exp.noSkyStateToggles=true  skip the sky's fixed-function state changes
 *   -Dedge.exp.noGpuTimer=true     stop issuing GPU timestamp queries at all
 *   -Dedge.exp.skyBreakdown=true   time every call the sky pass makes
 *   -Dedge.exp.hudBreakdown=true   time each piece of the overlay by name
 *   -Dedge.exp.fontCompare=true    draw the same strings through both font renderers
 * </pre>
 */
public final class Experiments {

    public static final boolean NO_SKY = flag("noSky");
    public static final boolean NO_NAMEPLATES = flag("noNameplates");
    public static final boolean NO_GLINT = flag("noGlint");
    public static final boolean NO_ARMOR = flag("noArmor");
    public static final boolean NO_HELD_ITEM = flag("noHeldItem");
    public static final boolean NO_SIGN_TEXT = flag("noSignText");
    public static final boolean NO_SKY_LISTS = flag("noSkyLists");
    public static final boolean NO_SKY_IMMEDIATE = flag("noSkyImmediate");
    public static final boolean NO_SKY_STATE_TOGGLES = flag("noSkyStateToggles");
    public static final boolean NO_GPU_TIMER = flag("noGpuTimer");
    public static final boolean SKY_BREAKDOWN = flag("skyBreakdown");
    public static final boolean HUD_BREAKDOWN = flag("hudBreakdown");
    public static final boolean FONT_COMPARE = flag("fontCompare");
    public static final boolean FONT_STYLES = flag("fontStyles");

    private Experiments() {
    }

    private static boolean flag(String name) {
        return Boolean.getBoolean("edge.exp." + name);
    }

    /** True when a probe should take effect: benchmark runs only. */
    public static boolean active(boolean flag) {
        return flag && BenchmarkMode.ACTIVE;
    }
}

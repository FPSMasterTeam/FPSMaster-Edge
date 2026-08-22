package top.fpsmaster.features.impl;

import top.fpsmaster.features.manager.Category;
import top.fpsmaster.features.manager.Module;
import top.fpsmaster.features.settings.impl.BooleanSetting;
import top.fpsmaster.features.settings.impl.ColorSetting;
import top.fpsmaster.features.settings.impl.NumberSetting;

import java.awt.*;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;

/**
 * Base for HUD modules.
 *
 * <p>The shared appearance settings below are consumed by {@code Component.drawRect} and
 * {@code Component.drawString} — by the base class, never by the subclass that inherits them.
 * Registration therefore belongs here too. Subclasses used to call {@code addSettings(...)} for them
 * by hand, which drifted badly: some registered none (the settings still took effect but were
 * invisible in the ClickGUI and never persisted), several registered the same setting twice
 * (duplicate rows bound to one value), and none of it changed behaviour either way.
 *
 * <p>Subclasses now declare their {@link Trait}s and register only their own settings;
 * {@link #registerCommonSettings()} is applied centrally once every module is constructed.
 */
public class InterfaceModule extends Module {
    /** What a HUD module draws, which decides the shared settings that apply to it. */
    public enum Trait {
        /** Draws a background panel: {@code bg}, {@code backgroundColor}, {@code rounded}, {@code roundRadius}. */
        BACKGROUND,
        /** Draws text: {@code betterFont}, {@code fontShadow}. */
        TEXT,
        /** Lays out repeated items and honours {@code spacing}. */
        SPACING
    }

    public BooleanSetting rounded = new BooleanSetting("Round", true);
    public NumberSetting roundRadius = new NumberSetting("RoundRadius", 3, 0, 30, 1, () -> rounded.getValue());
    public BooleanSetting betterFont = new BooleanSetting("BetterFont", false);
    public BooleanSetting fontShadow = new BooleanSetting("FontShadow", true);
    public BooleanSetting bg = new BooleanSetting("Background", true);
    public ColorSetting backgroundColor = new ColorSetting("BackgroundColor", new Color(0, 0, 0, 0), () -> bg.getValue());
    public NumberSetting spacing = new NumberSetting("Spacing", 0, 0, 3, 1);

    /** For modules that draw neither a panel nor text — pass this rather than an empty arg list,
     *  which would resolve to the two-arg constructor and silently pick up the defaults. */
    public static final Trait[] NONE = new Trait[0];

    private final Set<Trait> traits;

    /** Most HUD modules draw a background panel with text on it. */
    public InterfaceModule(String name, Category category) {
        this(name, category, Trait.BACKGROUND, Trait.TEXT);
    }

    public InterfaceModule(String name, Category category, Trait... traits) {
        super(name, category);
        this.traits = traits.length == 0
                ? EnumSet.noneOf(Trait.class)
                : EnumSet.copyOf(Arrays.asList(traits));
    }

    public boolean has(Trait trait) {
        return traits.contains(trait);
    }

    /**
     * Appends the shared settings this module's traits call for. Called once from
     * {@code ModuleManager.init()} after every module is constructed, so a module's own settings stay
     * at the top of its ClickGUI panel and the shared appearance ones follow. Idempotent —
     * {@code addSettings} ignores anything already registered.
     */
    public void registerCommonSettings() {
        if (has(Trait.BACKGROUND)) {
            bg.inGroup("background");
            backgroundColor.inGroup("background");
            addSettings(bg, backgroundColor);
            rounded.inGroup("style");
            roundRadius.inGroup("style");
            addSettings(rounded, roundRadius);
        }
        if (has(Trait.SPACING)) {
            spacing.inGroup("style");
            addSettings(spacing);
        }
        if (has(Trait.TEXT)) {
            betterFont.inGroup("font");
            fontShadow.inGroup("font");
            addSettings(betterFont, fontShadow);
        }
    }
}

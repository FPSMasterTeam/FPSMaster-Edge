package top.fpsmaster.features.impl.interfaces;

import top.fpsmaster.features.impl.InterfaceModule;
import top.fpsmaster.features.manager.Category;
import top.fpsmaster.features.settings.impl.BooleanSetting;
import top.fpsmaster.features.settings.impl.ColorSetting;
import top.fpsmaster.features.settings.impl.NumberSetting;

import java.awt.Color;

public class LyricsDisplay extends InterfaceModule {
    public static final BooleanSetting background = new BooleanSetting("Background", true);
    public static final NumberSetting fontSize = new NumberSetting("FontSize", 18, 10, 30, 1);
    public static final BooleanSetting scroll = new BooleanSetting("Scroll", true);
    public static final NumberSetting lines = new NumberSetting("Lines", 2, 1, 5, 1);
    public static final BooleanSetting translation = new BooleanSetting("Translation", true);
    public static final ColorSetting textColor = new ColorSetting("TextColor", Color.WHITE);
    public static final ColorSetting backgroundColor = new ColorSetting("PanelColor", new Color(0, 0, 0, 150));

    public LyricsDisplay() {
        super("LyricsDisplay", Category.Interface, NONE);
        addSettings(background.inGroup("background"), backgroundColor.inGroup("background"),
                fontSize.inGroup("font"), textColor.inGroup("font"),
                scroll.inGroup("style"), lines.inGroup("style"), translation.inGroup("style"));
    }
}

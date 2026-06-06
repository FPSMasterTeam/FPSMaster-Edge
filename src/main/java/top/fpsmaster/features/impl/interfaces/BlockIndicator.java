package top.fpsmaster.features.impl.interfaces;

import top.fpsmaster.features.impl.InterfaceModule;
import top.fpsmaster.features.manager.Category;
import top.fpsmaster.features.settings.impl.BooleanSetting;
import top.fpsmaster.features.settings.impl.ColorSetting;
import top.fpsmaster.features.settings.impl.NumberSetting;

import java.awt.Color;

public class BlockIndicator extends InterfaceModule {
    public final BooleanSetting showId = new BooleanSetting("ShowId", true);
    public final BooleanSetting showCoords = new BooleanSetting("ShowCoords", true);
    public final NumberSetting yOffset = new NumberSetting("YOffset", 18, 0, 120, 1);
    public final ColorSetting panelColor = new ColorSetting("PanelColor", new Color(255, 255, 255, 24));
    public final ColorSetting accentColor = new ColorSetting("AccentColor", new Color(105, 180, 255, 220));

    public BlockIndicator() {
        super("BlockIndicator", Category.Interface);
        backgroundColor.setColor(new Color(18, 20, 26, 190));
        addSettings(showId, showCoords, yOffset, bg, rounded, roundRadius, backgroundColor, panelColor, accentColor);
    }
}

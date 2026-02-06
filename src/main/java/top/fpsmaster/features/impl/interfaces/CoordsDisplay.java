package top.fpsmaster.features.impl.interfaces;

import top.fpsmaster.features.impl.InterfaceModule;
import top.fpsmaster.features.manager.Category;
import top.fpsmaster.features.settings.impl.BooleanSetting;
import top.fpsmaster.features.settings.impl.NumberSetting;

public class CoordsDisplay extends InterfaceModule {

    public BooleanSetting limitDisplay = new BooleanSetting("LimitDisplay", false);
    public NumberSetting limitDisplayY = new NumberSetting("LimitDisplayY", 92, 0, 255, 1);

    public CoordsDisplay() {
        super("CoordsDisplay", Category.Interface);
        addSettings(rounded, backgroundColor, fontShadow, betterFont, bg, rounded, roundRadius);
        addSettings(limitDisplay, limitDisplayY);
    }
}

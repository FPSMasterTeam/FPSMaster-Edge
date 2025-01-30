package top.fpsmaster.features.impl.interfaces;

import top.fpsmaster.features.impl.InterfaceModule;
import top.fpsmaster.features.manager.Category;

public class PlayerDisplay extends InterfaceModule {
    public PlayerDisplay() {
        super("PlayerDisplay", Category.Interface);
        addSettings(betterFont, rounded, fontShadow, backgroundColor, bg, rounded, roundRadius);
    }
}

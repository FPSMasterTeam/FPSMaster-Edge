package top.fpsmaster.features.impl.interfaces;

import top.fpsmaster.features.impl.InterfaceModule;
import top.fpsmaster.features.manager.Category;

public class InventoryDisplay extends InterfaceModule {
    public InventoryDisplay() {
        super("InventoryDisplay", Category.Interface);
        addSettings(backgroundColor, bg, rounded, roundRadius);
    }
}




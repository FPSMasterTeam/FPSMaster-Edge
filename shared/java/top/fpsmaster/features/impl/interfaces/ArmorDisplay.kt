package top.fpsmaster.features.impl.interfaces

import top.fpsmaster.features.impl.InterfaceModule
import top.fpsmaster.features.manager.Category
import top.fpsmaster.features.settings.impl.ModeSetting

class ArmorDisplay : InterfaceModule("ArmorDisplay", Category.Interface) {
    init {
        addSettings(rounded, backgroundColor, fontShadow, betterFont, mode, bg, rounded , roundRadius)
    }

    companion object {
        @JvmField
        var mode = ModeSetting("Mode", 0, "SimpleHoriz", "SimpleVertical", "Vertical")
    }
}

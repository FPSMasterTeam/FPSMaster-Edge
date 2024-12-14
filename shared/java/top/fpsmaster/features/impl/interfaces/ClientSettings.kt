package top.fpsmaster.features.impl.interfaces

import org.lwjgl.input.Keyboard
import top.fpsmaster.features.impl.InterfaceModule
import top.fpsmaster.features.manager.Category
import top.fpsmaster.features.settings.impl.BindSetting
import top.fpsmaster.features.settings.impl.BooleanSetting

class ClientSettings : InterfaceModule("ClientSettings", Category.Interface) {
    init {
        addSettings(keyBind, fixedScale)
    }

    override fun onEnable() {
        super.onEnable()
        this.set(false)
    }


    companion object {
        var keyBind = BindSetting("ClickGuiKey", Keyboard.KEY_RSHIFT)
        var fixedScale = BooleanSetting("FixedScale", true)
    }
}

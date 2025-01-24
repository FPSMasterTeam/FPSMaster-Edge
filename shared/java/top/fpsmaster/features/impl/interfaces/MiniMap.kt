package top.fpsmaster.features.impl.interfaces

import top.fpsmaster.FPSMaster
import top.fpsmaster.features.impl.InterfaceModule
import top.fpsmaster.features.manager.Category
import top.fpsmaster.ui.notification.Notification
import top.fpsmaster.ui.notification.NotificationManager
import top.fpsmaster.utils.OptifineUtil
import top.fpsmaster.utils.Utility

class MiniMap : InterfaceModule("MiniMap", Category.Interface) {
    override fun onEnable() {
        super.onEnable()
        if (OptifineUtil.isFastRender()) {
            OptifineUtil.setFastRender(false)
            NotificationManager.addNotification(
                FPSMaster.i18n["minimap.fastrender.disable.title"], FPSMaster.i18n["minimap.fastrender.disable.title"],
                5000f
            )
        }
    }
}
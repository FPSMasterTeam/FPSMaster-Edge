package top.fpsmaster.features.impl.interfaces;

import top.fpsmaster.FPSMaster;
import top.fpsmaster.features.impl.InterfaceModule;
import top.fpsmaster.features.manager.Category;
import top.fpsmaster.ui.notification.NotificationManager;
import top.fpsmaster.utils.OptifineUtil;

public class MiniMap extends InterfaceModule {
    public MiniMap() {
        super("MiniMap", Category.Interface);
    }

    public static boolean using = false;

    @Override
    public void onEnable() {
        super.onEnable();
        using = true;
        if (OptifineUtil.isFastRender()) {
            OptifineUtil.setFastRender(false);
            NotificationManager.addNotification(
                FPSMaster.i18n.get("minimap.fastrender.disable.title"),
                FPSMaster.i18n.get("minimap.fastrender.disable.title"),
                5000f
            );
        }
    }

    @Override
    public void onDisable() {
        super.onDisable();
        using = false;
    }
}

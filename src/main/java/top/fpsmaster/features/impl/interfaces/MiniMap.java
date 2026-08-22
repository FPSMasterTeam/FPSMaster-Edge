package top.fpsmaster.features.impl.interfaces;

import top.fpsmaster.utils.render.FastRender;
import top.fpsmaster.FPSMaster;
import top.fpsmaster.features.impl.InterfaceModule;
import top.fpsmaster.features.manager.Category;
import top.fpsmaster.ui.notification.NotificationManager;
import top.fpsmaster.prism.overlay.NotificationCenter;
import top.fpsmaster.utils.system.OptifineUtil;

public class MiniMap extends InterfaceModule {
    public MiniMap() {
        super("MiniMap", Category.Interface, InterfaceModule.NONE);
    }

    public static boolean using = false;

    @Override
    public void onEnable() {
        super.onEnable();
        using = true;
        if (FastRender.isActive()) {
            OptifineUtil.setFastRender(false);
            NotificationManager.addNotification(
                FPSMaster.i18n.get("minimap.fastrender.disable.title"),
                FPSMaster.i18n.get("minimap.fastrender.disable.title"),
                NotificationCenter.Type.WARNING,
                5f
            );
        }
    }

    @Override
    public void onDisable() {
        super.onDisable();
        using = false;
    }
}

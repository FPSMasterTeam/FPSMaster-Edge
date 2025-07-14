package top.fpsmaster.features.impl.interfaces;

import org.lwjgl.input.Keyboard;
import top.fpsmaster.FPSMaster;
import top.fpsmaster.event.EventDispatcher;
import top.fpsmaster.event.Subscribe;
import top.fpsmaster.event.events.EventValueChange;
import top.fpsmaster.features.impl.InterfaceModule;
import top.fpsmaster.features.manager.Category;
import top.fpsmaster.features.settings.impl.BindSetting;
import top.fpsmaster.features.settings.impl.BooleanSetting;
import top.fpsmaster.features.settings.impl.TextSetting;
import top.fpsmaster.utils.OptifineUtil;
import top.fpsmaster.utils.Utility;

public class ClientSettings extends InterfaceModule {
    public static BooleanSetting blur = new BooleanSetting("blur", false);
    public static BindSetting keyBind = new BindSetting("ClickGuiKey", Keyboard.KEY_RSHIFT);
    public static BooleanSetting fixedScale = new BooleanSetting("FixedScale", true);
    public static final TextSetting prefix = new TextSetting("prefix", "#");

    public ClientSettings() {
        super("ClientSettings", Category.Utility);
        addSettings(prefix, keyBind, fixedScale, blur);
        EventDispatcher.registerListener(this);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        this.set(false);
    }

    @Subscribe
    public void onValueChange(EventValueChange e){
        if (e.setting == blur && ((boolean) e.newValue)){
            if (OptifineUtil.isFastRender()) {
                Utility.sendClientNotify(FPSMaster.i18n.get("blur.fast_render"));
                e.cancel();
            } else {
                Utility.sendClientNotify(FPSMaster.i18n.get("blur.performance"));
            }
        }
    }
}

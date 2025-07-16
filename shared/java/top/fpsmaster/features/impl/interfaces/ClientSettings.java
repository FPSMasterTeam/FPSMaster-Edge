package top.fpsmaster.features.impl.interfaces;

import org.lwjgl.input.Keyboard;
import top.fpsmaster.FPSMaster;
import top.fpsmaster.event.EventDispatcher;
import top.fpsmaster.event.Subscribe;
import top.fpsmaster.event.events.EventValueChange;
import top.fpsmaster.exception.FileException;
import top.fpsmaster.features.impl.InterfaceModule;
import top.fpsmaster.features.manager.Category;
import top.fpsmaster.features.settings.impl.BindSetting;
import top.fpsmaster.features.settings.impl.BooleanSetting;
import top.fpsmaster.features.settings.impl.ModeSetting;
import top.fpsmaster.features.settings.impl.TextSetting;
import top.fpsmaster.utils.OptifineUtil;
import top.fpsmaster.utils.Utility;

import java.util.Locale;

public class ClientSettings extends InterfaceModule {
    public static ModeSetting language = new ModeSetting("Language", 1, "English", "Chinese");
    public static BooleanSetting blur = new BooleanSetting("blur", false);
    public static BindSetting keyBind = new BindSetting("ClickGuiKey", Keyboard.KEY_RSHIFT);
    public static BooleanSetting fixedScale = new BooleanSetting("FixedScale", false);
    public static BooleanSetting clientCommand = new BooleanSetting("Command", true);
    public static final TextSetting prefix = new TextSetting("prefix", "#", () -> clientCommand.getValue());

    public ClientSettings() {
        super("ClientSettings", Category.Utility);
        addSettings(language, keyBind, fixedScale, blur, clientCommand, prefix);
        EventDispatcher.registerListener(this);
        // get system language
        Locale locale = Locale.getDefault();
        if (locale.getLanguage().equals("zh")) {
            language.setValue(1);
        } else {
            language.setValue(0);
        }
    }

    @Override
    public void onEnable() {
        super.onEnable();
        this.set(false);
    }

    @Subscribe
    public void onValueChange(EventValueChange e) throws FileException {
        if (e.setting == language){
            if (((int) e.newValue) == 1) {
                FPSMaster.i18n.read("zh_cn");
            } else {
                FPSMaster.i18n.read("en_us");
            }
        }

        if (e.setting == blur && ((boolean) e.newValue)) {
            if (OptifineUtil.isFastRender()) {
                Utility.sendClientNotify(FPSMaster.i18n.get("blur.fast_render"));
                e.cancel();
            } else {
                Utility.sendClientNotify(FPSMaster.i18n.get("blur.performance"));
            }
        }
    }
}

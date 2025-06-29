package top.fpsmaster.features.impl.interfaces;

import org.lwjgl.input.Keyboard;
import top.fpsmaster.features.impl.InterfaceModule;
import top.fpsmaster.features.manager.Category;
import top.fpsmaster.features.settings.impl.BindSetting;
import top.fpsmaster.features.settings.impl.BooleanSetting;
import top.fpsmaster.features.settings.impl.TextSetting;

public class ClientSettings extends InterfaceModule {
    public static BooleanSetting blur = new BooleanSetting("blur", false);
    public static BindSetting keyBind = new BindSetting("ClickGuiKey", Keyboard.KEY_RSHIFT);
    public static BooleanSetting fixedScale = new BooleanSetting("FixedScale", true);
    public static final TextSetting prefix = new TextSetting("prefix", "#");

    public ClientSettings() {
        super("ClientSettings", Category.Utility);
        addSettings(prefix, keyBind, fixedScale, blur);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        this.set(false);
    }
}

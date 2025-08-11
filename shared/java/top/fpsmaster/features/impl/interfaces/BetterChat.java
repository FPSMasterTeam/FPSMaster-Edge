package top.fpsmaster.features.impl.interfaces;

import top.fpsmaster.features.impl.InterfaceModule;
import top.fpsmaster.features.manager.Category;
import top.fpsmaster.features.settings.impl.BooleanSetting;

public class BetterChat extends InterfaceModule {
    public static boolean using = false;
    public static final BooleanSetting foldMessage = new BooleanSetting("FoldMessage", false);

    public BetterChat() {
        super("BetterChat", Category.Interface);
        addSettings(foldMessage, backgroundColor, fontShadow, betterFont, bg);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        using = true;
    }

    @Override
    public void onDisable() {
        super.onDisable();
        using = false;
    }
}

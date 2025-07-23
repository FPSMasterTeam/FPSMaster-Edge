package top.fpsmaster.features.impl.interfaces;

import top.fpsmaster.features.impl.InterfaceModule;
import top.fpsmaster.features.manager.Category;
import top.fpsmaster.features.settings.impl.ModeSetting;

public class ItemCountDisplay extends InterfaceModule {

    //TODO: Custom 自定义物品数量，建议添加MultipleSetting，便于让用户自动添加，暂未实现
    public ModeSetting mode = new ModeSetting("mode",0,"potpvp","uhc","custom");

    public ItemCountDisplay() {
        super("ItemCountDisplay", Category.Interface);
        addSettings(mode, bg, backgroundColor, rounded, roundRadius, betterFont, fontShadow, spacing);
    }

}

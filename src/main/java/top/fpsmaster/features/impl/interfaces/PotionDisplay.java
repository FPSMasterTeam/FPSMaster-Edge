package top.fpsmaster.features.impl.interfaces;

import top.fpsmaster.features.impl.InterfaceModule;
import top.fpsmaster.features.manager.Category;
import top.fpsmaster.features.settings.impl.BooleanSetting;
import top.fpsmaster.features.settings.impl.NumberSetting;

public class PotionDisplay extends InterfaceModule {
    public static boolean using = false;
    public static BooleanSetting betterAnimation = new BooleanSetting("BetterAnimation", false);
    public static BooleanSetting noticeableReminder = new BooleanSetting("NoticeableReminder", false);
    public static NumberSetting reminderTime = new NumberSetting("ReminderTime", 20, 1, 120, 1, () -> noticeableReminder.getValue());

    public PotionDisplay() {
        super("PotionDisplay", Category.Interface);
        addSettings(backgroundColor, fontShadow, betterFont, betterAnimation, noticeableReminder, reminderTime, spacing, bg, rounded, roundRadius);
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




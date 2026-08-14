package top.fpsmaster.features.impl.interfaces;

import top.fpsmaster.utils.render.FastRender;
import net.minecraft.client.gui.ScaledResolution;
import org.lwjgl.input.Keyboard;
import top.fpsmaster.FPSMaster;
import top.fpsmaster.event.EventDispatcher;
import top.fpsmaster.event.Subscribe;
import top.fpsmaster.event.events.EventValueChange;
import top.fpsmaster.exception.FileException;
import top.fpsmaster.features.manager.Category;
import top.fpsmaster.features.manager.Module;
import top.fpsmaster.features.settings.impl.BindSetting;
import top.fpsmaster.features.settings.impl.BooleanSetting;
import top.fpsmaster.features.settings.impl.ModeSetting;
import top.fpsmaster.features.settings.impl.TextSetting;
import top.fpsmaster.utils.core.Utility;

import java.util.Locale;

import static top.fpsmaster.utils.core.Utility.mc;

public class ClientSettings extends Module {
    public static ModeSetting language = new ModeSetting("Language", 1, "English", "Chinese");
    public static BooleanSetting blur = new BooleanSetting("blur", false);
    public static BindSetting keyBind = new BindSetting("ClickGuiKey", Keyboard.KEY_RSHIFT);
    public static BooleanSetting followGameScale = new BooleanSetting("FixedScaleEnabled", true);
    private static final double[] SCALE_VALUES = new double[]{
            0.5, 0.75, 1.0, 1.25, 1.5, 2.0, 2.5, 3.0
    };
    public static ModeSetting fixedScale = new ModeSetting(
            "FixedScale",
            2,
            "0.5x", "0.75x", "1x", "1.25x", "1.5x", "2x", "2.5x", "3x"
    );
    public static ModeSetting theme = new ModeSetting("Theme", 0, "Dark", "Light");
    public static BooleanSetting clientCommand = new BooleanSetting("Command", true);
    public static BindSetting zoomBind = new BindSetting("ZoomBind", Keyboard.KEY_LCONTROL);
    public static final TextSetting prefix = new TextSetting("prefix", ".", () -> clientCommand.getValue());
    
    public static boolean isFollowGameScaleEnabled() {
        return followGameScale.getValue();
    }

    public static double getUiScaleMultiplier() {
        int index = fixedScale.getValue();
        if (index < 0 || index >= SCALE_VALUES.length) {
            return 1.0;
        }
        return SCALE_VALUES[index];
    }

    public static int getVanillaGuiScaleFactor() {
        ScaledResolution scaledResolution = new ScaledResolution(mc);
        return scaledResolution.getScaleFactor();
    }

    public static double getUiBaseScale() {
        // uiScale means "backing pixels per logical unit". Follow-game rides the vanilla factor,
        // which is pixel-based and stays visually constant on Retina because the one-time guiScale
        // migration doubles the setting alongside displayWidth. Fixed: "1x" is one window point
        // per unit, so it scales with the backing.
        return isFollowGameScaleEnabled()
                ? getVanillaGuiScaleFactor()
                : top.fpsmaster.utils.system.HiDpi.scale();
    }

    public static double getUiScale() {
        return getUiBaseScale() * getUiScaleMultiplier();
    }

    public static float getUiRenderScale() {
        int vanillaGuiScaleFactor = Math.max(1, getVanillaGuiScaleFactor());
        return (float) (getUiScale() / vanillaGuiScaleFactor);
    }

    public static boolean isZoomBindDown() {
        int zoomKey = zoomBind.getValue();
        if (zoomKey == 0) {
            return false;
        }

        if (Keyboard.isKeyDown(zoomKey)) {
            return true;
        }

        if (zoomKey == Keyboard.KEY_LCONTROL || zoomKey == Keyboard.KEY_RCONTROL) {
            return Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) || Keyboard.isKeyDown(Keyboard.KEY_RCONTROL);
        }
        if (zoomKey == Keyboard.KEY_LSHIFT || zoomKey == Keyboard.KEY_RSHIFT) {
            return Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT);
        }
        if (zoomKey == Keyboard.KEY_LMENU || zoomKey == Keyboard.KEY_RMENU) {
            return Keyboard.isKeyDown(Keyboard.KEY_LMENU) || Keyboard.isKeyDown(Keyboard.KEY_RMENU);
        }

        return false;
    }

    public ClientSettings() {
        super("ClientSettings", Category.Utility);
        addSettings(language, keyBind, followGameScale, fixedScale, blur, theme, zoomBind, clientCommand, prefix);
        // Always-on: language / blur guards must fire whether or not the module "enabled" flag is
        // true in a profile. onEnable/onDisable are no-ops so ConfigManager.set(true) cannot stack
        // a second registration on top of this one.
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
        // Registered once in the constructor; Module.set(true) must not register again.
    }

    @Override
    public void onDisable() {
        // Always-on listener — do not unregister when a profile writes enabled=false.
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
            if (FastRender.isActive()) {
                Utility.sendClientNotify(FPSMaster.i18n.get("blur.fast_render"));
                e.cancel();
            } else {
                Utility.sendClientNotify(FPSMaster.i18n.get("blur.performance"));
            }
        }
    }
}




package top.fpsmaster.features.impl.interfaces;

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
import top.fpsmaster.utils.system.OptifineUtil;
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
        return isFollowGameScaleEnabled() ? getVanillaGuiScaleFactor() : 1.0;
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




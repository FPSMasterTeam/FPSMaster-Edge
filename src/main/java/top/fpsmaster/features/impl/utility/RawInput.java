package top.fpsmaster.features.impl.utility;

import top.fpsmaster.features.manager.Category;
import top.fpsmaster.features.manager.Module;
import top.fpsmaster.utils.input.raw.RawInputMod;

public class RawInput extends Module {
    RawInputMod rawInputMod = new RawInputMod();
    public RawInput() {
        super("RawInput", Category.Utility);
    }

    @Override
    public void onEnable() {
        rawInputMod.start();
    }

    @Override
    public void onDisable() {
        rawInputMod.stop();
    }
}




package top.fpsmaster.modules.client;

import top.fpsmaster.features.impl.utility.NameProtect;

public class GlobalTextFilter {
    // NameProtect.filter is self-synchronizing via volatile state, so no monitor is needed here
    // on this per-frame hot path.
    public static String filter(String text) {
        return NameProtect.filter(text);
    }
}




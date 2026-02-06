package top.fpsmaster.modules.client;

import top.fpsmaster.features.impl.utility.NameProtect;

public class GlobalTextFilter {
    public static synchronized String filter(String text) {
//        if (!IRC.using || !IRC.showMates.getValue()) {
//            return NameProtect.filter(text);
//        }
        return NameProtect.filter(text);
//        StringBuilder result = new StringBuilder(text);
//        result = new StringBuilder(NameProtect.filter(result.toString()));
//        return text;
    }
}

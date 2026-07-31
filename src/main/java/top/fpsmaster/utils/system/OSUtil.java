package top.fpsmaster.utils.system;

import java.util.Locale;

public class OSUtil {

    public static boolean supportShader = true;

    public static boolean isMac() {
        return System.getProperty("os.name").toLowerCase().contains("mac");
    }

    public static boolean isUnix() {
        return System.getProperty("os.name").toLowerCase().contains("nix");
    }

    public static boolean isSolaris() {
        return System.getProperty("os.name").toLowerCase().contains("sunos");
    }

    public static boolean isLinux() {
        return System.getProperty("os.name").toLowerCase().contains("linux");
    }

    public static boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("windows");
    }

    public static boolean isAndroid() {
        return containsIgnoreCase(System.getProperty("os.version"), "android")
                || containsIgnoreCase(System.getProperty("java.runtime.name"), "android");
    }

    private static boolean containsIgnoreCase(String value, String expected) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(expected);
    }

    public static boolean supportShader() {
        return supportShader;
    }
}




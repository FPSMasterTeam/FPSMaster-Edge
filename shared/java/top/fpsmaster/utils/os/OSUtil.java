package top.fpsmaster.utils.os;

public class OSUtil {

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

    public static boolean supportShader() {
        return isWindows() || isLinux();
    }
}

package top.fpsmaster.utils.io;

import java.io.File;

/**
 * The per-user FPSMaster directory: %APPDATA%/FPSMaster on Windows, ~/.fpsmaster elsewhere.
 * Credentials (Microsoft refresh tokens, music logins) live here rather than in the game folder,
 * which players zip up and share together with their configs.
 */
public final class UserDataDir {
    private UserDataDir() {
    }

    public static File get() {
        String appData = System.getenv("APPDATA");
        File dir = appData != null && !appData.isEmpty()
                ? new File(appData, "FPSMaster")
                : new File(System.getProperty("user.home"), ".fpsmaster");
        if (!dir.isDirectory() && !dir.mkdirs() && !dir.isDirectory()) {
            top.fpsmaster.modules.logger.ClientLogger.warn("Cannot create user data directory " + dir);
        }
        return dir;
    }

    public static File file(String name) {
        return new File(get(), name);
    }
}

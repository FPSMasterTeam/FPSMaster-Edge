package top.fpsmaster.utils.system;

import com.sun.jna.Callback;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.win32.StdCallLibrary;
import top.fpsmaster.modules.logger.ClientLogger;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Opens a folder in the system file manager and, on Windows, tries to raise Explorer
 * above the game window. {@link Desktop#open} often succeeds but leaves the window behind
 * Minecraft's exclusive/always-on-top GL surface.
 */
public final class FolderOpen {
    private static final int SW_RESTORE = 9;
    private static final int ASFW_ANY = -1;
    private static final long FOREGROUND_WAIT_MS = 1200L;

    private FolderOpen() {
    }

    public static boolean open(File folder) {
        File target = resolveDirectory(folder);
        if (target == null) {
            return false;
        }
        if (!ensureDirectory(target)) {
            ClientLogger.warn("Failed to create folder: " + target.getAbsolutePath());
            return false;
        }
        if (OSUtil.isWindows()) {
            if (openWindows(target)) {
                return true;
            }
        } else if (OSUtil.isMac()) {
            if (run(new String[] {"open", target.getAbsolutePath()})) {
                return true;
            }
        } else if (run(new String[] {"xdg-open", target.getAbsolutePath()})) {
            return true;
        }
        return desktopBrowse(target) || desktopOpen(target);
    }

    public static boolean ensureDirectory(File folder) {
        if (folder == null) {
            return false;
        }
        if (folder.isDirectory()) {
            return true;
        }
        return folder.mkdirs() && folder.isDirectory();
    }

    static File resolveDirectory(File folder) {
        if (folder == null) {
            return null;
        }
        File target = folder.isFile() ? folder.getParentFile() : folder;
        return target;
    }

    static String[] windowsExplorerArgs(File folder) {
        return new String[] {"explorer.exe", folder.getAbsolutePath()};
    }

    private static boolean openWindows(File folder) {
        String path = folder.getAbsolutePath();
        try {
            new ProcessBuilder(windowsExplorerArgs(folder)).start();
            requestExplorerForeground(path);
            return true;
        } catch (IOException exception) {
            ClientLogger.warn("explorer.exe failed for " + path + ": " + exception.getMessage());
        }
        try {
            // start gives the child its own process group so it can take foreground.
            new ProcessBuilder("cmd", "/c", "start", "", "explorer.exe", path).start();
            requestExplorerForeground(path);
            return true;
        } catch (IOException exception) {
            ClientLogger.warn("cmd start explorer failed for " + path + ": " + exception.getMessage());
        }
        return desktopBrowse(folder) || desktopOpen(folder);
    }

    private static void requestExplorerForeground(final String path) {
        Thread raiser = new Thread(new Runnable() {
            @Override
            public void run() {
                raiseExplorer(path);
            }
        }, "FPSMaster-FolderOpen");
        raiser.setDaemon(true);
        raiser.start();
    }

    private static void raiseExplorer(String path) {
        try {
            User32 user32 = Native.load("user32", User32.class);
            user32.AllowSetForegroundWindow(ASFW_ANY);
            long deadline = System.currentTimeMillis() + FOREGROUND_WAIT_MS;
            while (System.currentTimeMillis() < deadline) {
                Pointer hwnd = findExplorerWindow(user32, path);
                if (hwnd != null) {
                    user32.ShowWindow(hwnd, SW_RESTORE);
                    user32.SetForegroundWindow(hwnd);
                    return;
                }
                Thread.sleep(50L);
            }
        } catch (Throwable exception) {
            ClientLogger.warn("Could not raise Explorer window: " + exception.getMessage());
        }
    }

    private static Pointer findExplorerWindow(User32 user32, String path) {
        final String needle = path == null ? "" : path.toLowerCase(Locale.ROOT);
        final AtomicReference<Pointer> found = new AtomicReference<Pointer>();
        user32.EnumWindows(new User32.WndEnumProc() {
            @Override
            public boolean callback(Pointer hWnd, Pointer lParam) {
                char[] classBuf = new char[64];
                user32.GetClassNameW(hWnd, classBuf, classBuf.length);
                String className = Native.toString(classBuf);
                if (!"CabinetWClass".equals(className) && !"ExploreWClass".equals(className)) {
                    return true;
                }
                if (needle.isEmpty()) {
                    found.set(hWnd);
                    return false;
                }
                char[] titleBuf = new char[512];
                user32.GetWindowTextW(hWnd, titleBuf, titleBuf.length);
                String title = Native.toString(titleBuf).toLowerCase(Locale.ROOT);
                if (title.contains(new File(needle).getName().toLowerCase(Locale.ROOT))) {
                    found.set(hWnd);
                    return false;
                }
                if (found.get() == null) {
                    found.set(hWnd);
                }
                return true;
            }
        }, Pointer.NULL);
        return found.get();
    }

    private static boolean desktopBrowse(File folder) {
        try {
            if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                return false;
            }
            URI uri = folder.toURI();
            Desktop.getDesktop().browse(uri);
            return true;
        } catch (Exception exception) {
            ClientLogger.warn("Desktop.browse failed for " + folder + ": " + exception.getMessage());
            return false;
        }
    }

    private static boolean desktopOpen(File folder) {
        try {
            if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                return false;
            }
            Desktop.getDesktop().open(folder);
            return true;
        } catch (Exception exception) {
            ClientLogger.warn("Desktop.open failed for " + folder + ": " + exception.getMessage());
            return false;
        }
    }

    private static boolean run(String[] command) {
        try {
            new ProcessBuilder(command).start();
            return true;
        } catch (IOException exception) {
            ClientLogger.warn("Failed to run " + command[0] + ": " + exception.getMessage());
            return false;
        }
    }

    interface User32 extends StdCallLibrary {
        boolean AllowSetForegroundWindow(int dwProcessId);

        boolean SetForegroundWindow(Pointer hWnd);

        boolean ShowWindow(Pointer hWnd, int nCmdShow);

        boolean EnumWindows(WndEnumProc lpEnumFunc, Pointer arg);

        int GetClassNameW(Pointer hWnd, char[] lpClassName, int nMaxCount);

        int GetWindowTextW(Pointer hWnd, char[] lpString, int nMaxCount);

        interface WndEnumProc extends Callback {
            boolean callback(Pointer hWnd, Pointer lParam);
        }
    }
}

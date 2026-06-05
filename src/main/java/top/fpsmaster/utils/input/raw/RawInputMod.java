package top.fpsmaster.utils.input.raw;

import net.java.games.input.Controller;
import net.java.games.input.ControllerEnvironment;
import net.java.games.input.Mouse;
import net.minecraft.client.Minecraft;
import net.minecraft.util.MouseHelper;
import top.fpsmaster.modules.logger.ClientLogger;

import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class RawInputMod {
    public static final AtomicInteger dx = new AtomicInteger(0);
    public static final AtomicInteger dy = new AtomicInteger(0);

    private static final AtomicBoolean acceptingInput = new AtomicBoolean(false);

    private Thread inputThread;

    private static ArrayList<Mouse> mouses = new ArrayList<>();
    private static Controller[] controllers;
    private static boolean hasDx8 = false;
    //**Periodically poll activeMouse**
    //26.5.3
    private static Mouse activeMouse = null;

    private static long lastFullScanTime = 0L;
    private static final long FULL_SCAN_INTERVAL = 1000L;

    public void start() {
        try {
            if (inputThread != null && inputThread.isAlive()) {
                return;
            }

            String environment;
            if (checkLibrary("jinput-dx8")) {
                environment = "DirectInputEnvironmentPlugin";
                hasDx8 = true;
            } else if (checkLibrary("jinput-raw")) {
                environment = "DirectAndRawInputEnvironmentPlugin";
                hasDx8 = false;
            } else {
                return;
            }

            Class<?> aClass = Class.forName("net.java.games.input." + environment);
            aClass.getDeclaredConstructor().setAccessible(true);
            controllers = (((ControllerEnvironment) aClass.newInstance())).getControllers();

            clearDeltas();
            acceptingInput.set(false);
            Minecraft.getMinecraft().mouseHelper = new RawMouseHelper();

            inputThread = new Thread(() -> {
                try {
                    while (!Thread.currentThread().isInterrupted()) {
                        initMouses();

                        int totalDx = 0;
                        int totalDy = 0;

                        if (!mouses.isEmpty()) {
                            long now = System.currentTimeMillis();
                            boolean needFullScan = activeMouse == null || now - lastFullScanTime >= FULL_SCAN_INTERVAL;

                            if (needFullScan) {
                                lastFullScanTime = now;

                                for (Mouse mouse : mouses) {
                                    mouse.poll();

                                    int mouseDx = (int) mouse.getX().getPollData();
                                    int mouseDy = (int) mouse.getY().getPollData();

                                    if (mouseDx != 0 || mouseDy != 0) {
                                        activeMouse = mouse;
                                    }

                                    totalDx += mouseDx;
                                    totalDy += mouseDy;

                                    if (hasDx8) {
                                        break;
                                    }
                                }
                            } else {
                                activeMouse.poll();

                                totalDx = (int) activeMouse.getX().getPollData();
                                totalDy = (int) activeMouse.getY().getPollData();
                            }
                        }

                        if (acceptingInput.get() && shouldAcceptGameInput()) {
                            if (totalDx != 0) {
                                dx.addAndGet(totalDx);
                            }

                            if (totalDy != 0) {
                                dy.addAndGet(totalDy);
                            }

                            Thread.sleep(1L);
                        } else {
                            clearDeltas();
                            Thread.sleep(5L);
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (Exception e) {
                    ClientLogger.error("Failed to poll raw input");
                }
            });

            inputThread.setName("inputThread");
            inputThread.setDaemon(true);
            inputThread.start();
        } catch (Exception e) {
            ClientLogger.error("Failed to start raw input");
        }
    }

    private static void initMouses() {
        if (controllers != null && mouses.isEmpty()) {
            for (Controller controller : controllers) {
                if (controller.getType() == Controller.Type.MOUSE) {
                    RawInputMod.mouses.add((Mouse) controller);

                    if (RawInputMod.hasDx8) {
                        break;
                    }
                }
            }
        }
    }

    public void stop() {
        try {
            if (inputThread != null && inputThread.isAlive()) {
                inputThread.interrupt();
                inputThread.join(200L);
            }

            inputThread = null;
            mouses = new ArrayList<>();
            controllers = null;
            activeMouse = null;
            lastFullScanTime = 0L;
            hasDx8 = false;

            acceptingInput.set(false);
            clearDeltas();

            Minecraft.getMinecraft().mouseHelper = new MouseHelper();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            ClientLogger.error("Failed to stop raw input");
        }
    }

    public static void setAcceptingInput(boolean value) {
        acceptingInput.set(value);

        if (!value) {
            clearDeltas();
        }
    }

    public static boolean shouldAcceptGameInput() {
        try {
            Minecraft mc = Minecraft.getMinecraft();
            return mc != null && mc.currentScreen == null && mc.inGameHasFocus;
        } catch (Exception e) {
            return false;
        }
    }

    public static void clearDeltas() {
        dx.set(0);
        dy.set(0);
    }

    public static boolean checkLibrary(String name) {
        try {
            String path = System.getProperty("java.library.path");
            if (path != null) {
                String mapped = System.mapLibraryName(name);
                String[] paths = path.split(File.pathSeparator);
                for (String libPath : paths) {
                    if (new File(libPath, mapped).exists()) {
                        return true;
                    }
                }
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }
}

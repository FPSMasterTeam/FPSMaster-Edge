package top.fpsmaster.utils.input.raw;

import net.java.games.input.Controller;
import net.java.games.input.ControllerEnvironment;
import net.java.games.input.Mouse;
import net.minecraft.client.Minecraft;
import net.minecraft.util.MouseHelper;
import top.fpsmaster.modules.logger.ClientLogger;

import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public class RawInputMod {

    private Thread inputThread;
    public static ArrayList<Mouse> mouses = new ArrayList<>();
    public static Controller[] controllers;
    public static final AtomicInteger dx = new AtomicInteger(0);
    public static final AtomicInteger dy = new AtomicInteger(0);

    public void start() {
        try {
            if (inputThread != null && inputThread.isAlive()) {
                return;
            }
            Minecraft.getMinecraft().mouseHelper = new RawMouseHelper();
            String environment;
            if (checkLibrary("jinput-dx8")) {
                environment = "DirectInputEnvironmentPlugin";
            } else if (checkLibrary("jinput-raw")) {
                environment = "DirectAndRawInputEnvironmentPlugin";
            } else {
                return;
            }
            Class<?> aClass = Class.forName("net.java.games.input." + environment);
            aClass.getDeclaredConstructor().setAccessible(true);
            ControllerEnvironment env = (ControllerEnvironment) aClass.newInstance();
            controllers = env.getControllers();
            inputThread = new Thread(() -> {
                while (!Thread.currentThread().isInterrupted()) {
                    if(environment.equals("DirectInputEnvironmentPlugin")){
                        int i = 0;
                        while (controllers != null && i < controllers.length && mouses.isEmpty()) {
                            if (controllers[i].getType() == Controller.Type.MOUSE) {
                                controllers[i].poll();
                                if (((Mouse) controllers[i]).getX().getPollData() != 0.0 || ((Mouse) controllers[i]).getY().getPollData() != 0.0) {
                                    mouses.add((Mouse) controllers[i]);
                                }
                            }
                            i++;
                        }
                    }else {
                        for (Controller controller : controllers) {
                            if (controller.getType() == Controller.Type.MOUSE) {
                                mouses.add((Mouse) controller);
                            }
                        }
                    }

                    if (!mouses.isEmpty()) {
                        for (Mouse mouse : mouses) {
                            mouse.poll();
                            dx.addAndGet((int) mouse.getX().getPollData());
                            dy.addAndGet((int) mouse.getY().getPollData());
                        }
                        if (Minecraft.getMinecraft().currentScreen != null) {
                            dx.set(0);
                            dy.set(0);
                        }
                    }
                    try {
                        Thread.sleep(1);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            });
            inputThread.setName("inputThread");
            inputThread.setDaemon(true);
            inputThread.start();
        } catch (Exception e) {
            ClientLogger.error("Failed to start raw input");
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
            dx.set(0);
            dy.set(0);
            Minecraft.getMinecraft().mouseHelper = new MouseHelper();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            ClientLogger.error("Failed to stop raw input");
        }
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




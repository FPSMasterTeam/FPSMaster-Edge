package top.fpsmaster.utils.input.raw;

import net.java.games.input.Controller;
import net.java.games.input.ControllerEnvironment;
import net.java.games.input.Mouse;
import net.minecraft.client.Minecraft;
import net.minecraft.util.MouseHelper;

import java.io.File;
import java.util.concurrent.atomic.AtomicInteger;

public class RawInputMod {

    private Thread inputThread;
    public static Mouse mouse = null;
    public static Controller[] controllers;
    public static final AtomicInteger dx = new AtomicInteger(0);
    public static final AtomicInteger dy = new AtomicInteger(0);

    public void start() {
        try {
            Minecraft.getMinecraft().mouseHelper = new RawMouseHelper();
            String environment;
            if (checkLibrary("jinput-dx8")){
                environment = "DirectInputEnvironmentPlugin";
            }else if (checkLibrary("jinput-raw")){
                environment = "DirectAndRawInputEnvironmentPlugin";
            }else{
                return;
            }

            Class<?> aClass = Class.forName("net.java.games.input." + environment);
            aClass.getDeclaredConstructor().setAccessible(true);
            ControllerEnvironment env = (ControllerEnvironment) aClass.newInstance();
            controllers = env.getControllers();
            inputThread = new Thread(() -> {
                while (!Thread.currentThread().isInterrupted()) {
                    int i = 0;
                    while (controllers != null && i < controllers.length && mouse == null) {
                        if (controllers[i].getType() == Controller.Type.MOUSE) {
                            controllers[i].poll();
                            if (((Mouse) controllers[i]).getX().getPollData() != 0.0 || ((Mouse) controllers[i]).getY().getPollData() != 0.0) {
                                mouse = (Mouse) controllers[i];
                            }
                        }
                        i++;
                    }
                    if (mouse != null) {
                        mouse.poll();
                        dx.addAndGet((int) mouse.getX().getPollData());
                        dy.addAndGet((int) mouse.getY().getPollData());
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
            // ignored
        }
    }

    public void stop() {
        try {
            if (inputThread != null && inputThread.isAlive()) {
                inputThread.interrupt();
            }
            inputThread = null;
            Minecraft.getMinecraft().mouseHelper = new MouseHelper();
        } catch (Exception e) {
            // ignored
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




package top.fpsmaster.utils.thirdparty.rawinput;

import net.java.games.input.Controller;
import net.java.games.input.ControllerEnvironment;
import net.java.games.input.Mouse;
import net.minecraft.client.Minecraft;
import net.minecraft.util.MouseHelper;

import java.io.File;

public class RawInputMod {

    private Thread inputThread;
    public static Mouse mouse = null;
    public static Controller[] controllers;
    public static int dx = 0;
    public static int dy = 0;
    private String environment;

    public void start() {
        try {
            Minecraft.getMinecraft().mouseHelper = new RawMouseHelper();
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
                while (true) {
                    int i = 0;
                    while (i < controllers.length && mouse == null) {
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
                        dx += (int) mouse.getX().getPollData();
                        dy += (int) mouse.getY().getPollData();
                        if (Minecraft.getMinecraft().currentScreen != null) {
                            dx = 0;
                            dy = 0;
                        }
                    }
                    try {
                        Thread.sleep(1);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            });
            inputThread.setName("inputThread");
            inputThread.start();
        } catch (Exception e) {
            // ignored
        }
    }

    public void stop() {
        try {
            if (inputThread.isAlive()) {
                inputThread.interrupt();
            }
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

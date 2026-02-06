package top.fpsmaster.forge.api;

import net.minecraft.util.Session;
import net.minecraft.util.Timer;

public interface IMinecraft {
    Timer arch$getTimer();
    Session arch$getSession();
    void arch$setSession(Session session);
    void arch$setLeftClickCounter(int c);
    void arch$setRightClickDelayTimer(int c);
    boolean arch$getRunning();
}




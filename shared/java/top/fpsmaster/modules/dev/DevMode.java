package top.fpsmaster.modules.dev;

import top.fpsmaster.event.EventDispatcher;
import top.fpsmaster.event.Subscribe;
import top.fpsmaster.event.events.EventUpdate;
import top.fpsmaster.modules.lua.LuaManager;
import top.fpsmaster.utils.math.MathTimer;

public class DevMode {
    public static final DevMode INSTACE = new DevMode();
    public boolean dev = false;
    public boolean hotswap = false;

    public void setDev(boolean value) {
        dev = value;
        if (value) {
            EventDispatcher.registerListener(this);
        } else {
            EventDispatcher.unregisterListener(this);
        }
    }

    public void setHotswap(boolean value) {
        hotswap = value;
    }
    MathTimer timer = new MathTimer();

    @Subscribe
    public void onUpdate(EventUpdate e){
        if (timer.delay(1000)){
            LuaManager.hotswap();
        }
    }
}

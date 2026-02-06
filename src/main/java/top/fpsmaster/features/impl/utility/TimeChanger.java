package top.fpsmaster.features.impl.utility;

import top.fpsmaster.event.Subscribe;
import top.fpsmaster.event.events.EventPacket;
import top.fpsmaster.event.events.EventTick;
import top.fpsmaster.features.manager.Category;
import top.fpsmaster.features.manager.Module;
import top.fpsmaster.features.settings.impl.NumberSetting;
import net.minecraft.network.play.server.S03PacketTimeUpdate;
import top.fpsmaster.utils.core.Utility;

public class TimeChanger extends Module {

    private final NumberSetting time;

    public TimeChanger() {
        super("TimeChanger", Category.Utility);
        time = new NumberSetting("Time", 0, 0, 24000, 1);
        addSettings(time);
    }

    @Subscribe
    public void onTick(EventTick e) {
        if (Utility.mc.theWorld != null) {
            Utility.mc.theWorld.setWorldTime(time.getValue().longValue());
        }
    }

    @Subscribe
    public void onPacket(EventPacket e) {
        if (e.type == EventPacket.PacketType.RECEIVE) {
            if (e.packet instanceof S03PacketTimeUpdate) {
                e.cancel();
            }
        }
    }
}




package top.fpsmaster.features.impl.utility;

import top.fpsmaster.event.Subscribe;
import top.fpsmaster.event.events.EventPacket;
import top.fpsmaster.event.events.EventTick;
import top.fpsmaster.features.manager.Category;
import top.fpsmaster.features.manager.Module;
import top.fpsmaster.features.settings.impl.NumberSetting;
import top.fpsmaster.interfaces.ProviderManager;

public class TimeChanger extends Module {

    private final NumberSetting time;

    public TimeChanger() {
        super("TimeChanger", Category.Utility);
        time = new NumberSetting("Time", 0, 0, 24000, 1);
        addSettings(time);
    }

    @Subscribe
    public void onTick(EventTick e) {
        if (ProviderManager.worldClientProvider.getWorld() != null) {
            ProviderManager.worldClientProvider.setWorldTime(time.getValue().longValue());
        }
    }

    @Subscribe
    public void onPacket(EventPacket e) {
        if (e.type == EventPacket.PacketType.RECEIVE) {
            if (ProviderManager.packetTimeUpdate.isPacket(e.packet)) {
                e.cancel();
            }
        }
    }
}

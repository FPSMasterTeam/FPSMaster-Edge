package top.fpsmaster.event.events;

import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.util.ResourceLocation;
import top.fpsmaster.event.CancelableEvent;

import java.util.HashMap;
import java.util.Map;

public class EventCapeLoading extends CancelableEvent {
    Map<String, ResourceLocation> capeCache = new HashMap<>();

    public String playerName;
    public AbstractClientPlayer player;
    public ResourceLocation cape;

    public EventCapeLoading(String playerName, AbstractClientPlayer player) {
        this.playerName = playerName;
        this.player = player;
    }

    public void setCachedCape(String cape) {
        if (capeCache.containsKey(cape)) {
            this.cape = capeCache.get(cape);
        } else {
            this.cape = new ResourceLocation(cape);
            capeCache.put(cape, this.cape);
        }
    }
}

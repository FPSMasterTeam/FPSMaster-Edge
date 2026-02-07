package top.fpsmaster.features.impl.interfaces;

import top.fpsmaster.event.EventDispatcher;
import top.fpsmaster.event.Subscribe;
import top.fpsmaster.event.events.EventMouseClick;
import top.fpsmaster.event.events.EventTick;
import top.fpsmaster.features.impl.InterfaceModule;
import top.fpsmaster.features.manager.Category;
import top.fpsmaster.features.settings.impl.ColorSetting;

import java.awt.*;
import java.util.LinkedList;

public class CPSDisplay extends InterfaceModule {

    private static final LinkedList<Key> KEYS = new LinkedList<>();
    private static final CPSCounterListener COUNTER_LISTENER = new CPSCounterListener();
    private static boolean trackingRegistered = false;

    public CPSDisplay() {
        super("CPSDisplay", Category.Interface);
        ensureTracking();
        addSettings(textColor);
        addSettings(rounded, backgroundColor, fontShadow, betterFont, bg, rounded, roundRadius);
    }

    public static void ensureTracking() {
        if (trackingRegistered) {
            return;
        }
        EventDispatcher.registerListener(COUNTER_LISTENER);
        trackingRegistered = true;
    }

    public static long lcps = 0;
    public static long rcps = 0;
    public static ColorSetting textColor = new ColorSetting("TextColor", new Color(255, 255, 255));

    public static class CPSCounterListener {
        @Subscribe
        public void onClick(EventMouseClick e) {
            if (e.button == 0) {
                KEYS.add(new Key(0, System.currentTimeMillis()));
            } else if (e.button == 1) {
                KEYS.add(new Key(1, System.currentTimeMillis()));
            }
        }

        @Subscribe
        public void onTick(EventTick e) {
            lcps = KEYS.stream()
                    .filter(key -> key.key == 0 && System.currentTimeMillis() - key.time < 1000L)
                    .count();
            rcps = KEYS.stream()
                    .filter(key -> key.key == 1 && System.currentTimeMillis() - key.time < 1000L)
                    .count();
            KEYS.removeIf(key -> System.currentTimeMillis() - key.time > 1000L);
        }
    }

    private static class Key {
        int key;
        long time;

        Key(int key, long time) {
            this.key = key;
            this.time = time;
        }
    }
}




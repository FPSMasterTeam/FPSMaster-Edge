package top.fpsmaster.features.impl.interfaces;

import top.fpsmaster.event.Subscribe;
import top.fpsmaster.event.events.EventMouseClick;
import top.fpsmaster.event.events.EventTick;
import top.fpsmaster.features.impl.InterfaceModule;
import top.fpsmaster.features.manager.Category;
import top.fpsmaster.features.settings.impl.ColorSetting;

import java.awt.*;
import java.util.LinkedList;

public class CPSDisplay extends InterfaceModule {

    private final LinkedList<Key> keys = new LinkedList<>();

    public CPSDisplay() {
        super("CPSDisplay", Category.Interface);
        addSettings(textColor);
        addSettings(rounded, backgroundColor, fontShadow, betterFont, bg, rounded, roundRadius);
    }

    @Subscribe
    public void onClick(EventMouseClick e) {
        if (e.button == 0) {
            keys.add(new Key(0, System.currentTimeMillis()));
        } else if (e.button == 1) {
            keys.add(new Key(1, System.currentTimeMillis()));
        }
    }

    @Subscribe
    public void onTick(EventTick e) {
        lcps = keys.stream()
                .filter(key -> key.key == 0 && System.currentTimeMillis() - key.time < 1000L)
                .count();
        rcps = keys.stream()
                .filter(key -> key.key == 1 && System.currentTimeMillis() - key.time < 1000L)
                .count();
        keys.removeIf(key -> System.currentTimeMillis() - key.time > 1000L);
    }

    public static long lcps = 0;
    public static long rcps = 0;
    public static ColorSetting textColor = new ColorSetting("TextColor", new Color(255, 255, 255));
}

class Key {

    int key;
    long time;

    public Key(int key, long time) {
        this.key = key;
        this.time = time;
    }
}

package top.fpsmaster.features.impl.interfaces;

import top.fpsmaster.features.impl.InterfaceModule;
import top.fpsmaster.features.manager.Category;
import top.fpsmaster.features.settings.impl.ColorSetting;

import java.awt.*;

public class PlayerDisplay extends InterfaceModule {
    public PlayerDisplay() {
        super("PlayerDisplay", Category.Interface);
        // This HUD used to paint a hard-coded translucent black panel instead of going through
        // drawRect, so its background ignored every appearance setting. Now that it uses drawRect,
        // seed the shared setting with that same colour so the look is unchanged but adjustable.
        backgroundColor = new ColorSetting("BackgroundColor", new Color(0, 0, 0, 60), () -> bg.getValue());
    }
}


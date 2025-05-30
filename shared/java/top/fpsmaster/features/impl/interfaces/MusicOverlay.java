package top.fpsmaster.features.impl.interfaces;

import top.fpsmaster.FPSMaster;
import top.fpsmaster.event.Subscribe;
import top.fpsmaster.event.events.EventRender2D;
import top.fpsmaster.features.impl.InterfaceModule;
import top.fpsmaster.features.manager.Category;
import top.fpsmaster.features.settings.impl.ColorSetting;
import top.fpsmaster.features.settings.impl.NumberSetting;
import top.fpsmaster.modules.music.IngameOverlay;
import top.fpsmaster.modules.music.JLayerHelper;
import top.fpsmaster.utils.math.MathTimer;

import java.awt.Color;

public class MusicOverlay extends InterfaceModule {
    public static final NumberSetting amplitude = new NumberSetting("Amplitude", 10, 0, 10, 0.1);
    public static final ColorSetting progressColor = new ColorSetting("ProgressColor", new Color(255, 255, 255, 100));
    public static final ColorSetting color = new ColorSetting("Visual", new Color(255, 255, 255, 100));

    private final MathTimer timer = new MathTimer();

    public MusicOverlay() {
        super("MusicDisplay", Category.Interface);
        addSettings(amplitude, progressColor, color, betterFont, fontShadow);
    }

    @Subscribe
    public void onRender(EventRender2D e) {
        if (timer.delay(50)) {
            FPSMaster.async.runnable(JLayerHelper::updateLoudness);
        }
        IngameOverlay.onRender();
    }
}

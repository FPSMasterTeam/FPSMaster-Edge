package top.fpsmaster.ui.custom.impl;

import net.minecraft.client.Minecraft;
import top.fpsmaster.features.impl.interfaces.FPSDisplay;
import top.fpsmaster.ui.custom.TextComponent;

public class FPSDisplayComponent extends TextComponent {

    public FPSDisplayComponent() {
        super(FPSDisplay.class);
        x = 0.05f;
        y = 0.05f;
        allowScale = true;
    }

    @Override
    protected String text() {
        return Minecraft.getDebugFPS() + "fps";
    }

    @Override
    protected int fontSize() {
        return 18;
    }

    @Override
    protected int textColor() {
        return FPSDisplay.textColor.getRGB();
    }
}

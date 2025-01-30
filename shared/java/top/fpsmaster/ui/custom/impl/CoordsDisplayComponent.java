package top.fpsmaster.ui.custom.impl;

import org.jetbrains.annotations.NotNull;
import top.fpsmaster.FPSMaster;
import top.fpsmaster.features.impl.interfaces.CoordsDisplay;
import top.fpsmaster.features.impl.interfaces.FPSDisplay;
import top.fpsmaster.ui.custom.Component;
import top.fpsmaster.interfaces.ProviderManager;
import top.fpsmaster.wrapper.TextFormattingProvider;

public class CoordsDisplayComponent extends Component {

    public CoordsDisplayComponent() {
        super(CoordsDisplay.class);
        allowScale = true;
    }

    @Override
    public void draw(float x, float y) {
        super.draw(x, y);
        String s = String.format("X:%d Y:%d Z:%d", 
                (int) ProviderManager.mcProvider.getPlayer().posX,
                (int) ProviderManager.mcProvider.getPlayer().posY,
                (int) ProviderManager.mcProvider.getPlayer().posZ);

        if (((CoordsDisplay) mod).limitDisplay.value) {
            String yStr = getString();

            s = String.format("X:%d Y:%d(%s) Z:%d", 
                    (int) ProviderManager.mcProvider.getPlayer().posX, 
                    (int) ProviderManager.mcProvider.getPlayer().posY, 
                    yStr, 
                    (int) ProviderManager.mcProvider.getPlayer().posZ);
        }

        width = getStringWidth(18, s) + 4;
        height = 14f;

        drawRect(x - 2, y, width, height, mod.backgroundColor.getColor());
        drawString(18, s, x, y + 2, FPSDisplay.textColor.getRGB());
    }

    private @NotNull String getString() {
        int restHeight = (int) ((CoordsDisplay) mod).limitDisplayY.value - (int) ProviderManager.mcProvider.getPlayer().posY;
        String yStr = "";

        // color
        if (restHeight < 5) {
            yStr = TextFormattingProvider.getRed() + String.valueOf(restHeight) + TextFormattingProvider.getReset();
        } else if (restHeight < 10) {
            yStr = TextFormattingProvider.getYellow() + String.valueOf(restHeight) + TextFormattingProvider.getReset();
        } else {
            yStr = TextFormattingProvider.getGreen() + String.valueOf(restHeight) + TextFormattingProvider.getReset();
        }
        return yStr;
    }
}

package top.fpsmaster.ui.custom.impl;

import org.jetbrains.annotations.NotNull;
import top.fpsmaster.features.impl.interfaces.CoordsDisplay;
import top.fpsmaster.features.impl.interfaces.FPSDisplay;
import top.fpsmaster.ui.custom.Component;
import net.minecraft.util.EnumChatFormatting;

import static top.fpsmaster.utils.Utility.mc;

public class CoordsDisplayComponent extends Component {

    public CoordsDisplayComponent() {
        super(CoordsDisplay.class);
        allowScale = true;
    }

    @Override
    public void draw(float x, float y) {
        super.draw(x, y);
        String s = String.format("X:%d Y:%d Z:%d",
                (int) mc.thePlayer.posX,
                (int) mc.thePlayer.posY,
                (int) mc.thePlayer.posZ);

        if (((CoordsDisplay) mod).limitDisplay.getValue()) {
            String yStr = getString();

            s = String.format("X:%d Y:%d(%s) Z:%d", 
                    (int) mc.thePlayer.posX, 
                    (int) mc.thePlayer.posY, 
                    yStr, 
                    (int) mc.thePlayer.posZ);
        }

        width = getStringWidth(18, s) + 4;
        height = 14f;

        drawRect(x - 2, y, width, height, mod.backgroundColor.getColor());
        drawString(18, s, x, y + 2, FPSDisplay.textColor.getRGB());
    }

    private @NotNull String getString() {
        int restHeight = ((CoordsDisplay) mod).limitDisplayY.getValue().intValue() - (int) mc.thePlayer.posY;
        String yStr;

        // color
        if (restHeight < 5) {
            yStr = EnumChatFormatting.RED + String.valueOf(restHeight) + EnumChatFormatting.RESET;
        } else if (restHeight < 10) {
            yStr = EnumChatFormatting.YELLOW + String.valueOf(restHeight) + EnumChatFormatting.RESET;
        } else {
            yStr = EnumChatFormatting.GREEN + String.valueOf(restHeight) + EnumChatFormatting.RESET;
        }
        return yStr;
    }
}

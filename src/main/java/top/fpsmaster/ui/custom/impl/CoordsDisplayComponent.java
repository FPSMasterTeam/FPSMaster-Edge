package top.fpsmaster.ui.custom.impl;

import org.jetbrains.annotations.NotNull;
import top.fpsmaster.features.impl.interfaces.CoordsDisplay;
import top.fpsmaster.features.impl.interfaces.FPSDisplay;
import top.fpsmaster.ui.custom.TextComponent;
import net.minecraft.util.EnumChatFormatting;

import static top.fpsmaster.utils.core.Utility.mc;

public class CoordsDisplayComponent extends TextComponent {

    public CoordsDisplayComponent() {
        super(CoordsDisplay.class);
        allowScale = true;
    }

    @Override
    protected String text() {
        if (mc.thePlayer == null) {
            return null;
        }
        if (((CoordsDisplay) mod).limitDisplay.getValue()) {
            return String.format("X:%d Y:%d(%s) Z:%d",
                    (int) mc.thePlayer.posX,
                    (int) mc.thePlayer.posY,
                    getString(),
                    (int) mc.thePlayer.posZ);
        }
        return String.format("X:%d Y:%d Z:%d",
                (int) mc.thePlayer.posX,
                (int) mc.thePlayer.posY,
                (int) mc.thePlayer.posZ);
    }

    @Override
    protected int fontSize() {
        return 18;
    }

    @Override
    protected int textColor() {
        return FPSDisplay.textColor.getRGB();
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

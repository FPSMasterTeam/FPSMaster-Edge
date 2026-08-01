package top.fpsmaster.ui.custom.impl;

import net.minecraft.util.EnumChatFormatting;
import top.fpsmaster.features.impl.interfaces.CPSDisplay;
import top.fpsmaster.ui.custom.TextComponent;

public class CPSDisplayComponent extends TextComponent {

    public CPSDisplayComponent() {
        super(CPSDisplay.class);
        x = 0.05f;
        y = 0.05f;
        allowScale = true;
    }

    @Override
    protected String text() {
        return String.format("CPS: %d%s | %s%d",
                CPSDisplay.lcps,
                EnumChatFormatting.GRAY,
                EnumChatFormatting.RESET,
                CPSDisplay.rcps);
    }

    @Override
    protected int fontSize() {
        return 16;
    }

    @Override
    protected int textColor() {
        return CPSDisplay.textColor.getRGB();
    }
}

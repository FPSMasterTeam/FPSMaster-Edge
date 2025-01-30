package top.fpsmaster.ui.custom.impl;

import top.fpsmaster.FPSMaster;
import top.fpsmaster.features.impl.interfaces.CPSDisplay;
import top.fpsmaster.ui.custom.Component;
import top.fpsmaster.wrapper.TextFormattingProvider;

public class CPSDisplayComponent extends Component {

    public CPSDisplayComponent() {
        super(CPSDisplay.class);
        x = 0.05f;
        y = 0.05f;
        allowScale = true;
    }

    @Override
    public void draw(float x, float y) {
        super.draw(x, y);
        String text = String.format("CPS: %d%s | %s%d", 
                CPSDisplay.lcps, 
                TextFormattingProvider.getGray(), 
                TextFormattingProvider.getReset(), 
                CPSDisplay.rcps);

        width = getStringWidth(16, text) + 4;
        height = 14f;

        drawRect(x - 2, y, width, height, mod.backgroundColor.getColor());
        drawString(16, text, x, y + 2, CPSDisplay.textColor.getRGB());
    }
}

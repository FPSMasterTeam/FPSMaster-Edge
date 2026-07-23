package top.fpsmaster.ui.custom.impl;

import top.fpsmaster.FPSMaster;
import top.fpsmaster.features.impl.interfaces.ClockDisplay;
import top.fpsmaster.ui.custom.Component;

import java.text.SimpleDateFormat;
import java.util.Date;

public class ClockDisplayComponent extends Component {
    public ClockDisplayComponent() {
        super(ClockDisplay.class);
        x = 0.60f;
        y = 0.05f;
        allowScale = true;
    }

    @Override
    public void draw(float x, float y) {
        super.draw(x, y);
        ClockDisplay module = getModule();

        String pattern = module.hour24Mode.getValue() ? "HH:mm" : "hh:mm";
        if (module.showSeconds.getValue()) {
            pattern += ":ss";
        }
        if (!module.hour24Mode.getValue()) {
            pattern += " a";
        }

        String timeStr = new SimpleDateFormat(pattern).format(new Date());
        String text = getLabel(module) + timeStr;

        width = getStringWidth(16, text) + 8;
        height = 16f;

        drawRect(x - 2, y, width, height, mod.backgroundColor.getColor());
        drawString(16, text, x + 2, y + 3, module.textColor.getRGB());
    }

    private String getLabel(ClockDisplay module) {
        String label = module.label.getValue();
        if (label == null || label.trim().isEmpty()) {
            label = FPSMaster.i18n.get("clockdisplay.defaultlabel");
            if ("clockdisplay.defaultlabel".equals(label)) {
                label = "Time: ";
            }
            module.label.setValue(label);
        }
        if (!label.endsWith("：") && !label.endsWith(":") && !label.endsWith(" ")) {
            label += ": ";
        }
        return label;
    }

    private ClockDisplay getModule() {
        return (ClockDisplay) mod;
    }
}

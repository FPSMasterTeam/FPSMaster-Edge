package top.fpsmaster.ui.custom.impl;

import top.fpsmaster.features.impl.utility.Sprint;
import top.fpsmaster.ui.custom.Component;

import static top.fpsmaster.utils.core.Utility.mc;

public class SprintComponent extends Component{
    public SprintComponent() {
        super(Sprint.class);
        allowScale = true;
    }

    @Override
    public void draw(float x, float y) {
        super.draw(x, y);
        String text;
        if (Sprint.sprint) {
            text = "[Sprinting (Toggled)]";
        }else{
            text = "";
            if (mc.thePlayer.isSprinting()){
                text = "[Sprinting (Vanilla)]";
            }
        }
        if (mc.thePlayer.capabilities.isFlying){
            text = "[Flying]";
        }
        drawString(16, text, x, y,-1);
        this.width = getStringWidth(16, text);
        this.height = 12;
    }

    @Override
    public void measurePreview() {
        width = getStringWidth(16, "[Sprinting (Toggled)]");
        height = 12f;
    }

    @Override
    public void drawPreview(float x, float y) {
        drawString(16, "[Sprinting (Toggled)]", x, y, -1);
    }
}



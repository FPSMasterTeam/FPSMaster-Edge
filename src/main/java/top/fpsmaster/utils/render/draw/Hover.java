package top.fpsmaster.utils.render.draw;

import top.fpsmaster.utils.render.types.Bounding;

public class Hover {
    public static boolean is(float x, float y, float width, float height, int mouseX, int mouseY) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    public static boolean is(Bounding bounding, int mouseX, int mouseY) {
        return is(bounding.x, bounding.y, bounding.width, bounding.height, mouseX, mouseY);
    }
}


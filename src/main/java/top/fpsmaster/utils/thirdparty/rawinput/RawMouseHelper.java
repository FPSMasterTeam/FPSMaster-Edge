package top.fpsmaster.utils.thirdparty.rawinput;

import net.minecraft.util.MouseHelper;

public class RawMouseHelper extends MouseHelper {

    @Override
    public void mouseXYChange() {
        deltaX = RawInputMod.dx;
        RawInputMod.dx = 0;
        deltaY = -RawInputMod.dy;
        RawInputMod.dy = 0;
    }
}

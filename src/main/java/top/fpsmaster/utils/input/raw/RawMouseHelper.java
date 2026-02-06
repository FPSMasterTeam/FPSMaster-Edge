package top.fpsmaster.utils.input.raw;

import net.minecraft.util.MouseHelper;

public class RawMouseHelper extends MouseHelper {

    @Override
    public void mouseXYChange() {
        deltaX = RawInputMod.dx.getAndSet(0);
        deltaY = -RawInputMod.dy.getAndSet(0);
    }
}




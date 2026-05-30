package top.fpsmaster.utils.input.raw;

import net.minecraft.util.MouseHelper;

public class RawMouseHelper extends MouseHelper {

    @Override
    public void mouseXYChange() {
        if (!RawInputMod.shouldAcceptGameInput()) {
            RawInputMod.clearDeltas();
            deltaX = 0;
            deltaY = 0;
            return;
        }

        deltaX = RawInputMod.dx.getAndSet(0);
        deltaY = -RawInputMod.dy.getAndSet(0);
    }
}
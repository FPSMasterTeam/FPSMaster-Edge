package top.fpsmaster.ui.minimap.animation;

public class MinimapAnimation
{
    public static long lastTick = System.currentTimeMillis();

    public static void tick() {
        MinimapAnimation.lastTick = System.currentTimeMillis();
    }
    
    public static double animate(double a, final double factor) {
        final double times = (System.currentTimeMillis() - MinimapAnimation.lastTick) / 16.666666666666668;
        a *= Math.pow(factor, times);
        return a;
    }
}

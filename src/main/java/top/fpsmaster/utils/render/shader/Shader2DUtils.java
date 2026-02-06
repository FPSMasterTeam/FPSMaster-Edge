package top.fpsmaster.utils.render.shader;

import top.fpsmaster.utils.core.Utility;

public class Shader2DUtils extends Utility {
    static RainbowShader rainbowShader = new RainbowShader();
    static RainbowFontShader rainbowFontShader = new RainbowFontShader();


    public static void drawRainbow(float x, float y, float offset, Runnable function) {
        rainbowShader.strengthX = x;
        rainbowShader.strengthY = y;
        rainbowShader.offset = offset;
        rainbowShader.startShader();
        function.run();
        rainbowShader.stopShader();
    }

    public static void drawFontRainbow(float x, float y, float offset, Runnable function) {
        rainbowFontShader.strengthX = x;
        rainbowFontShader.strengthY = y;
        rainbowFontShader.offset = offset;
        rainbowFontShader.startShader();
        function.run();
        rainbowFontShader.stopShader();
    }
}



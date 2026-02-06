package top.fpsmaster.utils.render.shader;

import org.lwjgl.opengl.GL20;

public class RainbowFontShader extends Shader {
    public RainbowFontShader() {
        super(Shaders.font_rainbow);
    }

    public float strengthX = 0f;
    public float strengthY = 0f;
    public float offset = 0f;

    @Override
    public void setupUniforms() {
        setupUniform("offset");
        setupUniform("strength");
    }

    @Override
    public void updateUniforms() {
        GL20.glUniform2f(getUniform("strength"), strengthX, strengthY);
        GL20.glUniform1f(getUniform("offset"), offset);
    }
}



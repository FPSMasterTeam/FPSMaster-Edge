package top.fpsmaster.utils.render.shader;

import net.minecraft.client.gui.ScaledResolution;
import org.lwjgl.opengl.GL11;
import top.fpsmaster.modules.logger.ClientLogger;
import top.fpsmaster.utils.system.OptifineUtil;
import top.fpsmaster.utils.core.Utility;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL20.*;

public class ShaderUtil extends Utility {
    private final int programID;

    public ShaderUtil(String shader, String vertexShaderLoc) {
        int program = glCreateProgram();
        int fragmentShaderID;

        switch (shader) {
            case "roundedRect":
                fragmentShaderID = createShader(Shaders.roundedRect, GL_FRAGMENT_SHADER);
                break;
            case "roundedRectGradient":
                fragmentShaderID = createShader(Shaders.roundedRectGradient, GL_FRAGMENT_SHADER);
                break;
            case "blurUp":
                fragmentShaderID = createShader(Shaders.blurUp, GL_FRAGMENT_SHADER);
                break;
            case "blurDown":
                fragmentShaderID = createShader(Shaders.blurDown, GL_FRAGMENT_SHADER);
                break;
            default:
                fragmentShaderID = createShader(shader, GL_FRAGMENT_SHADER);
                break;
        }
        glAttachShader(program, fragmentShaderID);

        int vertexShaderID = createShader(vertexShaderLoc, GL_VERTEX_SHADER);
        glAttachShader(program, vertexShaderID);


        glLinkProgram(program);
        int status = glGetProgrami(program, GL_LINK_STATUS);

        if (status == 0) {
            throw new IllegalStateException("Shader failed to link!");
        }
        this.programID = program;
    }

    public ShaderUtil(String fragmentShaderLoc) {
        this(fragmentShaderLoc, Shaders.vertex);
    }


    public void init() {
        glUseProgram(programID);
    }

    public void unload() {
        glUseProgram(0);
    }

    public void setUniformf(String name, float... args) {
        int loc = glGetUniformLocation(programID, name);
        switch (args.length) {
            case 1:
                glUniform1f(loc, args[0]);
                break;
            case 2:
                glUniform2f(loc, args[0], args[1]);
                break;
            case 3:
                glUniform3f(loc, args[0], args[1], args[2]);
                break;
            case 4:
                glUniform4f(loc, args[0], args[1], args[2], args[3]);
                break;
        }
    }

    public void setUniformi(String name, int... args) {
        int loc = glGetUniformLocation(programID, name);
        if (args.length > 1) glUniform2i(loc, args[0], args[1]);
        else glUniform1i(loc, args[0]);
    }

    public static void drawQuads(float x, float y, float width, float height) {
        if (OptifineUtil.isFastRender()) return;
        glBegin(GL_QUADS);
        glTexCoord2f(0, 0);
        glVertex2f(x, y);
        glTexCoord2f(0, 1);
        glVertex2f(x, y + height);
        glTexCoord2f(1, 1);
        glVertex2f(x + width, y + height);
        glTexCoord2f(1, 0);
        glVertex2f(x + width, y);
        glEnd();
    }

    public static void drawQuads() {
        if (OptifineUtil.isFastRender()) {
            return;
        }
        ScaledResolution sr = new ScaledResolution(mc);
        float width = (float) sr.getScaledWidth_double();
        float height = (float) sr.getScaledHeight_double();

        GL11.glBegin(GL11.GL_QUADS);
        GL11.glTexCoord2f(0f, 1f);
        GL11.glVertex2f(0f, 0f);
        GL11.glTexCoord2f(0f, 0f);
        GL11.glVertex2f(0f, height);
        GL11.glTexCoord2f(1f, 0f);
        GL11.glVertex2f(width, height);
        GL11.glTexCoord2f(1f, 1f);
        GL11.glVertex2f(width, 0f);
        GL11.glEnd();
    }


    private int createShader(String input, int shaderType) {
        int shader = glCreateShader(shaderType);
        glShaderSource(shader, input);
        glCompileShader(shader);


        if (glGetShaderi(shader, GL_COMPILE_STATUS) == 0) {
            ClientLogger.info(glGetShaderInfoLog(shader, 4096));
            throw new IllegalStateException(String.format("Shader (%s) failed to compile!", shaderType));
        }

        return shader;
    }
}



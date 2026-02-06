package top.fpsmaster.utils.render.shader;

import org.apache.commons.io.IOUtils;
import org.lwjgl.opengl.*;
import top.fpsmaster.modules.logger.ClientLogger;
import top.fpsmaster.utils.core.Utility;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public abstract class Shader extends Utility {
    private int program;

    private Map<String, Integer> uniformsMap;

    public Shader(final String shader) {
        int vertexShaderID, fragmentShaderID;

        try {
            final InputStream vertexStream = getClass().getResourceAsStream("/assets/minecraft/client/shader/vertex.vert");
            if (vertexStream == null)
                return;
            vertexShaderID = createShader(IOUtils.toString(vertexStream, StandardCharsets.UTF_8), ARBVertexShader.GL_VERTEX_SHADER_ARB);
            IOUtils.closeQuietly(vertexStream);

            fragmentShaderID = createShader(shader, ARBFragmentShader.GL_FRAGMENT_SHADER_ARB);
        } catch (Exception e) {
            ClientLogger.error("An error occurred while loading shader: " + shader);
            e.printStackTrace();
            return;
        }

        if (vertexShaderID == 0 || fragmentShaderID == 0)
            return;

        program = ARBShaderObjects.glCreateProgramObjectARB();

        if (program == 0)
            return;

        ARBShaderObjects.glAttachObjectARB(program, vertexShaderID);
        ARBShaderObjects.glAttachObjectARB(program, fragmentShaderID);

        ARBShaderObjects.glLinkProgramARB(program);
        ARBShaderObjects.glValidateProgramARB(program);
    }

    public void startShader() {
        GL11.glPushMatrix();
        GL20.glUseProgram(program);

        if (uniformsMap == null) {
            uniformsMap = new HashMap<>();
            setupUniforms();
        }

        updateUniforms();
    }

    public void stopShader() {
        GL20.glUseProgram(0);
        GL11.glPopMatrix();
    }

    public abstract void setupUniforms();

    public abstract void updateUniforms();

    private int createShader(String shaderSource, int shaderType) {
        int shader = 0;

        try {
            shader = ARBShaderObjects.glCreateShaderObjectARB(shaderType);

            if (shader == 0)
                return 0;

            ARBShaderObjects.glShaderSourceARB(shader, shaderSource);
            ARBShaderObjects.glCompileShaderARB(shader);

            if (ARBShaderObjects.glGetObjectParameteriARB(shader, ARBShaderObjects.GL_OBJECT_COMPILE_STATUS_ARB) == GL11.GL_FALSE)
                throw new RuntimeException("Error creating shader: " + getLogInfo(shader));

            return shader;
        } catch (final Exception e) {
            ARBShaderObjects.glDeleteObjectARB(shader);
            throw e;

        }
    }

    private String getLogInfo(int i) {
        return ARBShaderObjects.glGetInfoLogARB(i, ARBShaderObjects.glGetObjectParameteriARB(i, ARBShaderObjects.GL_OBJECT_INFO_LOG_LENGTH_ARB));
    }

    public void setUniform(final String uniformName, final int location) {
        uniformsMap.put(uniformName, location);
    }

    public void setupUniform(final String uniformName) {
        setUniform(uniformName, GL20.glGetUniformLocation(program, uniformName));
    }

    public int getUniform(final String uniformName) {
        return uniformsMap.get(uniformName);
    }

    public int getProgramId() {
        return program;
    }
}



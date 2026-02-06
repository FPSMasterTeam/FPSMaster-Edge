package top.fpsmaster.forge.api;

import com.google.common.collect.Lists;
import net.minecraft.client.shader.Shader;

import java.util.List;

public interface IShaderGroup {

    void load(float partialTicks);

    List<Shader> getListShaders();
}

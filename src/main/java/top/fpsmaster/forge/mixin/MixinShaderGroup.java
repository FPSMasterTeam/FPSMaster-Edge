package top.fpsmaster.forge.mixin;

import net.minecraft.client.shader.Shader;
import net.minecraft.client.shader.ShaderGroup;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import top.fpsmaster.forge.api.IShaderGroup;

import java.util.List;

@Mixin(ShaderGroup.class)
public abstract class MixinShaderGroup implements IShaderGroup {

    @Shadow
    @Final
    private List<Shader> listShaders;

    @Shadow
    public abstract void loadShaderGroup(float partialTicks);

    @Override
    public void load(float partialTicks){
        loadShaderGroup(partialTicks);
    }

    @Override
    public List<Shader> getListShaders() {
        return listShaders;
    }
}

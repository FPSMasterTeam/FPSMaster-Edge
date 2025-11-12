package top.fpsmaster.wrapper;

import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import org.jetbrains.annotations.NotNull;
import top.fpsmaster.api.wrapper.EffectRendererWrap;
import top.fpsmaster.wrapper.blockpos.WrapperBlockPos;

import static top.fpsmaster.utils.Utility.mc;

public class EffectRendererProvider implements EffectRendererWrap {
    @Override
    public void addRedStoneBreak(@NotNull WrapperBlockPos pos) {
        mc.effectRenderer.addBlockDestroyEffects(pos.getPos(), Blocks.REDSTONE_BLOCK.getBlockState().getBaseState());
    }
}

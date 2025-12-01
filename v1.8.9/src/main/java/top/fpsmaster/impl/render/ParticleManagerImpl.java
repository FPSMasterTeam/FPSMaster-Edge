package top.fpsmaster.impl.render;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.EffectRenderer;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumParticleTypes;
import top.fpsmaster.api.domain.render.IParticleManager;
import top.fpsmaster.api.model.BlockPos;

public class ParticleManagerImpl implements IParticleManager {
    
    private final EffectRenderer effectRenderer;
    
    public ParticleManagerImpl() {
        this.effectRenderer = Minecraft.getMinecraft().effectRenderer;
    }
    
    @Override
    public void addRedstoneBreakParticle(BlockPos pos) {
        net.minecraft.util.BlockPos mcPos = new net.minecraft.util.BlockPos(pos.getX(), pos.getY(), pos.getZ());
        IBlockState state = Blocks.redstone_block.getDefaultState();
        effectRenderer.addBlockDestroyEffects(mcPos, state);
    }
    
    @Override
    public void addBlockBreakParticle(BlockPos pos, String blockId) {
        net.minecraft.util.BlockPos mcPos = new net.minecraft.util.BlockPos(pos.getX(), pos.getY(), pos.getZ());
        
        // 尝试从 blockId 获取方块
        Block block = Block.getBlockFromName(blockId);
        if (block == null) {
            block = Blocks.stone;
        }
        
        IBlockState state = block.getDefaultState();
        effectRenderer.addBlockDestroyEffects(mcPos, state);
    }
}

package top.fpsmaster.api.provider.game;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.entity.effect.EntityLightningBolt;
import top.fpsmaster.api.provider.IProvider;
import top.fpsmaster.wrapper.blockpos.WrapperBlockPos;
import top.fpsmaster.wrapper.util.WrapperAxisAlignedBB;

public interface IWorldClientProvider extends IProvider {
    IBlockState getBlockState(WrapperBlockPos pos);
    Block getBlock(WrapperBlockPos pos);
    WrapperAxisAlignedBB getBlockBoundingBox(WrapperBlockPos pos, IBlockState state);
    void addWeatherEffect(EntityLightningBolt entityLightningBolt);
    WorldClient getWorld();
    void setWorldTime(long l);
}
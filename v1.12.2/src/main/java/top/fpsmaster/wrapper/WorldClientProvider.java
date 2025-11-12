package top.fpsmaster.wrapper;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.entity.effect.EntityLightningBolt;
import top.fpsmaster.api.wrapper.WorldWrap;
import top.fpsmaster.wrapper.blockpos.WrapperBlockPos;
import top.fpsmaster.wrapper.util.WrapperAxisAlignedBB;

public class WorldClientProvider implements WorldWrap {
    public static WorldClient getWorldClient() {
        return Minecraft.getMinecraft().world;
    }

    @Override
    public IBlockState getBlockState(WrapperBlockPos pos) {
        return getWorldClient().getBlockState(pos.getPos());
    }

    @Override
    public Block getBlock(WrapperBlockPos pos) {
        return getWorldClient().getBlockState(pos.getPos()).getBlock();
    }

    @Override
    public WrapperAxisAlignedBB getBlockBoundingBox(WrapperBlockPos pos, IBlockState state) {
        return new WrapperAxisAlignedBB(state.getBoundingBox(getWorld(), pos.getPos()));
    }

    @Override
    public void addWeatherEffect(EntityLightningBolt entityLightningBolt) {
        getWorldClient().addWeatherEffect(entityLightningBolt);
    }

    @Override
    public WorldClient getWorld() {
        return getWorldClient();
    }

    @Override
    public void setWorldTime(long l) {
        getWorldClient().setWorldTime(l);
    }
}

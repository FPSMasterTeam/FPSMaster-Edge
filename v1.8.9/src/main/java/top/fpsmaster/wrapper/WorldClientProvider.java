package top.fpsmaster.wrapper;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.entity.effect.EntityLightningBolt;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import top.fpsmaster.api.wrapper.WorldWrap;
import top.fpsmaster.wrapper.blockpos.WrapperBlockPos;
import top.fpsmaster.wrapper.util.WrapperAxisAlignedBB;

public class WorldClientProvider implements WorldWrap {

    public IBlockState getBlockState(WrapperBlockPos pos) {
        return Minecraft.getMinecraft().theWorld.getBlockState(pos.getPos());
    }

    public Block getBlock(WrapperBlockPos pos) {
        return Minecraft.getMinecraft().theWorld.getBlockState(pos.getPos()).getBlock();
    }

    public WrapperAxisAlignedBB getBlockBoundingBox(WrapperBlockPos pos, IBlockState state) {
        return new WrapperAxisAlignedBB(new AxisAlignedBB(getBlock(pos).getBlockBoundsMinX(), getBlock(pos).getBlockBoundsMinY(), getBlock(pos).getBlockBoundsMinZ(), getBlock(pos).getBlockBoundsMaxX(), getBlock(pos).getBlockBoundsMaxY(), getBlock(pos).getBlockBoundsMaxZ()));
    }


    public void addWeatherEffect(EntityLightningBolt entityLightningBolt) {
        Minecraft.getMinecraft().theWorld.addWeatherEffect(entityLightningBolt);
    }

    public WorldClient getWorld() {
        return Minecraft.getMinecraft().theWorld;
    }

    public void setWorldTime(long l) {
        Minecraft.getMinecraft().theWorld.setWorldTime(l);
    }
}

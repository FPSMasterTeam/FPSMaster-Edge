package top.fpsmaster.impl.world;

import net.minecraft.block.Block;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.util.BlockPos;
import top.fpsmaster.api.domain.world.IWorld;
import top.fpsmaster.api.model.WeatherType;

public class WorldImpl implements IWorld {
    
    private final WorldClient world;
    
    public WorldImpl(WorldClient world) {
        this.world = world;
    }
    
    @Override
    public String getWorldName() {
        return world.getWorldInfo().getWorldName();
    }
    
    @Override
    public long getWorldTime() {
        return world.getWorldTime();
    }
    
    @Override
    public void setWorldTime(long time) {
        world.setWorldTime(time);
    }
    
    @Override
    public String getBlockId(top.fpsmaster.api.model.BlockPos pos) {
        BlockPos mcPos = new BlockPos(pos.getX(), pos.getY(), pos.getZ());
        Block block = world.getBlockState(mcPos).getBlock();
        return Block.blockRegistry.getNameForObject(block).toString();
    }
    
    @Override
    public boolean isAirBlock(top.fpsmaster.api.model.BlockPos pos) {
        BlockPos mcPos = new BlockPos(pos.getX(), pos.getY(), pos.getZ());
        return world.isAirBlock(mcPos);
    }
    
    @Override
    public top.fpsmaster.api.model.BlockPos getSpawnPoint() {
        BlockPos spawn = world.getSpawnPoint();
        return new top.fpsmaster.api.model.BlockPos(spawn.getX(), spawn.getY(), spawn.getZ());
    }
    
    @Override
    public WeatherType getWeatherType() {
        if (world.isThundering()) {
            return WeatherType.THUNDER;
        } else if (world.isRaining()) {
            return WeatherType.RAIN;
        }
        return WeatherType.CLEAR;
    }
    
    @Override
    public float getRainStrength() {
        return world.getRainStrength(1.0f);
    }


    @Override
    public int getLightLevel(top.fpsmaster.api.model.BlockPos pos) {
        net.minecraft.util.BlockPos mcPos = new net.minecraft.util.BlockPos(pos.getX(), pos.getY(), pos.getZ());
        return world.getLight(mcPos);
    }
    
    @Override
    public int getSkyLightLevel(top.fpsmaster.api.model.BlockPos pos) {
        net.minecraft.util.BlockPos mcPos = new net.minecraft.util.BlockPos(pos.getX(), pos.getY(), pos.getZ());
        return world.getLightFromNeighborsFor(net.minecraft.world.EnumSkyBlock.SKY, mcPos);
    }
    
    @Override
    public int getBlockLightLevel(top.fpsmaster.api.model.BlockPos pos) {
        net.minecraft.util.BlockPos mcPos = new net.minecraft.util.BlockPos(pos.getX(), pos.getY(), pos.getZ());
        return world.getLightFromNeighborsFor(net.minecraft.world.EnumSkyBlock.BLOCK, mcPos);
    }
    
    @Override
    public Object getRawWorld() {
        return world;
    }
}

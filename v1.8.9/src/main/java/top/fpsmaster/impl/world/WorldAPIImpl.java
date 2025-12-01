package top.fpsmaster.impl.world;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import org.jetbrains.annotations.Nullable;
import top.fpsmaster.api.domain.world.IWorld;
import top.fpsmaster.api.domain.world.IWorldAPI;

public class WorldAPIImpl implements IWorldAPI {
    
    @Override
    @Nullable
    public IWorld getWorld() {
        WorldClient mcWorld = Minecraft.getMinecraft().theWorld;
        if (mcWorld == null) return null;
        return new WorldImpl(mcWorld);
    }
}

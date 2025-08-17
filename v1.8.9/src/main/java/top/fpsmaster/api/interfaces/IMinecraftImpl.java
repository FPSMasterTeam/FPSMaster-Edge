package top.fpsmaster.api.interfaces;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;

public class IMinecraftImpl implements IMinecraft{
    private final Minecraft mc;
    private IPlayerImpl player;
    private EntityPlayerSP mcPlayer;

    public IMinecraftImpl(Minecraft mc) {
        this.mc = mc;
    }

    @Override
    public IPlayer getPlayer() {
        if (player == null || mcPlayer != mc.thePlayer){
            player = new IPlayerImpl(mc.thePlayer);
            mcPlayer = mc.thePlayer;
        }
        return player;
    }
}

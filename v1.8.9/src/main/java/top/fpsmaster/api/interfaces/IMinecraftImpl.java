package top.fpsmaster.api.interfaces;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import org.jetbrains.annotations.Nullable;

public class IMinecraftImpl implements IMinecraft{
    private final Minecraft mc;
    private IPlayerImpl player;
    private EntityPlayerSP mcPlayer;

    public IMinecraftImpl(Minecraft mc) {
        this.mc = mc;
    }

    @Override
    public @Nullable IPlayer getPlayer() {
        if (player == null || mcPlayer != mc.thePlayer) {
            if (mc.thePlayer == null) {
                return null; // Better keep the nullability.
            }
            player = new IPlayerImpl(mc.thePlayer);
            mcPlayer = mc.thePlayer;
        }
        return player;
    }
}

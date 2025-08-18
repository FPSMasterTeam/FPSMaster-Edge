package top.fpsmaster.api.interfaces;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import org.jetbrains.annotations.Nullable;
import top.fpsmaster.api.interfaces.client.IMinecraft;
import top.fpsmaster.api.interfaces.client.IClientPlayerEntity;

public class IMinecraftImpl implements IMinecraft {
    private final Minecraft mc;
    private IClientPlayerEntityImpl player;
    private EntityPlayerSP mcPlayer;

    public IMinecraftImpl(Minecraft mc) {
        this.mc = mc;
    }

    @Override
    public @Nullable IClientPlayerEntity getClientPlayer() {
        if (player == null || mcPlayer != mc.thePlayer) {
            if (mc.thePlayer == null) {
                return null; // Better keep the nullability.
            }
            player = new IClientPlayerEntityImpl(mc.thePlayer);
            mcPlayer = mc.thePlayer;
        }
        return player;
    }
}

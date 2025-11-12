package top.fpsmaster.wrapper;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.SoundHandler;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Session;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import top.fpsmaster.forge.api.IMinecraft;
import top.fpsmaster.api.wrapper.MinecraftWrap;

import java.io.File;
import java.util.Collection;

public class MinecraftProvider implements MinecraftWrap {
    public File getGameDir() {
        return Minecraft.getMinecraft().mcDataDir;
    }

    public FontRenderer getFontRenderer() {
        return Minecraft.getMinecraft().fontRendererObj;
    }

    public EntityPlayerSP getPlayer() {
        return Minecraft.getMinecraft().thePlayer;
    }

    public boolean isHoveringOverBlock() {
        return Minecraft.getMinecraft().objectMouseOver != null && Minecraft.getMinecraft().objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK;
    }

    public ItemStack getPlayerHeldItem() {
        return Minecraft.getMinecraft().thePlayer.getHeldItem();
    }

    public WorldClient getWorld() {
        return Minecraft.getMinecraft().theWorld;
    }

    public ItemStack[] getArmorInventory() {
        return getPlayer().inventory.armorInventory;
    }

    public void setSession(Session mojang) {
        ((IMinecraft) Minecraft.getMinecraft()).arch$setSession(mojang);
    }

    public Integer getRespondTime() {
        if (Minecraft.getMinecraft().isSingleplayer())
            return 0;
        return Minecraft.getMinecraft().getNetHandler().getPlayerInfo(Minecraft.getMinecraft().thePlayer.getUniqueID()).getResponseTime();
    }
    public void drawString(String text, float x, float y, int color) {
        Minecraft.getMinecraft().fontRendererObj.drawString(text, (int) x, (int) y, color);
    }
    @NotNull
    public String getServerAddress() {
        if (Minecraft.getMinecraft().isSingleplayer())
            return "localhost";
        return Minecraft.getMinecraft().getNetHandler().getNetworkManager().getRemoteAddress().toString();
    }
    public void removeClickDelay() {
        ((IMinecraft) Minecraft.getMinecraft()).arch$setLeftClickCounter(0);
    }

    @Override
    public void printChatMessage(Object message) {
        Minecraft.getMinecraft().ingameGUI.getChatGUI().printChatMessage((IChatComponent) message);
    }

    @Override
    public Collection<NetworkPlayerInfo> getPlayerInfoMap() {
        return Minecraft.getMinecraft().getNetHandler().getPlayerInfoMap();
    }

    @Nullable
    @Override
    public Object getCurrentScreen() {
        return Minecraft.getMinecraft().currentScreen;
    }
}

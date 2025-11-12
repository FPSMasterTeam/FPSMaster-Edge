package top.fpsmaster.api.wrapper;

import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Session;

import java.io.File;
import java.util.Collection;

public interface MinecraftWrap {
    Object getCurrentScreen();
    File getGameDir();
    FontRenderer getFontRenderer();
    EntityPlayerSP getPlayer();
    boolean isHoveringOverBlock();
    ItemStack getPlayerHeldItem();
    WorldClient getWorld();
    ItemStack[] getArmorInventory();
    void setSession(Session mojang);
    Integer getRespondTime();
    void drawString(String text, float x, float y, int color);
    String getServerAddress();
    void removeClickDelay();
    void printChatMessage(Object message);
    Collection<NetworkPlayerInfo> getPlayerInfoMap();
}
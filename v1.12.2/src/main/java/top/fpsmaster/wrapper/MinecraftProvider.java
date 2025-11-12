package top.fpsmaster.wrapper;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraft.util.Session;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.text.ITextComponent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import top.fpsmaster.forge.api.IMinecraft;
import top.fpsmaster.api.wrapper.MinecraftWrap;

import java.io.File;
import java.util.Collection;
import java.util.Objects;

public class MinecraftProvider implements MinecraftWrap {

    @Override
    public File getGameDir() {
        return Minecraft.getMinecraft().gameDir;
    }


    @Override
    public FontRenderer getFontRenderer() {
        return Minecraft.getMinecraft().fontRenderer;
    }


    @Override
    public EntityPlayerSP getPlayer() {
        return Minecraft.getMinecraft().player;
    }

    @Override
    public boolean isHoveringOverBlock() {
        return Minecraft.getMinecraft().objectMouseOver != null && Minecraft.getMinecraft().objectMouseOver.typeOfHit == RayTraceResult.Type.BLOCK;
    }

    @Override
    public ItemStack getPlayerHeldItem() {
        return getPlayer().getHeldItem(EnumHand.MAIN_HAND);
    }

    @Override
    public WorldClient getWorld() {
        return Minecraft.getMinecraft().world;
    }

    @Override
    public ItemStack[] getArmorInventory() {
        return getPlayer().inventory.armorInventory.toArray(new ItemStack[0]);
    }

    @Override
    public void setSession(Session mojang) {
        ((IMinecraft) Minecraft.getMinecraft()).arch$setSession(mojang);
    }

    @Override
    public Integer getRespondTime() {
        NetHandlerPlayClient connection = Minecraft.getMinecraft().getConnection();
        if (connection == null || getPlayer() == null)
            return -1;
        if (Minecraft.getMinecraft().isSingleplayer())
            return 0;
        return connection.getPlayerInfo(getPlayer().getUniqueID()).getResponseTime();
    }

    @Override
    public void drawString(String text, float x, float y, int color) {
        Minecraft.getMinecraft().fontRenderer.drawStringWithShadow(text, (int) x, (int) y, color);
    }

    @Override
    public String getServerAddress() {
        if (Minecraft.getMinecraft().getConnection() == null || Minecraft.getMinecraft().isSingleplayer())
            return "localhost";
        return Minecraft.getMinecraft().getConnection().getNetworkManager().getRemoteAddress().toString();
    }

    @Override
    public Collection<NetworkPlayerInfo> getPlayerInfoMap() {
        return Objects.requireNonNull(Minecraft.getMinecraft().getConnection()).getPlayerInfoMap();
    }

    @Override
    public void removeClickDelay() {
        ((IMinecraft) Minecraft.getMinecraft()).arch$setLeftClickCounter(0);
//        Minecraft.getMinecraft().rightClickDelayTimer = 0;
    }

    @Override
    public void printChatMessage(@NotNull Object message) {
        Minecraft.getMinecraft().ingameGUI.getChatGUI().printChatMessage((ITextComponent) message);
    }

    @Nullable
    @Override
    public Object getCurrentScreen() {
        return Minecraft.getMinecraft().currentScreen;
    }
}

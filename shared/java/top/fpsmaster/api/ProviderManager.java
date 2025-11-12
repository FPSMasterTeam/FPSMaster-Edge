package top.fpsmaster.api;

import top.fpsmaster.api.provider.client.IConstantsProvider;
import top.fpsmaster.api.provider.game.*;
import top.fpsmaster.api.provider.gui.IGuiIngameProvider;
import top.fpsmaster.api.provider.gui.IGuiMainMenuProvider;
import top.fpsmaster.api.provider.gui.IGuiNewChatProvider;
import top.fpsmaster.api.provider.packets.IAddPlayerData;
import top.fpsmaster.api.provider.packets.IPacketChat;
import top.fpsmaster.api.provider.packets.IPacketPlayerList;
import top.fpsmaster.api.provider.packets.IPacketTimeUpdate;
import top.fpsmaster.api.provider.render.IEffectRendererProvider;
import top.fpsmaster.api.provider.render.IRenderManagerProvider;
import top.fpsmaster.api.provider.sound.ISoundProvider;
import top.fpsmaster.api.wrapper.packets.PlayerListItemAddWrap;
import top.fpsmaster.wrapper.*;
import top.fpsmaster.wrapper.packets.SPacketChatProvider;
import top.fpsmaster.wrapper.packets.SPacketPlayerListProvider;
import top.fpsmaster.wrapper.packets.SPacketTimeUpdateProvider;
import top.fpsmaster.wrapper.packets.WrapperAddPlayerData;
import top.fpsmaster.wrapper.sound.SoundProvider;

import java.util.List;

public class ProviderManager {
    public static final IConstantsProvider constants = new IConstantsProvider() {
        private final Constants impl = new Constants();
        public String getVersion() { return impl.getVersion(); }
        public String getEdition() { return impl.getEdition(); }
    };

    public static final IUtilityProvider utilityProvider = new IUtilityProvider() {
        private final UtilityProvider impl = new UtilityProvider();
        public String getResourcePath(net.minecraft.util.ResourceLocation rl) { return impl.getResourcePath(rl); }
        public double getDistanceToEntity(net.minecraft.entity.Entity e1, net.minecraft.entity.Entity e2) { return impl.getDistanceToEntity(e1, e2); }
        public boolean isItemEnhancementEmpty(net.minecraft.item.ItemStack i) { return impl.isItemEnhancementEmpty(i); }
        public int getPotionIconIndex(net.minecraft.potion.PotionEffect effect) { return impl.getPotionIconIndex(effect); }
        public Object makeChatComponent(String msg) { return impl.makeChatComponent(msg); }
    };

    public static final ILegacyMinecraftProvider mcProvider = new ILegacyMinecraftProvider() {
        private final MinecraftProvider impl = new MinecraftProvider();
        public Object getCurrentScreen() { return impl.getCurrentScreen(); }
        public java.io.File getGameDir() { return impl.getGameDir(); }
        public net.minecraft.client.gui.FontRenderer getFontRenderer() { return impl.getFontRenderer(); }
        public net.minecraft.client.entity.EntityPlayerSP getPlayer() { return impl.getPlayer(); }
        public boolean isHoveringOverBlock() { return impl.isHoveringOverBlock(); }
        public net.minecraft.item.ItemStack getPlayerHeldItem() { return impl.getPlayerHeldItem(); }
        public net.minecraft.client.multiplayer.WorldClient getWorld() { return impl.getWorld(); }
        public net.minecraft.item.ItemStack[] getArmorInventory() { return impl.getArmorInventory(); }
        public void setSession(net.minecraft.util.Session mojang) { impl.setSession(mojang); }
        public Integer getRespondTime() { return impl.getRespondTime(); }
        public void drawString(String text, float x, float y, int color) { impl.drawString(text, x, y, color); }
        public String getServerAddress() { return impl.getServerAddress(); }
        public void removeClickDelay() { impl.removeClickDelay(); }
        public void printChatMessage(Object message) { impl.printChatMessage(message); }
        public java.util.Collection<net.minecraft.client.network.NetworkPlayerInfo> getPlayerInfoMap() { return impl.getPlayerInfoMap(); }
    };

    public static final IGuiMainMenuProvider mainmenuProvider = new IGuiMainMenuProvider() {
        private final GuiMainMenuProvider impl = new GuiMainMenuProvider();
        public void initGui() { impl.initGui(); }
        public void renderSkybox(int mouseX, int mouseY, float partialTicks, int width, int height, float zLevel) { impl.renderSkybox(mouseX, mouseY, partialTicks, width, height, zLevel); }
        public void showSinglePlayer(net.minecraft.client.gui.GuiScreen screen) { impl.showSinglePlayer(screen); }
    };


    public static final IWorldClientProvider worldClientProvider = new IWorldClientProvider() {
        private final WorldClientProvider impl = new WorldClientProvider();
        public net.minecraft.block.state.IBlockState getBlockState(top.fpsmaster.wrapper.blockpos.WrapperBlockPos pos) { return impl.getBlockState(pos); }
        public net.minecraft.block.Block getBlock(top.fpsmaster.wrapper.blockpos.WrapperBlockPos pos) { return impl.getBlock(pos); }
        public top.fpsmaster.wrapper.util.WrapperAxisAlignedBB getBlockBoundingBox(top.fpsmaster.wrapper.blockpos.WrapperBlockPos pos, net.minecraft.block.state.IBlockState state) { return impl.getBlockBoundingBox(pos, state); }
        public void addWeatherEffect(net.minecraft.entity.effect.EntityLightningBolt entityLightningBolt) { impl.addWeatherEffect(entityLightningBolt); }
        public net.minecraft.client.multiplayer.WorldClient getWorld() { return impl.getWorld(); }
        public void setWorldTime(long l) { impl.setWorldTime(l); }
    };

    public static final ITimerProvider timerProvider = new ITimerProvider() {
        private final TimerProvider impl = new TimerProvider();
        public float getRenderPartialTicks() { return impl.getRenderPartialTicks(); }
    };

    public static final IRenderManagerProvider renderManagerProvider = new IRenderManagerProvider() {
        private final RenderManagerProvider impl = new RenderManagerProvider();
        public double renderPosX() { return impl.renderPosX(); }
        public double renderPosY() { return impl.renderPosY(); }
        public double renderPosZ() { return impl.renderPosZ(); }
    };

    public static final IGameSettings gameSettings = new IGameSettings() {
        private final GameSettingsProvider impl = new GameSettingsProvider();
        public void setKeyPress(net.minecraft.client.settings.KeyBinding key, boolean value) { impl.setKeyPress(key, value); }
    };

    // Packets
    public static final IPacketChat packetChat = new IPacketChat() {
        private final SPacketChatProvider impl = new SPacketChatProvider();
        public String getUnformattedText(Object packet) { return impl.getUnformattedText(packet); }
        public net.minecraft.util.IChatComponent getChatComponent(Object packet) { return impl.getChatComponent(packet); }
        public int getType(Object p) { return impl.getType(p); }
        public void appendTranslation(Object p) { impl.appendTranslation(p); }
        public boolean isPacket(Object packet) { return impl.isPacket(packet); }
    };

    public static final IPacketPlayerList packetPlayerList = new IPacketPlayerList() {
        private final SPacketPlayerListProvider impl = new SPacketPlayerListProvider();
        public boolean isPacket(Object packet) { return impl.isPacket(packet); }
        public boolean isActionJoin(Object p) { return impl.isActionJoin(p); }
        public boolean isActionLeave(Object p) { return impl.isActionLeave(p); }
        public List<PlayerListItemAddWrap> getEntries(Object p) {
            List<PlayerListItemAddWrap> entries = impl.getEntries(p);
            return entries;
        }
    };

    public static final IPacketTimeUpdate packetTimeUpdate = new IPacketTimeUpdate() {
        private final SPacketTimeUpdateProvider impl = new SPacketTimeUpdateProvider();
        public boolean isPacket(Object packet) { return impl.isPacket(packet); }
    };

    public static final IGuiIngameProvider guiIngameProvider = new IGuiIngameProvider() {
        private final GuiIngameProvider impl = new GuiIngameProvider();
        public void drawHealth(net.minecraft.entity.Entity entityIn) { impl.drawHealth(entityIn); }
    };

    public static final ISoundProvider soundProvider = new ISoundProvider() {
        private final SoundProvider impl = new SoundProvider();
        public void playLightning(double posX, double posY, double posZ, float i, float v, boolean b) { impl.playLightning(posX, posY, posZ, i, v, b); }
        public void playExplosion(double posX, double posY, double posZ, float i, float v, boolean b) { impl.playExplosion(posX, posY, posZ, i, v, b); }
        public void playRedStoneBreak(double posX, double posY, double posZ, float i, float v, boolean b) { impl.playRedStoneBreak(posX, posY, posZ, i, v, b); }
    };

    public static final IEffectRendererProvider effectManager = new IEffectRendererProvider() {
        private final EffectRendererProvider impl = new EffectRendererProvider();
        public void addRedStoneBreak(top.fpsmaster.wrapper.blockpos.WrapperBlockPos pos) { impl.addRedStoneBreak(pos); }
    };
}
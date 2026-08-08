package top.fpsmaster.ui.mc;

import com.google.common.base.Charsets;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.base64.Base64;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.multiplayer.GuiConnecting;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.ResourceLocation;
import org.apache.commons.lang3.Validate;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import top.fpsmaster.FPSMaster;
import top.fpsmaster.font.impl.UFontRenderer;
import top.fpsmaster.utils.render.draw.Hover;
import top.fpsmaster.utils.render.draw.Images;

import java.awt.image.BufferedImage;
import java.net.UnknownHostException;
import java.util.List;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;

public class ServerListEntry {
    private static final Logger logger = LogManager.getLogger();
    private static final ThreadPoolExecutor field_148302_b = new ScheduledThreadPoolExecutor(5, (new ThreadFactoryBuilder()).setNameFormat("Server Pinger #%d").setDaemon(true).build());
    private static final ResourceLocation UNKNOWN_SERVER = new ResourceLocation("textures/misc/unknown_server.png");
    private final Minecraft mc;
    private final ServerData server;
    private final ResourceLocation serverIcon;
    private String field_148299_g;
    private DynamicTexture field_148305_h;
    private final GuiMultiplayer owner;
    private long lastClick = 0;

    protected ServerListEntry(GuiMultiplayer multiplayer, ServerData p_i45048_2_) {
        this.owner = multiplayer;
        this.server = p_i45048_2_;
        this.mc = Minecraft.getMinecraft();
        this.serverIcon = new ResourceLocation("servers/" + p_i45048_2_.serverIP + "/icon");
        this.field_148305_h = (DynamicTexture) this.mc.getTextureManager().getTexture(this.serverIcon);
    }


    public void drawEntry(int slotIndex, int x, int y, int listWidth, int mouseX, int mouseY) {
        if (!this.server.field_78841_f) {
            this.server.field_78841_f = true;
            this.server.pingToServer = -2L;
            this.server.serverMOTD = "";
            this.server.populationInfo = "";

            field_148302_b.submit(() -> {
                try {
                    logger.info("PINGDBG ping start " + ServerListEntry.this.server.serverIP + " entry=" + System.identityHashCode(ServerListEntry.this));
                    owner.oldServerPinger.ping(ServerListEntry.this.server);
                    logger.info("PINGDBG ping returned " + ServerListEntry.this.server.serverIP + " ping=" + ServerListEntry.this.server.pingToServer
                            + " motd=[" + ServerListEntry.this.server.serverMOTD + "] pop=[" + ServerListEntry.this.server.populationInfo + "]");
                } catch (UnknownHostException var2) {
                    ServerListEntry.this.server.pingToServer = -1L;
                    ServerListEntry.this.server.serverMOTD = EnumChatFormatting.DARK_RED + "Can't resolve hostname";
                    logger.info("PINGDBG ping UnknownHost " + ServerListEntry.this.server.serverIP);
                } catch (Exception var3) {
                    ServerListEntry.this.server.pingToServer = -1L;
                    ServerListEntry.this.server.serverMOTD = EnumChatFormatting.DARK_RED + "Can't connect to server.";
                    logger.info("PINGDBG ping exception " + ServerListEntry.this.server.serverIP + " " + var3);
                }

            });
        }
        UFontRenderer title = FPSMaster.fontManager.s18;
        UFontRenderer text = FPSMaster.fontManager.s16;

        boolean flag = this.server.version > 47;
        boolean flag1 = this.server.version < 47;
        boolean flag2 = flag || flag1;
        title.drawString(this.server.serverName, x + 32 + 3 + 10, y + 1 + 10, -1);
        List<String> list = text.listFormattedStringToWidth(this.server.serverMOTD, listWidth - 48 - 2);

        for (int i = 0; i < Math.min(list.size(), 2); ++i) {
            text.drawString(list.get(i), x + 32 + 3 + 10, y + 12 + 10 + this.mc.fontRendererObj.FONT_HEIGHT * i, -1);
        }

        String s2 = flag2 ? EnumChatFormatting.DARK_RED + this.server.gameVersion : this.server.populationInfo;
        int j = text.getStringWidth(s2);
        text.drawString(s2, x + listWidth - j - 15 - 2 - 5, y + 1 + 5, -1);
        int k = 0;
        int l;
        String s1;
        if (flag2) {
            l = 5;
            s1 = flag ? "Client out of date!" : "Server out of date!";
        } else if (this.server.field_78841_f && this.server.pingToServer != -2L) {
            if (this.server.pingToServer < 0L) {
                l = 5;
            } else if (this.server.pingToServer < 150L) {
                l = 0;
            } else if (this.server.pingToServer < 300L) {
                l = 1;
            } else if (this.server.pingToServer < 600L) {
                l = 2;
            } else if (this.server.pingToServer < 1000L) {
                l = 3;
            } else {
                l = 4;
            }

            if (this.server.pingToServer < 0L) {
                s1 = "(no connection)";
            } else {
                s1 = this.server.pingToServer + "ms";
            }
        } else {
            k = 1;
            l = (int) (Minecraft.getSystemTime() / 100L + (slotIndex * 2L) & 7L);
            if (l > 4) {
                l = 8 - l;
            }

            s1 = "Pinging...";
        }

//        this.mc.getTextureManager().bindTexture(Gui.icons);
        Images.drawUV(Gui.icons, x + listWidth - 18, y + 7, k * 10, 176 + l * 8, 10, 8, 256, 256,-1,false);

//        Gui.drawModalRectWithCustomSizedTexture(UiScale.scale(x + listWidth - 15 - 5), UiScale.scale(y + 5), (float) (k * 10), (float) (176 + l * 8), 10, 8, 256.0F, 256.0F);
        if (this.server.getBase64EncodedIconData() != null && !this.server.getBase64EncodedIconData().equals(this.field_148299_g)) {
            this.field_148299_g = this.server.getBase64EncodedIconData();
            this.prepareServerIcon();
            owner.saveServerList();
        }

        if (this.field_148305_h != null) {
            this.drawTextureAt(x + 10, y + 10, this.serverIcon);
        } else {
            this.drawTextureAt(x + 10, y + 10, UNKNOWN_SERVER);
        }
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
//        int i1 = mouseX - x;
//        int j1 = mouseY - y;
//        String tooltip = FMLClientHandler.instance().enhanceServerListEntry(this, this.server, x, listWidth, y, i1, j1);
//        if (tooltip != null) {
//            this.owner.setHoveringText(tooltip);
//        } else if (i1 >= listWidth - 15 && i1 <= listWidth - 5 && j1 >= 0 && j1 <= 8) {
//            this.owner.setHoveringText(s1);
//        } else if (i1 >= listWidth - j - 15 - 2 && i1 <= listWidth - 15 - 2 && j1 >= 0 && j1 <= 8) {
//            this.owner.setHoveringText(s);
//        }

        if (Hover.is(x + listWidth - 18, y + 7, 10, 8, mouseX, mouseY)) {
            text.drawString(s1, mouseX + 6, mouseY - 6, -1);
        }
    }

    protected void drawTextureAt(int p_178012_1_, int p_178012_2_, ResourceLocation p_178012_3_) {
        Images.draw(p_178012_3_, p_178012_1_, p_178012_2_, 32, 32);
    }

    @SuppressWarnings("VulnerableCodeUsages")
    private void prepareServerIcon() {
        if (this.server.getBase64EncodedIconData() == null) {
            this.mc.getTextureManager().deleteTexture(this.serverIcon);
            this.field_148305_h = null;
        } else {
            ByteBuf bytebuf = Unpooled.copiedBuffer(this.server.getBase64EncodedIconData(), Charsets.UTF_8);
            ByteBuf bytebuf1 = Base64.decode(bytebuf);

            BufferedImage bufferedimage;
            label80:
            {
                try {
                    bufferedimage = TextureUtil.readBufferedImage(new ByteBufInputStream(bytebuf1));
                    Validate.validState(bufferedimage.getWidth() == 64, "Must be 64 pixels wide");
                    Validate.validState(bufferedimage.getHeight() == 64, "Must be 64 pixels high");
                    break label80;
                } catch (Throwable throwable) {
                    logger.error("Invalid icon for server {} ({})", this.server.serverName, this.server.serverIP, throwable);
                    this.server.setBase64EncodedIconData(null);
                } finally {
                    bytebuf.release();
                    bytebuf1.release();
                }

                return;
            }

            if (this.field_148305_h == null) {
                this.field_148305_h = new DynamicTexture(bufferedimage.getWidth(), bufferedimage.getHeight());
                this.mc.getTextureManager().loadTexture(this.serverIcon, this.field_148305_h);
            }

            bufferedimage.getRGB(0, 0, bufferedimage.getWidth(), bufferedimage.getHeight(), this.field_148305_h.getTextureData(), 0, bufferedimage.getWidth());
            this.field_148305_h.updateDynamicTexture();
        }

    }

    public ServerData getServerData() {
        return this.server;
    }

    public void triggerClick() {
        if (Minecraft.getSystemTime() - lastClick < 250L) {
            this.mc.displayGuiScreen(new GuiConnecting(owner, this.mc, getServerData()));
        }
        lastClick = Minecraft.getSystemTime();
    }
}





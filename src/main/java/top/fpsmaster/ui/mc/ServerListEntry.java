package top.fpsmaster.ui.mc;

import com.google.common.base.Charsets;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.base64.Base64;
import net.minecraft.client.Minecraft;
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
import top.fpsmaster.ui.click.ClickGuiTheme;
import top.fpsmaster.ui.click.UiChrome;
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
    /** Backend-featured row: drawn with the accent "featured" badge, delete dismisses locally. */
    private final boolean promoted;
    /** Player-pinned own server: drawn with the neutral "pinned" chip, sorted to the very top. */
    private final boolean pinned;
    private final String promotedDescription;

    protected ServerListEntry(GuiMultiplayer multiplayer, ServerData server) {
        this(multiplayer, server, false, false, null);
    }

    protected ServerListEntry(GuiMultiplayer multiplayer, ServerData server, boolean promoted,
                              boolean pinned, String promotedDescription) {
        this.owner = multiplayer;
        this.server = server;
        this.promoted = promoted;
        this.pinned = pinned;
        this.promotedDescription = promotedDescription;
        this.mc = Minecraft.getMinecraft();
        this.serverIcon = new ResourceLocation("servers/" + server.serverIP + "/icon");
        this.field_148305_h = (DynamicTexture) this.mc.getTextureManager().getTexture(this.serverIcon);
    }


    /** Draws one prototype-style server row: icon · name/MOTD · players + ping bars. */
    public void drawEntry(int x, int y, int listWidth, int rowHeight, int mouseX, int mouseY) {
        if (!this.server.field_78841_f) {
            this.server.field_78841_f = true;
            this.server.pingToServer = -2L;
            this.server.serverMOTD = "";
            this.server.populationInfo = "";

            field_148302_b.submit(() -> {
                try {
                    owner.oldServerPinger.ping(ServerListEntry.this.server);
                } catch (UnknownHostException var2) {
                    ServerListEntry.this.server.pingToServer = -1L;
                    ServerListEntry.this.server.serverMOTD = EnumChatFormatting.DARK_RED + "Can't resolve hostname";
                } catch (Exception var3) {
                    ServerListEntry.this.server.pingToServer = -1L;
                    ServerListEntry.this.server.serverMOTD = EnumChatFormatting.DARK_RED + "Can't connect to server.";
                }
            });
        }
        if (this.server.getBase64EncodedIconData() != null && !this.server.getBase64EncodedIconData().equals(this.field_148299_g)) {
            this.field_148299_g = this.server.getBase64EncodedIconData();
            this.prepareServerIcon();
            owner.saveServerList();
        }

        float iconSize = 19f;
        float iconY = y + (rowHeight - iconSize) / 2f;
        drawIcon(x + 5f, iconY, iconSize);

        boolean unreachable = this.server.field_78841_f && this.server.pingToServer == -1L;
        boolean versionMismatch = this.server.version != 47 && this.server.pingToServer >= 0L;

        float textX = x + 5f + iconSize + 5f;
        float rightW = 44f;
        float textW = listWidth - (textX - x) - rightW - 5f;
        UFontRenderer nameFont = FPSMaster.fontManager.s14;
        UFontRenderer subFont = FPSMaster.fontManager.getFont(12);
        // Two distinct markers next to the name: featured rows get the accent badge pill,
        // player-pinned rows get the neutral bordered chip. Both are existing theme widgets.
        String markerText = promoted
                ? FPSMaster.i18n.get("multiplayer.promoted.badge")
                : pinned ? FPSMaster.i18n.get("multiplayer.pinned.badge") : null;
        float nameW = textW;
        if (markerText != null) {
            nameW -= UiChrome.keyChipWidth(markerText) + 4f;
        }
        String name = nameFont.trimStringToWidth(this.server.serverName, nameW);
        nameFont.drawString(name, textX, y + 5f,
                (unreachable ? ClickGuiTheme.textSecondary() : ClickGuiTheme.textPrimary()).getRGB());
        if (markerText != null) {
            float markerX = textX + nameFont.getStringWidth(name) + 4f;
            if (promoted) {
                UiChrome.badge(markerX, y + 3.5f, markerText);
            } else {
                UiChrome.keyChip(markerX, y + 3.5f, UiChrome.keyChipWidth(markerText), 10f, markerText, false, false);
            }
        }
        String motd = this.server.serverMOTD == null ? "" : this.server.serverMOTD.replaceAll("§.", "");
        if (motd.isEmpty() && promoted && promotedDescription != null) {
            motd = promotedDescription;
        }
        if (unreachable) {
            subFont.drawString(subFont.trimStringToWidth(motd, textW), textX, y + 15f,
                    ClickGuiTheme.danger().getRGB());
        } else {
            subFont.drawString(subFont.trimStringToWidth(motd, textW), textX, y + 15f,
                    ClickGuiTheme.textSecondary().getRGB());
        }

        // right column: players over ping bars
        String players = versionMismatch
                ? this.server.gameVersion
                : (this.server.populationInfo == null || this.server.populationInfo.isEmpty() ? "—" : this.server.populationInfo);
        float pw = subFont.getStringWidth(players);
        subFont.drawString(players, x + listWidth - 7f - pw, y + 5f,
                (versionMismatch ? ClickGuiTheme.danger() : ClickGuiTheme.textSecondary()).getRGB());

        long ping = this.server.pingToServer;
        float barsX = x + listWidth - 7f - 8.25f;
        float barsBaseline = y + rowHeight - 6f;
        if (ping == -2L) {
            // still pinging: sweeping animation over dim bars
            int frame = (int) (Minecraft.getSystemTime() / 150L % 5L);
            for (int i = 0; i < 4; i++) {
                float h = 2f + i;
                java.awt.Color c = i == frame - 1 ? ClickGuiTheme.textSecondary() : ClickGuiTheme.layerActive();
                top.fpsmaster.utils.render.draw.Rects.rounded(barsX + i * 2.25f, barsBaseline - h, 1.5f, h, 1, c.getRGB(), false);
            }
        } else {
            UiChrome.pingBars(barsX, barsBaseline, UiChrome.pingLevel(ping), UiChrome.pingColor(ping));
        }
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();

        if (ping > 0 && Hover.is(barsX - 2, barsBaseline - 8, 12, 10, mouseX, mouseY)) {
            subFont.drawString(ping + "ms", mouseX + 4, mouseY - 5, ClickGuiTheme.textPrimary().getRGB());
        }
    }

    public void drawIcon(float x, float y, float size) {
        ResourceLocation icon = field_148305_h != null ? serverIcon : UNKNOWN_SERVER;
        Images.draw(icon, x, y, size, size);
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

    public boolean isPromoted() {
        return this.promoted;
    }

    public void triggerClick() {
        if (Minecraft.getSystemTime() - lastClick < 250L) {
            this.mc.displayGuiScreen(new GuiConnecting(owner, this.mc, getServerData()));
        }
        lastClick = Minecraft.getSystemTime();
    }
}





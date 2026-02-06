package top.fpsmaster.ui.mc;

import top.fpsmaster.utils.render.draw.Hover;
import top.fpsmaster.utils.render.draw.Rects;

import com.google.common.collect.Lists;
import net.minecraft.client.gui.GuiScreenAddServer;
import net.minecraft.client.gui.GuiScreenServerList;
import net.minecraft.client.gui.GuiYesNo;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.network.OldServerPinger;
import net.minecraft.client.resources.I18n;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.fml.client.FMLClientHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.opengl.GL11;
import top.fpsmaster.FPSMaster;
import top.fpsmaster.font.impl.UFontRenderer;
import top.fpsmaster.ui.click.component.ScrollContainer;
import top.fpsmaster.ui.common.GuiButton;
import top.fpsmaster.ui.screens.mainmenu.MainMenu;
import top.fpsmaster.utils.math.MathTimer;
import top.fpsmaster.utils.render.gui.ScaledGuiScreen;
import top.fpsmaster.utils.render.gui.Backgrounds;
import top.fpsmaster.utils.render.gui.Scissor;

import java.awt.*;
import java.io.File;
import java.util.List;

public class GuiMultiplayer extends ScaledGuiScreen {

    private ServerData selectedServer;
    private static final Logger logger = LogManager.getLogger();
    private final List<ServerData> servers = Lists.newArrayList();
    private final List<ServerListEntry> serverListDisplay = Lists.newArrayList();
    private final List<ServerListEntry> serverListInternet = Lists.newArrayList();
    public final OldServerPinger oldServerPinger = new OldServerPinger();

    String action = "";

    MathTimer timer = new MathTimer();

    GuiButton join = new GuiButton("加入服务器", () -> {
        if (selectedServer == null)
            return;
        FMLClientHandler.instance().connectToServer(this, selectedServer);
    }, new Color(0, 0, 0, 140), new Color(113, 127, 254));
    GuiButton connect = new GuiButton("直接连接", () -> {
        this.mc.displayGuiScreen(new GuiScreenServerList(this, this.selectedServer = new ServerData(I18n.format("selectServer.defaultName"), "", false)));
        action = "connect";
    }, new Color(0, 0, 0, 140), new Color(113, 127, 254));
    GuiButton add = new GuiButton("添加服务器", () -> {
        action = "add";
        this.mc.displayGuiScreen(new GuiScreenAddServer(this, this.selectedServer = new ServerData(I18n.format("selectServer.defaultName"), "", false)));
    }, new Color(0, 0, 0, 140), new Color(113, 127, 254));
    GuiButton edit = new GuiButton("编辑", () -> {
        if (selectedServer == null)
            return;
        action = "edit";
        mc.displayGuiScreen(new GuiScreenAddServer(this, selectedServer));
    }, new Color(0, 0, 0, 140), new Color(113, 127, 254));
    GuiButton remove = new GuiButton("删除", () -> {
        if (selectedServer == null)
            return;
        action = "remove";
        String s4 = selectedServer.serverName;
        if (s4 != null) {
            String s = I18n.format("selectServer.deleteQuestion");
            String s1 = "'" + s4 + "' " + I18n.format("selectServer.deleteWarning");
            String s2 = I18n.format("selectServer.deleteButton");
            String s3 = I18n.format("gui.cancel");
            GuiYesNo guiyesno = new GuiYesNo(this, s, s1, s2, s3, servers.indexOf(selectedServer));
            this.mc.displayGuiScreen(guiyesno);
        }
    }, new Color(0, 0, 0, 140), new Color(113, 127, 254));
    GuiButton refresh = new GuiButton("刷新", () -> mc.displayGuiScreen(new GuiMultiplayer()), new Color(0, 0, 0, 140), new Color(113, 127, 254));
    GuiButton back = new GuiButton("返回", () -> mc.displayGuiScreen(new MainMenu()), new Color(0, 0, 0, 140), new Color(113, 127, 254));


    @Override
    public void initGui() {
        super.initGui();
        loadServerList();
        serverListInternet.clear();
        for (ServerData server : servers) {
            this.serverListInternet.add(new ServerListEntry(this, server));
        }
        serverListDisplay.clear();
        serverListDisplay.addAll(serverListInternet);
    }

    @Override
    public void confirmClicked(boolean result, int id) {
        super.confirmClicked(result, id);

        if (result) {
            switch (action) {
                case "add":
                    servers.add(selectedServer);
                    saveServerList();
                    selectedServer = null;
                    break;
                case "edit":
                    saveServerList();
                    break;
                case "remove":
                    servers.remove(selectedServer);
                    saveServerList();
                    break;
                case "connect":
                    FMLClientHandler.instance().connectToServer(this, selectedServer);
                    break;
            }
            action = "";
        }
        mc.displayGuiScreen(this);

    }

    public void saveServerList() {
        try {
            NBTTagList nBTTagList = new NBTTagList();

            for (ServerData serverData : this.servers) {
                nBTTagList.appendTag(serverData.getNBTCompound());
            }

            NBTTagCompound nBTTagCompound = new NBTTagCompound();
            nBTTagCompound.setTag("servers", nBTTagList);
            CompressedStreamTools.safeWrite(nBTTagCompound, new File(this.mc.mcDataDir, "servers.dat"));
        } catch (Exception exception) {
            logger.error("Couldn't save server list", exception);
        }

    }

    ScrollContainer scrollContainer = new ScrollContainer();
    @Override
    public void render(int mouseX, int mouseY, float partialTicks) {
        super.render(mouseX, mouseY, partialTicks);
        Backgrounds.draw((int) guiWidth, (int) guiHeight, mouseX, mouseY, partialTicks, (int) zLevel);

        UFontRenderer title = FPSMaster.fontManager.s22;
        UFontRenderer font = FPSMaster.fontManager.s18;
        title.drawCenteredString("多人游戏", guiWidth / 2f, 16, -1);

        Rects.rounded((guiWidth - 180) / 2f, 30, 180, 24, 3, new Color(0, 0, 0, 80).getRGB());
        Rects.rounded((guiWidth - 176) / 2f, 32, 176, 20, 3, -1);
        FPSMaster.fontManager.s16.drawCenteredString("服务器列表", guiWidth / 2f, 36, new Color(50, 50, 50).getRGB());

        GL11.glPushMatrix();
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        Scissor.apply((guiWidth - 400) / 2f, 60f, 400f, guiHeight - 120, scaleFactor);
        scrollContainer.draw((guiWidth - 400) / 2f, 60, 396, guiHeight - 120, mouseX, mouseY, () -> {
            float y = 70 + scrollContainer.getScroll();
            Rects.rounded((guiWidth - 400) / 2f, y - 10, 400, guiHeight - y, 5, new Color(0, 0, 0, 100).getRGB());
            for (ServerListEntry server : serverListDisplay) {
                if (server.getServerData() == null) {
                    return;
                }
                Rects.rounded((guiWidth - 340) / 2f, y, 340, 54, new Color(0, 0, 0, 120));
                if (Hover.is((guiWidth - 340) / 2f, y, 340, 54, mouseX, mouseY)) {
                    Rects.rounded((guiWidth - 340) / 2f, y, 340, 54, new Color(0, 0, 0, 50));
                }

                if (selectedServer != null && selectedServer == server.getServerData()) {
                    Rects.rounded((guiWidth - 340) / 2f, y, 340, 54, new Color(255, 255, 255, 50));
                }
                server.drawEntry(0, (int) ((guiWidth - 340) / 2), (int) y, 340, 54, mouseX, mouseY, false);
                y += 58;
            }
            scrollContainer.setHeight(y - 50 - scrollContainer.getScroll());
        });
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
        GL11.glPopMatrix();


        join.render((guiWidth - 400) / 2f + 20, guiHeight - 56, 380f / 3 - 20, 20, mouseX, mouseY);
        connect.render((guiWidth - 400) / 2f + 20 + 380f / 3, guiHeight - 56, 380f / 3 - 20, 20, mouseX, mouseY);
        add.render((guiWidth - 400) / 2f + 20 + 380f / 3 * 2, guiHeight - 56, 380f / 3 - 20, 20, mouseX, mouseY);

        edit.render((guiWidth - 400) / 2f + 20, guiHeight - 26, 380f / 4 - 20, 20, mouseX, mouseY);
        remove.render((guiWidth - 400) / 2f + 20 + 380f / 4, guiHeight - 26, 380f / 4 - 20, 20, mouseX, mouseY);
        refresh.render((guiWidth - 400) / 2f + 20 + 380f / 4 * 2, guiHeight - 26, 380f / 4 - 20, 20, mouseX, mouseY);
        back.render((guiWidth - 400) / 2f + 20 + 380f / 4 * 3, guiHeight - 26, 380f / 4 - 20, 20, mouseX, mouseY);
    }


    @Override
    public void updateScreen() {
        super.updateScreen();
        FMLClientHandler.instance().setupServerList();
        this.oldServerPinger.pingPendingNetworks();

    }

    @Override
    public void onGuiClosed() {
        super.onGuiClosed();
        this.oldServerPinger.clearPendingNetworks();
    }

    @Override
    public void onClick(int mouseX, int mouseY, int mouseButton) {
        super.onClick(mouseX, mouseY, mouseButton);
        join.mouseClick(mouseX, mouseY, mouseButton);
        connect.mouseClick(mouseX, mouseY, mouseButton);
        add.mouseClick(mouseX, mouseY, mouseButton);
        edit.mouseClick(mouseX, mouseY, mouseButton);
        remove.mouseClick(mouseX, mouseY, mouseButton);
        refresh.mouseClick(mouseX, mouseY, mouseButton);
        back.mouseClick(mouseX, mouseY, mouseButton);


        float y = 70 + scrollContainer.getScroll();

        for (ServerListEntry server : serverListDisplay) {
            if (server.getServerData() == null) {
                return;
            }
            if (Hover.is((guiWidth - 340) / 2f, y, 340, 54, mouseX, mouseY)) {
                if (selectedServer != server.getServerData()) {
                    selectedServer = server.getServerData();
                    timer.reset();
                } else {
                    if (timer.delay(200)) {
                        selectedServer = null;
                    } else {
                        FMLClientHandler.instance().connectToServer(this, selectedServer);
                    }
                }
            }
            y += 58;
        }


    }


    public void loadServerList() {
        try {
            this.servers.clear();
            NBTTagCompound nBTTagCompound = CompressedStreamTools.read(new File(this.mc.mcDataDir, "servers.dat"));
            if (nBTTagCompound == null) {
                return;
            }

            NBTTagList nBTTagList = nBTTagCompound.getTagList("servers", 10);

            for (int i = 0; i < nBTTagList.tagCount(); ++i) {
                this.servers.add(ServerData.getServerDataFromNBTCompound(nBTTagList.getCompoundTagAt(i)));
            }
        } catch (Exception exception) {
            logger.error("Couldn't load server list", exception);
        }

    }
}





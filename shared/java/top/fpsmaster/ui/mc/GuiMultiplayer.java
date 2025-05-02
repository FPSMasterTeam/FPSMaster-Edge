package top.fpsmaster.ui.mc;

import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.gui.*;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.ServerList;
import net.minecraft.client.network.LanServerDetector;
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
import top.fpsmaster.interfaces.ProviderManager;
import top.fpsmaster.modules.client.AsyncTask;
import top.fpsmaster.ui.click.component.ScrollContainer;
import top.fpsmaster.ui.common.GuiButton;
import top.fpsmaster.ui.screens.mainmenu.MainMenu;
import top.fpsmaster.utils.os.HttpRequest;
import top.fpsmaster.utils.render.Render2DUtils;
import top.fpsmaster.utils.render.ScaledGuiScreen;
import top.fpsmaster.utils.thirdparty.github.UpdateChecker;
import top.fpsmaster.wrapper.ChatFormattingProvider;

import java.awt.*;
import java.io.File;
import java.util.List;

public class GuiMultiplayer extends ScaledGuiScreen {

    private ServerData selectedServer;
    private static final Logger logger = LogManager.getLogger();
    private final List<ServerData> servers = Lists.newArrayList();
    private final List<ServerListEntry> serverListDisplay = Lists.newArrayList();
    private final List<ServerListEntry> serverListInternet = Lists.newArrayList();
    private static List<ServerListEntry> serverListRecommended = Lists.newArrayList();
    public final OldServerPinger oldServerPinger = new OldServerPinger();

    private Gson gson = new GsonBuilder().setPrettyPrinting().create();

    String action = "";

    GuiButton join = new GuiButton("加入服务器", () -> {
        if (selectedServer == null)
            return;
        FMLClientHandler.instance().connectToServer(this, selectedServer);
    }, new Color(0, 0, 0, 140), new Color(113, 127, 254));
    GuiButton connect = new GuiButton("直接连接", () -> {
        this.mc.displayGuiScreen(new GuiScreenServerList(this, this.selectedServer = new ServerData(I18n.format("selectServer.defaultName", new Object[0]), "", false)));
        action = "connect";
    }, new Color(0, 0, 0, 140), new Color(113, 127, 254));
    GuiButton add = new GuiButton("添加服务器", () -> {
        action = "add";
        this.mc.displayGuiScreen(new GuiScreenAddServer(this, this.selectedServer = new ServerData(I18n.format("selectServer.defaultName", new Object[0]), "", false)));
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
            String s = I18n.format("selectServer.deleteQuestion", new Object[0]);
            String s1 = "'" + s4 + "' " + I18n.format("selectServer.deleteWarning", new Object[0]);
            String s2 = I18n.format("selectServer.deleteButton", new Object[0]);
            String s3 = I18n.format("gui.cancel", new Object[0]);
            GuiYesNo guiyesno = new GuiYesNo(this, s, s1, s2, s3, servers.indexOf(selectedServer));
            this.mc.displayGuiScreen(guiyesno);
        }
    }, new Color(0, 0, 0, 140), new Color(113, 127, 254));
    GuiButton refresh = new GuiButton("刷新", () -> {
        mc.displayGuiScreen(new GuiMultiplayer());
    }, new Color(0, 0, 0, 140), new Color(113, 127, 254));
    GuiButton back = new GuiButton("返回", () -> {
        mc.displayGuiScreen(new MainMenu());
    }, new Color(0, 0, 0, 140), new Color(113, 127, 254));


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
        if (serverListRecommended.size() == 0) {
            AsyncTask asyncTask = new AsyncTask(100);
            asyncTask.runnable(() -> {
                String s = HttpRequest.get("https://service.fpsmaster.top/getServers");
                System.out.println(s);
                JsonObject jsonObject = gson.fromJson(s, JsonObject.class);
                jsonObject.get("data").getAsJsonArray().forEach(e -> {
                    this.serverListRecommended.add(new ServerListEntry(this, new ServerData(e.getAsJsonObject().get("name").getAsString() + " - " + e.getAsJsonObject().get("description").getAsString(), e.getAsJsonObject().get("address").getAsString(), false)));
                });
            });
        }
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
    int tab = 0;


    @Override
    public void render(int mouseX, int mouseY, float partialTicks) {
        super.render(mouseX, mouseY, partialTicks);
        Render2DUtils.drawBackground((int) guiWidth, (int) guiHeight, mouseX, mouseY, partialTicks, (int) zLevel);

        UFontRenderer title = FPSMaster.fontManager.s22;
        UFontRenderer font = FPSMaster.fontManager.s18;
        title.drawCenteredString("多人游戏", width / 2f, 16, -1);

        Render2DUtils.drawOptimizedRoundedRect((width - 180) / 2f, 30, 180, 24, 3, new Color(255, 255, 255, 80).getRGB());
        Render2DUtils.drawOptimizedRoundedRect((width - 176) / 2f + 90 * tab, 32, 86, 20, 3, new Color(113, 127, 254).getRGB());
        FPSMaster.fontManager.s16.drawCenteredString("服务器列表", (width - 90) / 2f, 36, -1);
        FPSMaster.fontManager.s16.drawCenteredString("推荐服务器", (width + 90) / 2f, 36, -1);

        GL11.glPushMatrix();
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        Render2DUtils.doGlScissor((width - 400) / 2f, 60f, 400f, height - 120, scaleFactor);
        scrollContainer.draw((width - 400) / 2f, 60, 396, height - 120, mouseX, mouseY, () -> {
            float y = 70 + scrollContainer.getScroll();
            Render2DUtils.drawOptimizedRoundedRect((width - 400) / 2f, y - 10, 400, height - y, 5, new Color(0, 0, 0, 100).getRGB());
            int index = 0;
            for (ServerListEntry server : serverListDisplay) {
                if (server.getServerData() == null) {
                    return;
                }
                Render2DUtils.drawOptimizedRoundedRect((width - 340) / 2f, y, 340, 54, new Color(0, 0, 0, 120));
                if (Render2DUtils.isHovered((width - 340) / 2f, y, 340, 54, mouseX, mouseY)) {
                    Render2DUtils.drawOptimizedRoundedRect((width - 340) / 2f, y, 340, 54, new Color(0, 0, 0, 50));
                }
                if (selectedServer != null && servers.indexOf(selectedServer) == index) {
                    Render2DUtils.drawOptimizedRoundedRect((width - 340) / 2f, y, 340, 54, new Color(255, 255, 255, 50));
                }
                server.drawEntry(0, (width - 340) / 2, (int) y, 340, 54, mouseX, mouseY, false);
                index++;
                y += 58;
            }
            scrollContainer.setHeight(y - 50 - scrollContainer.getScroll());
        });
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
        GL11.glPopMatrix();


        join.render((width - 400) / 2f + 20, height - 56, 380f / 3 - 20, 20, mouseX, mouseY);
        connect.render((width - 400) / 2f + 20 + 380f / 3, height - 56, 380f / 3 - 20, 20, mouseX, mouseY);
        add.render((width - 400) / 2f + 20 + 380f / 3 * 2, height - 56, 380f / 3 - 20, 20, mouseX, mouseY);


        edit.render((width - 400) / 2f + 20, height - 26, 380f / 4 - 20, 20, mouseX, mouseY);
        remove.render((width - 400) / 2f + 20 + 380f / 4, height - 26, 380f / 4 - 20, 20, mouseX, mouseY);
        refresh.render((width - 400) / 2f + 20 + 380f / 4 * 2, height - 26, 380f / 4 - 20, 20, mouseX, mouseY);
        back.render((width - 400) / 2f + 20 + 380f / 4 * 3, height - 26, 380f / 4 - 20, 20, mouseX, mouseY);


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


        Render2DUtils.drawOptimizedRoundedRect((width - 180) / 2f, 30, 180, 24, 3, new Color(255, 255, 255, 80).getRGB());
        Render2DUtils.drawOptimizedRoundedRect((width - 176) / 2f, 32, 86, 20, 3, new Color(113, 127, 254).getRGB());
        FPSMaster.fontManager.s16.drawCenteredString("服务器列表", (width - 90) / 2f, 36, -1);
        FPSMaster.fontManager.s16.drawCenteredString("推荐服务器", (width + 90) / 2f, 36, -1);

        if (Render2DUtils.isHovered((width - 180) / 2f, 30, 90, 24, mouseX, mouseY)) {
            tab = 0;
            serverListDisplay.clear();
            serverListDisplay.addAll(serverListInternet);
        } else if (Render2DUtils.isHovered((width) / 2f, 30, 90, 24, mouseX, mouseY)) {
            tab = 1;
            serverListDisplay.clear();
            serverListDisplay.addAll(serverListRecommended);
        }

        int y = 80;
        int index = 0;
        for (ServerListEntry server : serverListDisplay) {
            if (server.getServerData() == null) {
                return;
            }
            if (Render2DUtils.isHovered((width - 340) / 2f, y, 340, 54, mouseX, mouseY)) {
                if (selectedServer != servers.get(index)) {
                    selectedServer = servers.get(index);
                } else {
                    selectedServer = null;
                }
            }
            index++;
            y += 54;
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
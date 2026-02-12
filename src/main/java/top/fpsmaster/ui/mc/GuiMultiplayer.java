package top.fpsmaster.ui.mc;

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
import top.fpsmaster.FPSMaster;
import top.fpsmaster.font.impl.UFontRenderer;
import top.fpsmaster.ui.common.GuiButton;
import top.fpsmaster.ui.screens.mainmenu.MainMenu;
import top.fpsmaster.utils.render.gui.ScaledGuiScreen;
import top.fpsmaster.utils.render.gui.Backgrounds;

import java.awt.*;
import java.io.File;
import java.util.List;

public class GuiMultiplayer extends ScaledGuiScreen {

    private ServerData selectedServer;
    private static final Logger logger = LogManager.getLogger();
    private final List<ServerData> servers = Lists.newArrayList();
    public final OldServerPinger oldServerPinger = new OldServerPinger();

    String action = "";

    GuiButton join = new GuiButton("multiplayer.join", () -> {
        if (selectedServer == null)
            return;
        FMLClientHandler.instance().connectToServer(this, selectedServer);
    }, new Color(0, 0, 0, 140), new Color(113, 127, 254))
            .setBackgroundColors(new Color(0, 0, 0, 140), new Color(113, 127, 254), new Color(128, 140, 255))
            .setClickEffect(GuiButton.ClickEffect.STACK, new Color(255, 255, 255, 120), 0.22f);
    GuiButton connect = new GuiButton("multiplayer.direct", () -> {
        this.mc.displayGuiScreen(new GuiScreenServerList(this, this.selectedServer = new ServerData(I18n.format("selectServer.defaultName"), "", false)));
        action = "connect";
    }, new Color(0, 0, 0, 140), new Color(113, 127, 254))
            .setBackgroundColors(new Color(0, 0, 0, 140), new Color(113, 127, 254), new Color(128, 140, 255))
            .setClickEffect(GuiButton.ClickEffect.STACK, new Color(255, 255, 255, 120), 0.22f);
    GuiButton add = new GuiButton("multiplayer.add", () -> {
        action = "add";
        this.mc.displayGuiScreen(new GuiScreenAddServer(this, this.selectedServer = new ServerData(I18n.format("selectServer.defaultName"), "", false)));
    }, new Color(0, 0, 0, 140), new Color(113, 127, 254))
            .setBackgroundColors(new Color(0, 0, 0, 140), new Color(113, 127, 254), new Color(128, 140, 255))
            .setClickEffect(GuiButton.ClickEffect.STACK, new Color(255, 255, 255, 120), 0.22f);
    GuiButton edit = new GuiButton("multiplayer.edit", () -> {
        if (selectedServer == null)
            return;
        action = "edit";
        mc.displayGuiScreen(new GuiScreenAddServer(this, selectedServer));
    }, new Color(0, 0, 0, 140), new Color(113, 127, 254))
            .setBackgroundColors(new Color(0, 0, 0, 140), new Color(113, 127, 254), new Color(128, 140, 255))
            .setClickEffect(GuiButton.ClickEffect.STACK, new Color(255, 255, 255, 120), 0.22f);
    GuiButton remove = new GuiButton("multiplayer.delete", () -> {
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
    }, new Color(0, 0, 0, 140), new Color(113, 127, 254))
            .setBackgroundColors(new Color(0, 0, 0, 140), new Color(113, 127, 254), new Color(128, 140, 255))
            .setClickEffect(GuiButton.ClickEffect.STACK, new Color(255, 255, 255, 120), 0.22f);
    GuiButton refresh = new GuiButton("multiplayer.refresh", () -> mc.displayGuiScreen(new GuiMultiplayer()), new Color(0, 0, 0, 140), new Color(113, 127, 254))
            .setBackgroundColors(new Color(0, 0, 0, 140), new Color(113, 127, 254), new Color(128, 140, 255))
            .setClickEffect(GuiButton.ClickEffect.STACK, new Color(255, 255, 255, 120), 0.22f);
    GuiButton back = new GuiButton("multiplayer.back", () -> mc.displayGuiScreen(new MainMenu()), new Color(0, 0, 0, 140), new Color(113, 127, 254))
            .setBackgroundColors(new Color(0, 0, 0, 140), new Color(113, 127, 254), new Color(128, 140, 255))
            .setClickEffect(GuiButton.ClickEffect.STACK, new Color(255, 255, 255, 120), 0.22f);


    @Override
    public void initGui() {
        super.initGui();
        loadServerList();
        selectedServer = servers.isEmpty() ? null : servers.get(0);
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

    @Override
    public void render(int mouseX, int mouseY, float partialTicks) {
        super.render(mouseX, mouseY, partialTicks);
        Backgrounds.draw((int) (guiWidth * scaleFactor), (int) (guiHeight * scaleFactor), mouseX, mouseY, partialTicks, (int) zLevel);

        UFontRenderer title = FPSMaster.fontManager.s22;
        title.drawCenteredString(FPSMaster.i18n.get("multiplayer.title"), guiWidth / 2f, 16, -1);


        join.renderInScreen(this, (guiWidth - 400) / 2f + 20, guiHeight - 56, 380f / 3 - 20, 20, mouseX, mouseY);
        connect.renderInScreen(this, (guiWidth - 400) / 2f + 20 + 380f / 3, guiHeight - 56, 380f / 3 - 20, 20, mouseX, mouseY);
        add.renderInScreen(this, (guiWidth - 400) / 2f + 20 + 380f / 3 * 2, guiHeight - 56, 380f / 3 - 20, 20, mouseX, mouseY);

        edit.renderInScreen(this, (guiWidth - 400) / 2f + 20, guiHeight - 26, 380f / 4 - 20, 20, mouseX, mouseY);
        remove.renderInScreen(this, (guiWidth - 400) / 2f + 20 + 380f / 4, guiHeight - 26, 380f / 4 - 20, 20, mouseX, mouseY);
        refresh.renderInScreen(this, (guiWidth - 400) / 2f + 20 + 380f / 4 * 2, guiHeight - 26, 380f / 4 - 20, 20, mouseX, mouseY);
        back.renderInScreen(this, (guiWidth - 400) / 2f + 20 + 380f / 4 * 3, guiHeight - 26, 380f / 4 - 20, 20, mouseX, mouseY);
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





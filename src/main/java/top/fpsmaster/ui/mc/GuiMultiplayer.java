package top.fpsmaster.ui.mc;

import com.google.common.collect.Lists;
import net.minecraft.client.gui.GuiScreenAddServer;
import net.minecraft.client.gui.GuiScreenServerList;
import net.minecraft.client.gui.GuiYesNo;
import net.minecraft.client.multiplayer.GuiConnecting;
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
import top.fpsmaster.modules.client.api.PromotedServersService;
import top.fpsmaster.modules.client.api.model.PromotedServerView;
import top.fpsmaster.ui.click.ClickGuiTheme;
import top.fpsmaster.ui.click.UiChrome;
import top.fpsmaster.ui.click.component.ScrollContainer;
import top.fpsmaster.ui.common.GuiButton;
import top.fpsmaster.ui.common.TextField;
import top.fpsmaster.ui.screens.mainmenu.MainMenu;
import top.fpsmaster.utils.render.draw.Hover;
import top.fpsmaster.utils.render.draw.Icons;
import top.fpsmaster.utils.render.draw.Rects;
import top.fpsmaster.utils.render.gui.ScaledGuiScreen;
import top.fpsmaster.utils.render.gui.Backgrounds;
import top.fpsmaster.utils.render.gui.Scissor;

import java.awt.*;
import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class GuiMultiplayer extends ScaledGuiScreen {
    private ServerData selectedServer;
    private static final Logger logger = LogManager.getLogger();
    private final List<ServerData> servers = Lists.newArrayList();
    public final OldServerPinger oldServerPinger = new OldServerPinger();
    private final List<ServerListEntry> serverListDisplay = Lists.newArrayList();
    private final List<ServerListEntry> serverListInternet = Lists.newArrayList();

    String action = "";
    private TextField searchField;
    private String lastQuery = "";

    /** Synthetic {@link ServerData} per promoted-only address, reused across rebuilds so each is pinged once. */
    private final Map<String, ServerData> promotedServerData = new HashMap<>();
    private long promotedRevisionSeen = -1L;

    private void joinSelected() {
        if (selectedServer != null) {
            this.mc.displayGuiScreen(new GuiConnecting(this, this.mc, selectedServer));
        }
    }

    private void directConnect() {
        action = "connect";
        this.mc.displayGuiScreen(new GuiScreenServerList(this, this.selectedServer = new ServerData(I18n.format("selectServer.defaultName"), "", false)));
    }

    private void addServer() {
        action = "add";
        this.mc.displayGuiScreen(new GuiScreenAddServer(this, this.selectedServer = new ServerData(I18n.format("selectServer.defaultName"), "", false)));
    }

    private void editSelected() {
        if (selectedServer == null) {
            return;
        }
        action = "edit";
        mc.displayGuiScreen(new GuiScreenAddServer(this, selectedServer));
    }

    private void removeSelected() {
        if (selectedServer == null || selectedServer.serverName == null) {
            return;
        }
        action = "remove";
        GuiYesNo guiyesno = new GuiYesNo(this,
                I18n.format("selectServer.deleteQuestion"),
                "'" + selectedServer.serverName + "' " + I18n.format("selectServer.deleteWarning"),
                I18n.format("selectServer.deleteButton"),
                I18n.format("gui.cancel"),
                servers.indexOf(selectedServer));
        this.mc.displayGuiScreen(guiyesno);
    }

    @Override
    public void initGui() {
        super.initGui();
        // Forge patches OldServerPinger to feed every status response to
        // FMLClientHandler.bindServerListData, whose backing maps are only created by vanilla
        // GuiMultiplayer (constructor/initGui) via FMLClientHandler.setupServerList(). This custom
        // screen replaces the vanilla one, so the init has to happen here or the first ping dies
        // with an NPE inside Forge's bindServerListData.
        if (!Boolean.getBoolean("fpsmaster.noforge")) {
            try {
                FMLClientHandler.instance().setupServerList();
            } catch (NoClassDefFoundError e) {
                // Forge-free runtime: no FMLClientHandler, and no Forge patch to feed either.
            }
        }
        loadServerList();
        PromotedServersService promotedService = PromotedServersService.getInstance();
        promotedService.refreshIfStale();
        promotedRevisionSeen = promotedService.revision();
        rebuildServerEntries();
        // Prototype opens with the first server already selected so the detail column is
        // never an empty pane when there is anything to show.
        selectedServer = serverListInternet.isEmpty() ? null : serverListInternet.get(0).getServerData();
        if (searchField == null) {
            searchField = new TextField(
                    FPSMaster.fontManager.getFont(12),
                    FPSMaster.i18n.get("multiplayer.search.placeholder"),
                    0,
                    ClickGuiTheme.textFieldText().getRGB(),
                    48
            );
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
                    // Deleting a promoted row is a local dismissal: remember it so it is never
                    // inserted again, without touching the backend.
                    if (isPromotedEntry(selectedServer)) {
                        PromotedServersService.getInstance().hideByAddress(selectedServer.serverIP);
                    }
                    servers.remove(selectedServer);
                    saveServerList();
                    break;
                case "connect":
                    this.mc.displayGuiScreen(new GuiConnecting(this, this.mc, selectedServer));
                    break;
            }
            action = "";
        }
        mc.displayGuiScreen(this);
    }

    @Override
    public void keyTyped(char typedChar, int keyCode) throws java.io.IOException {
        if (searchField != null && searchField.isFocused()) {
            if (keyCode == 1) {
                searchField.setFocused(false);
                return;
            }
            searchField.textboxKeyTyped(typedChar, keyCode);
            return;
        }
        super.keyTyped(typedChar, keyCode);
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
        applySearchFilter();

        float pageW = Math.min(470f, guiWidth - 24f);
        float pageX = (guiWidth - pageW) / 2f;
        float pageY = 20f;
        float pageH = guiHeight - pageY - 16f;

        // ---- header row: back · title · count · search · refresh · add ----
        float headH = 18f;
        boolean backHover = Hover.is(pageX, pageY, headH, headH, mouseX, mouseY);
        UiChrome.ghostButton(pageX, pageY, headH, headH, backHover);
        Icons.draw("back", pageX + 5f, pageY + 5f, 8f,
                (backHover ? ClickGuiTheme.textPrimary() : ClickGuiTheme.textSecondary()).getRGB());
        if (consumePressInBounds(pageX, pageY, headH, headH, 0) != null) {
            mc.displayGuiScreen(new MainMenu());
        }

        String title = FPSMaster.i18n.get("multiplayer.title");
        UiChrome.boldString(FPSMaster.fontManager.s20, title, pageX + headH + 6f, pageY + 3f,
                ClickGuiTheme.textPrimary().getRGB());
        FPSMaster.fontManager.getFont(12).drawString(
                String.format(FPSMaster.i18n.get("multiplayer.count"), serverListDisplay.size()),
                pageX + headH + 6f + FPSMaster.fontManager.s20.getStringWidth(title) + 5f,
                pageY + 6f,
                ClickGuiTheme.textSecondary().getRGB()
        );

        float addW = Math.max(58f, FPSMaster.fontManager.s14.getStringWidth(FPSMaster.i18n.get("multiplayer.add")) + 22f);
        float addX = pageX + pageW - addW;
        float refreshX = addX - 5f - headH;
        float searchW = 110f;
        float searchX = refreshX - 5f - searchW;

        if (searchField != null) {
            searchField.backGroundColor = 0;
            searchField.fontColor = ClickGuiTheme.textFieldText().getRGB();
            searchField.placeHolder = FPSMaster.i18n.get("multiplayer.search.placeholder");
            UiChrome.searchBox(searchX, pageY, searchW, headH, searchField.isFocused());
            Icons.draw("search", searchX + 6f, pageY + (headH - 6.5f) / 2f, 6.5f, ClickGuiTheme.textDisabled().getRGB());
            searchField.drawTextBox(searchX + 15f, pageY + 1f, searchW - 20f, headH - 2f);
            ScaledGuiScreen.PointerEvent searchClick = consumePressInBounds(searchX, pageY, searchW, headH, 0);
            if (searchClick != null) {
                searchField.setFocused(true);
                searchField.mouseClicked(searchClick.x, searchClick.y, 0);
            }
        }

        boolean refreshHover = Hover.is(refreshX, pageY, headH, headH, mouseX, mouseY);
        UiChrome.iconButton(refreshX, pageY, headH, refreshHover);
        Icons.draw("refresh", refreshX + 5f, pageY + 5f, 8f,
                (refreshHover ? ClickGuiTheme.textPrimary() : ClickGuiTheme.textSecondary()).getRGB());
        if (consumePressInBounds(refreshX, pageY, headH, headH, 0) != null) {
            mc.displayGuiScreen(new GuiMultiplayer());
        }

        if (UiChrome.buttonClicked(this, addX, pageY, addW, headH, "plus",
                FPSMaster.i18n.get("multiplayer.add"), UiChrome.Style.PRIMARY, mouseX, mouseY)) {
            addServer();
        }

        // ---- two columns ----
        float colsY = pageY + headH + 7f;
        float colsH = pageH - headH - 7f;
        float detailW = Math.min(180f, pageW * 0.38f);
        float listW = pageW - detailW - 7f;
        UiChrome.panel(pageX, colsY, listW, colsH);
        UiChrome.panel(pageX + listW + 7f, colsY, detailW, colsH);

        float footH = 27f;
        float listViewportX = pageX + 5f;
        float listViewportY = colsY + 5f;
        float listViewportWidth = listW - 10f;
        float listViewportHeight = colsH - 10f - footH;
        float rowHeight = 29f;
        GL11.glPushMatrix();
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        Scissor.apply(listViewportX, listViewportY, listViewportWidth, listViewportHeight);
        scrollContainer.draw(this, listViewportX, listViewportY, listViewportWidth, listViewportHeight, mouseX, mouseY, () -> {
            float y = listViewportY + scrollContainer.getScroll();
            for (ServerListEntry server : serverListDisplay) {
                if (server.getServerData() == null) {
                    return;
                }
                boolean selected = selectedServer != null && selectedServer == server.getServerData();
                boolean hovered = Hover.is(listViewportX, y, listViewportWidth, rowHeight, mouseX, mouseY)
                        && Hover.is(listViewportX, listViewportY, listViewportWidth, listViewportHeight, mouseX, mouseY);
                if (selected) {
                    UiChrome.selectedCard(listViewportX, y, listViewportWidth, rowHeight);
                } else {
                    UiChrome.card(listViewportX, y, listViewportWidth, rowHeight, hovered, false);
                }
                if (hovered && consumePressInBounds(listViewportX, y, listViewportWidth, rowHeight, 0) != null) {
                    selectedServer = server.getServerData();
                    server.triggerClick();
                }
                server.drawEntry((int) listViewportX, (int) y, (int) listViewportWidth, (int) rowHeight, mouseX, mouseY);
                y += rowHeight + 2.5f;
            }
            scrollContainer.setHeight(y - listViewportY - scrollContainer.getScroll());
        });
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
        GL11.glPopMatrix();

        // ---- list footer: direct connect ----
        float footY = colsY + colsH - footH;
        UiChrome.hairlineH(pageX + 1, footY, listW - 2);
        if (UiChrome.buttonClicked(this, pageX + 5f, footY + 5f, listW - 10f, 17f, "link",
                FPSMaster.i18n.get("multiplayer.direct"), UiChrome.Style.DEFAULT, mouseX, mouseY)) {
            directConnect();
        }

        renderDetail(pageX + listW + 7f, colsY, detailW, colsH, mouseX, mouseY);
    }

    private void applySearchFilter() {
        if (searchField == null) {
            return;
        }
        String query = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();
        if (query.equals(lastQuery)) {
            return;
        }
        lastQuery = query;
        serverListDisplay.clear();
        for (ServerListEntry entry : serverListInternet) {
            ServerData data = entry.getServerData();
            if (query.isEmpty()
                    || data.serverName.toLowerCase().contains(query)
                    || data.serverIP.toLowerCase().contains(query)) {
                serverListDisplay.add(entry);
            }
        }
    }

    private void renderDetail(float x, float y, float width, float height, int mouseX, int mouseY) {
        ServerListEntry selected = null;
        for (ServerListEntry entry : serverListInternet) {
            if (entry.getServerData() == selectedServer) {
                selected = entry;
                break;
            }
        }
        if (selected == null || selectedServer == null) {
            FPSMaster.fontManager.s14.drawCenteredString(
                    FPSMaster.i18n.get("multiplayer.selected.none"),
                    x + width / 2f,
                    y + height / 2f,
                    ClickGuiTheme.textDisabled().getRGB()
            );
            return;
        }
        float pad = 11f;
        selected.drawIcon(x + pad, y + pad, 28f);
        UiChrome.boldString(FPSMaster.fontManager.s18, selectedServer.serverName, x + pad + 35f, y + pad + 3f,
                ClickGuiTheme.textPrimary().getRGB());
        FPSMaster.fontManager.getFont(12).drawString(
                FPSMaster.fontManager.getFont(12).trimStringToWidth(selectedServer.serverIP, width - pad * 2f - 35f),
                x + pad + 35f,
                y + pad + 15f,
                ClickGuiTheme.textDisabled().getRGB()
        );

        // MOTD box
        float motdY = y + pad + 36f;
        float motdH = 26f;
        Rects.rounded(x + pad - 0.5f, motdY - 0.5f, width - pad * 2f + 1f, motdH + 1f, UiChrome.CARD_RADIUS + 1,
                ClickGuiTheme.stroke().getRGB(), false);
        Rects.rounded(x + pad, motdY, width - pad * 2f, motdH, UiChrome.CARD_RADIUS,
                ClickGuiTheme.mask(56).getRGB(), false);
        String motd = selectedServer.serverMOTD == null ? "" : selectedServer.serverMOTD.replaceAll("§.", "");
        java.util.List<String> motdLines = FPSMaster.fontManager.getFont(12).listFormattedStringToWidth(motd, (int) (width - pad * 2f - 14f));
        for (int i = 0; i < Math.min(2, motdLines.size()); i++) {
            FPSMaster.fontManager.getFont(12).drawString(motdLines.get(i), x + pad + 7f, motdY + 6f + i * 9f,
                    ClickGuiTheme.textSecondary().getRGB());
        }

        // Stats grid
        float statY = motdY + motdH + 8f;
        float statW = (width - pad * 2f - 4f) / 2f;
        long ping = selectedServer.pingToServer;
        drawStat(x + pad, statY, statW, "users",
                selectedServer.populationInfo, FPSMaster.i18n.get("multiplayer.players"));
        drawStat(x + pad + statW + 4f, statY, statW, "zap",
                ping < 0 ? "—" : ping + "ms", FPSMaster.i18n.get("multiplayer.ping"));
        drawStat(x + pad, statY + 25f, statW, "box",
                selectedServer.gameVersion, FPSMaster.i18n.get("multiplayer.version"));
        drawStat(x + pad + statW + 4f, statY + 25f, statW, "globe",
                ping < 0 ? FPSMaster.i18n.get("multiplayer.offline") : FPSMaster.i18n.get("multiplayer.online"),
                FPSMaster.i18n.get("multiplayer.status"));

        // Actions pinned to the bottom. Promoted-only rows are not the player's servers:
        // no edit and no pin, delete just dismisses the promotion locally.
        boolean ownServer = servers.contains(selectedServer);
        float joinH = 21f;
        float rowH = 18f;
        float rowY = y + height - pad - rowH;
        float joinY = rowY - 4f - joinH;
        boolean joinHover = Hover.is(x + pad, joinY, width - pad * 2f, joinH, mouseX, mouseY);
        UiChrome.fillButton(x + pad, joinY, width - pad * 2f, joinH, joinHover, false);
        float joinLabelW = FPSMaster.fontManager.s14.getStringWidth(FPSMaster.i18n.get("multiplayer.join"));
        Icons.draw("play", x + width / 2f - joinLabelW / 2f - 10f, joinY + (joinH - 7.5f) / 2f, 7.5f, 0xFFFFFFFF);
        FPSMaster.fontManager.s14.drawString(FPSMaster.i18n.get("multiplayer.join"),
                x + width / 2f - joinLabelW / 2f + 1f, joinY + joinH / 2f - 3.5f, 0xFFFFFFFF);
        if (consumePressInBounds(x + pad, joinY, width - pad * 2f, joinH, 0) != null) {
            joinSelected();
        }

        if (ownServer) {
            boolean pinned = PromotedServersService.getInstance().isPinned(selectedServer.serverIP);
            float pinY = joinY - 4f - rowH;
            if (UiChrome.buttonClicked(this, x + pad, pinY, width - pad * 2f, rowH, null,
                    FPSMaster.i18n.get(pinned ? "multiplayer.unpin" : "multiplayer.pin"),
                    UiChrome.Style.DEFAULT, mouseX, mouseY)) {
                PromotedServersService.getInstance().setPinned(selectedServer.serverIP, !pinned);
                rebuildServerEntries();
            }
            float halfW = (width - pad * 2f - 4f) / 2f;
            if (UiChrome.buttonClicked(this, x + pad, rowY, halfW, rowH, "rename",
                    FPSMaster.i18n.get("multiplayer.edit"), UiChrome.Style.DEFAULT, mouseX, mouseY)) {
                editSelected();
            }
            if (UiChrome.buttonClicked(this, x + pad + halfW + 4f, rowY, halfW, rowH, "delete",
                    FPSMaster.i18n.get("multiplayer.delete"), UiChrome.Style.DANGER, mouseX, mouseY)) {
                removeSelected();
            }
        } else {
            if (UiChrome.buttonClicked(this, x + pad, rowY, width - pad * 2f, rowH, "delete",
                    FPSMaster.i18n.get("multiplayer.delete"), UiChrome.Style.DANGER, mouseX, mouseY)) {
                removeSelected();
            }
        }
    }

    private void drawStat(float x, float y, float width, String icon, String value, String label) {
        Rects.rounded(x, y, width, 21f, UiChrome.CARD_RADIUS, ClickGuiTheme.layer().getRGB(), false);
        Icons.draw(icon, x + 6f, y + 6.75f, 7.5f, ClickGuiTheme.textDisabled().getRGB());
        String v = value == null || value.isEmpty() ? "—" : value;
        FPSMaster.fontManager.getFont(13).drawString(
                FPSMaster.fontManager.getFont(13).trimStringToWidth(v, width - 24f),
                x + 17f, y + 3.5f, ClickGuiTheme.textPrimary().getRGB());
        FPSMaster.fontManager.getFont(10).drawString(label, x + 17f, y + 12.5f, ClickGuiTheme.textDisabled().getRGB());
    }


    @Override
    public void updateScreen() {
        super.updateScreen();
        long promotedRevision = PromotedServersService.getInstance().revision();
        if (promotedRevision != promotedRevisionSeen) {
            promotedRevisionSeen = promotedRevision;
            rebuildServerEntries();
            if (selectedServer == null && !serverListInternet.isEmpty()) {
                selectedServer = serverListInternet.get(0).getServerData();
            }
        }
        // Forge's FMLClientHandler.setupServerList() only wires LAN/Realms discovery, which this
        // custom multiplayer screen does not use; the vanilla ping loop below is all we need.
        // Guard the ping loop: OldServerPinger's legacy-ping fallback can surface a netty NPE
        // (Bootstrap.checkAddress with a null address) on the tick thread when a server entry
        // fails to resolve, which would otherwise crash the whole client (issue #179). A single
        // bad server ping must never take down the screen.
        try {
            this.oldServerPinger.pingPendingNetworks();
        } catch (Throwable t) {
            logger.error("Couldn't ping pending server networks", t);
        }
    }

    @Override
    public void onGuiClosed() {
        super.onGuiClosed();
        logger.info("PINGDBG onGuiClosed");
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

    /**
     * Rebuilds the entry list in display order: the player's pinned servers first, then visible
     * promoted servers in backend order, then the remaining own servers in {@code servers.dat}
     * order. A promoted server whose address matches an own server collapses into one row — styled
     * as user-pinned when the player pinned it, as promoted otherwise.
     */
    private void rebuildServerEntries() {
        PromotedServersService service = PromotedServersService.getInstance();
        serverListInternet.clear();

        Set<ServerData> placed = Collections.newSetFromMap(new IdentityHashMap<>());
        for (ServerData server : servers) {
            if (service.isPinned(server.serverIP)) {
                serverListInternet.add(new ServerListEntry(this, server, false, true, null));
                placed.add(server);
            }
        }

        Set<String> promotedAddressesSeen = new HashSet<>();
        for (PromotedServerView view : service.promotedServers()) {
            String address = PromotedServersService.normalizeAddress(view.getAddress());
            if (!view.isActive() || service.isHidden(view) || !promotedAddressesSeen.add(address)) {
                continue;
            }
            ServerData ownServer = findServerByAddress(address);
            if (ownServer != null) {
                // User-pinned styling wins for a pinned own server; the promoted row is dropped.
                if (!placed.contains(ownServer)) {
                    serverListInternet.add(new ServerListEntry(this, ownServer, true, false, view.getDescription()));
                    placed.add(ownServer);
                }
            } else {
                serverListInternet.add(new ServerListEntry(this, promotedServerData(view), true, false, view.getDescription()));
            }
        }

        for (ServerData server : servers) {
            if (!placed.contains(server)) {
                serverListInternet.add(new ServerListEntry(this, server, false, false, null));
            }
        }

        serverListDisplay.clear();
        serverListDisplay.addAll(serverListInternet);
        // Force applySearchFilter to re-filter the fresh entries on the next frame.
        lastQuery = null;
    }

    private ServerData findServerByAddress(String normalizedAddress) {
        for (ServerData server : servers) {
            if (PromotedServersService.normalizeAddress(server.serverIP).equals(normalizedAddress)) {
                return server;
            }
        }
        return null;
    }

    private ServerData promotedServerData(PromotedServerView view) {
        String key = PromotedServersService.normalizeAddress(view.getAddress());
        ServerData data = promotedServerData.get(key);
        if (data == null) {
            String name = view.getName() == null || view.getName().trim().isEmpty()
                    ? view.getAddress().trim()
                    : view.getName().trim();
            data = new ServerData(name, view.getAddress().trim(), false);
            promotedServerData.put(key, data);
        }
        return data;
    }

    private boolean isPromotedEntry(ServerData server) {
        if (server == null) {
            return false;
        }
        for (ServerListEntry entry : serverListInternet) {
            if (entry.getServerData() == server) {
                return entry.isPromoted();
            }
        }
        return false;
    }
}





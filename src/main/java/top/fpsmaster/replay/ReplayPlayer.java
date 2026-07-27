package top.fpsmaster.replay;

import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;
import io.netty.buffer.Unpooled;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityOtherPlayerMP;
import net.minecraft.client.multiplayer.PlayerControllerMP;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.item.ItemStack;
import net.minecraft.network.EnumConnectionState;
import net.minecraft.network.EnumPacketDirection;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.server.S08PacketPlayerPosLook;
import net.minecraft.network.play.server.S0CPacketSpawnPlayer;
import net.minecraft.network.play.server.S2BPacketChangeGameState;
import net.minecraft.network.play.server.S2DPacketOpenWindow;
import net.minecraft.network.play.server.S39PacketPlayerAbilities;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import org.lwjgl.input.Keyboard;
import top.fpsmaster.forge.mixin.accessor.NetHandlerPlayClientAccessor;
import top.fpsmaster.modules.logger.ClientLogger;
import top.fpsmaster.ui.screens.replay.ReplayScreen;
import top.fpsmaster.utils.io.FileUtils;

import java.io.File;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * Plays a recording back into a live client.
 *
 * <h3>Why there is no server</h3>
 *
 * <p>The obvious design is to spin up a loopback server and replay the packets over a socket. That
 * would be faithful to how the packets originally arrived, but it also reintroduces everything a
 * benchmark wants held still: syscalls, Nagle, the encryption and compression pipeline, and a second
 * process competing for the same cores. Instead the recorded packets are handed straight to a
 * {@link NetHandlerPlayClient} on the client thread, which is where they would have ended up anyway
 * — Netty only ever hands packets across; the handler does the work being measured.
 *
 * <p>The connection object is real but has no channel. Anything the client tries to send is dropped,
 * which is correct: there is nobody to answer, and a queued outbound backlog would grow for the
 * length of the session.
 *
 * <h3>Camera</h3>
 *
 * <p>The viewer is a spectator: no collision, no interaction, free flight. The recording player is
 * rebuilt as an avatar from the position track and can be possessed by looking at it and clicking,
 * which moves the render view onto it; sneak releases it. While possessed the view is entirely the
 * recorder's own — the mouse does not move it, because the point is to see what they saw.
 */
public final class ReplayPlayer {

    /** Far above any id a server hands out, so the camera and avatar cannot collide with the stream. */
    private static final int CAMERA_ENTITY_ID = Integer.MAX_VALUE;
    private static final int AVATAR_ENTITY_ID = Integer.MAX_VALUE - 1;

    /** How far the possession ray reaches. Vanilla's 3-block spectator pick is unusable here. */
    private static final double POSSESS_REACH = 128.0d;

    /** Packet id of S38PacketPlayerListItem in the 1.8 play/clientbound table. */
    private static final int PLAYER_LIST_ITEM_ID = 0x38;

    /** The "change game mode" reason of S2BPacketChangeGameState. */
    private static final int GAME_STATE_CHANGE_GAME_MODE = 3;

    /** Bounded so a paused replay cannot pull the whole file into memory. */
    private static final int QUEUE_CAPACITY = 8192;

    /** Enough to see the shape of a failure without filling the log with one repeat. */
    private static final int MAX_LOGGED_FAILURES = 3;

    private static final ReplayPlayer INSTANCE = new ReplayPlayer();

    private final BlockingQueue<Frame> queue = new ArrayBlockingQueue<Frame>(QUEUE_CAPACITY);

    private volatile boolean active;
    private volatile boolean readerFinished;
    private Thread readerThread;
    private Frame pending;

    private NetHandlerPlayClient netHandler;
    private GameProfile recorderProfile;
    private EntityOtherPlayerMP avatar;
    private Object lastWorld;
    private Object lastCameraPlayer;
    private File file;

    private long originNanos;
    private int pausedAtMillis;
    private boolean paused;
    private int durationMillis;
    private int elapsedMillis;

    private boolean possessing;
    private boolean attackWasDown;
    private boolean sneakWasDown;
    private boolean pauseWasDown;
    private boolean autoPlayDone;
    private int loggedFailures;

    private ReplayPlayer() {
    }

    public static ReplayPlayer instance() {
        return INSTANCE;
    }

    public boolean isActive() {
        return active;
    }

    /** True once the recorder's avatar has been built from the movement track. */
    public boolean hasAvatar() {
        return avatar != null;
    }

    public boolean isPossessing() {
        return possessing;
    }

    public boolean isPaused() {
        return paused;
    }

    public int elapsedMillis() {
        return elapsedMillis;
    }

    public int durationMillis() {
        return durationMillis;
    }

    public String recorderName() {
        return recorderProfile == null ? "" : recorderProfile.getName();
    }

    public File file() {
        return file;
    }

    /**
     * True for the entity the viewer is flying around as.
     *
     * <p>Vanilla decides whether a client player is a spectator by looking its own UUID up in the tab
     * list, which cannot work here: there is no server to put the viewer in that list, and when you
     * watch your own recording the entry that <em>is</em> there belongs to the avatar. So the camera
     * is marked directly instead — see {@code AbstractClientPlayerMixin_ReplaySpectator}.
     */
    public boolean isCameraEntity(Object entity) {
        return active && entity == Minecraft.getMinecraft().thePlayer;
    }

    public boolean isAvatar(Object entity) {
        return entity != null && entity == avatar;
    }

    /**
     * Starts playback if {@code -Dedge.replay.play=<name>} was passed.
     *
     * <p>The automated counterpart to picking a recording in the browser, so playback can be checked
     * without a human sitting in front of it.
     */
    public void startIfRequested() {
        String requested = System.getProperty("edge.replay.play");
        if (requested == null || requested.isEmpty() || active || autoPlayDone) {
            return;
        }
        autoPlayDone = true;
        start(new File(new File(FileUtils.dir, "replays"), requested + ".edgereplay"));
    }

    /** State of the recorder's avatar, for the automated probe. Null before the first sample. */
    public JsonObject avatarState() {
        EntityOtherPlayerMP current = avatar;
        if (current == null) {
            return null;
        }
        JsonObject state = new JsonObject();
        state.addProperty("x", Double.valueOf(current.posX));
        state.addProperty("y", Double.valueOf(current.posY));
        state.addProperty("z", Double.valueOf(current.posZ));
        state.addProperty("yaw", Float.valueOf(current.rotationYaw));
        state.addProperty("pitch", Float.valueOf(current.rotationPitch));
        state.addProperty("inWorld", Boolean.valueOf(current.isEntityAlive() && current.worldObj != null
                && current.worldObj.getEntityByID(AVATAR_ENTITY_ID) == current));
        StringBuilder equipment = new StringBuilder();
        for (int slot = 0; slot < 5; slot++) {
            ItemStack held = current.getEquipmentInSlot(slot);
            equipment.append(slot == 0 ? "" : ", ").append(held == null ? "-" : held.getDisplayName());
        }
        state.addProperty("equipment", equipment.toString());
        return state;
    }

    public synchronized void start(File replay) {
        if (active) {
            stop();
        }
        ReplayFile.Header header;
        try {
            header = ReplayFile.openForRead(replay);
        } catch (Exception failure) {
            ClientLogger.error("replay", "cannot open " + replay.getName() + ": " + failure.getMessage());
            return;
        }

        this.file = replay;
        this.recorderProfile = new GameProfile(header.recorderId, header.recorderName);
        this.durationMillis = 0;
        this.elapsedMillis = 0;
        this.pausedAtMillis = 0;
        this.paused = false;
        this.possessing = false;
        this.avatar = null;
        this.lastWorld = null;
        this.lastCameraPlayer = null;
        this.pending = null;
        this.readerFinished = false;
        this.loggedFailures = 0;
        queue.clear();

        openWorld(header);
        active = true;
        originNanos = System.nanoTime();

        readerThread = new Thread(new Reader(header), "Edge-ReplayReader");
        readerThread.setDaemon(true);
        readerThread.start();
        ClientLogger.info("replay", "playing " + replay.getName() + " recorded by " + header.recorderName);
    }

    /**
     * Builds the world the recording will be poured into.
     *
     * <p>This is what {@code handleJoinGame} does, minus its one Forge call: that resolves the
     * dimension through the connection's Netty channel, and playback has no channel.
     */
    private void openWorld(ReplayFile.Header header) {
        Minecraft mc = Minecraft.getMinecraft();
        NetworkManager connection = new SilentConnection();
        netHandler = new NetHandlerPlayClient(mc, null, connection, recorderProfile);
        connection.setNetHandler(netHandler);

        mc.playerController = new PlayerControllerMP(mc, netHandler);
        WorldClient world = new WorldClient(netHandler,
                new WorldSettings(0L, WorldSettings.GameType.SPECTATOR, false, false, WorldType.DEFAULT),
                header.dimension, EnumDifficulty.NORMAL, mc.mcProfiler);
        ((NetHandlerPlayClientAccessor) netHandler).setClientWorldController(world);

        mc.loadWorld(world);
        mc.thePlayer.dimension = header.dimension;
        mc.thePlayer.setEntityId(CAMERA_ENTITY_ID);
        mc.playerController.setGameType(WorldSettings.GameType.SPECTATOR);
        mc.displayGuiScreen(null);
    }

    public synchronized void stop() {
        if (!active) {
            return;
        }
        active = false;
        if (readerThread != null) {
            readerThread.interrupt();
            try {
                readerThread.join(2000L);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
            }
            readerThread = null;
        }
        queue.clear();
        pending = null;
        avatar = null;
        possessing = false;
        netHandler = null;

        // Unloading the world leaves no world, no player and no render view entity. Vanilla never
        // ends up in that state without a screen on top - every disconnect path puts one there - and
        // the rest of the tick and the frame after it assume as much. Ending playback without one
        // crashed the client; landing back in the browser is also where you want to be.
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.theWorld != null) {
            mc.loadWorld(null);
        }
        // Only when nothing else is up. Reaching the end of a recording leaves no screen at all and
        // the client cannot survive that, but someone who pressed Disconnect has already been sent
        // somewhere and should not be pulled out of it.
        if (mc.currentScreen == null) {
            mc.displayGuiScreen(new ReplayScreen(null));
        }
        ClientLogger.info("replay", "playback stopped");
    }

    public void togglePause() {
        if (!active) {
            return;
        }
        if (paused) {
            // Restart the clock where it stopped rather than where it would have been.
            originNanos = System.nanoTime() - pausedAtMillis * 1_000_000L;
            paused = false;
        } else {
            pausedAtMillis = elapsedMillis;
            paused = true;
        }
    }

    /**
     * Puts the viewer back together after the recording changes world.
     *
     * <p>A dimension change in the stream runs vanilla's respawn handler, which builds a new world,
     * replaces {@code mc.thePlayer} outright and sets the game type to whatever the recording says
     * the recorder was in. That undoes everything playback relies on: the camera stops being a
     * spectator, so it falls and cannot fly; the avatar belongs to the previous world, so it is
     * neither rendered nor updated; and possession keeps pointing at it. It also leaves the terrain
     * loading screen up, because the packet that would dismiss it is one playback drops.
     */
    private void reestablish(Minecraft mc) {
        lastWorld = mc.theWorld;
        lastCameraPlayer = mc.thePlayer;
        avatar = null;
        possessing = false;
        mc.thePlayer.setEntityId(CAMERA_ENTITY_ID);
        mc.playerController.setGameType(WorldSettings.GameType.SPECTATOR);
        mc.setRenderViewEntity(mc.thePlayer);
        if (mc.currentScreen instanceof net.minecraft.client.gui.GuiDownloadTerrain) {
            mc.displayGuiScreen(null);
        }
    }

    /** Delivers everything the recording says has happened by now. Called once per client tick. */
    public void onClientTick() {
        if (!active) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.theWorld == null || mc.thePlayer == null) {
            // Something unloaded the world under us - the in-game menu's Disconnect, or a failure
            // elsewhere. Staying "active" would leave the reader thread running and the playback
            // overlay drawn over whatever screen came next.
            stop();
            return;
        }
        if (mc.theWorld != lastWorld || mc.thePlayer != lastCameraPlayer) {
            reestablish(mc);
        }
        boolean pauseDown = mc.currentScreen == null && Keyboard.isKeyDown(Keyboard.KEY_P);
        if (pauseDown && !pauseWasDown) {
            togglePause();
        }
        pauseWasDown = pauseDown;

        if (!paused) {
            elapsedMillis = (int) ((System.nanoTime() - originNanos) / 1_000_000L);
            drain();
        }
        updateCamera(mc);
    }

    private void drain() {
        while (true) {
            if (pending == null) {
                pending = queue.poll();
            }
            if (pending == null) {
                if (readerFinished) {
                    ClientLogger.info("replay", "reached the end of " + file.getName());
                    stop();
                }
                return;
            }
            if (pending.millis > elapsedMillis) {
                return;
            }
            apply(pending);
            pending = null;
        }
    }

    /**
     * True for packets that reconfigure the client as the player it was recorded from.
     *
     * <p>The viewer is a spectator, not the recorder. These packets would change the viewer's game
     * mode to whatever the recorder was in - which is what turns the hotbar, health and armour bar
     * back on and takes away the free camera's flight - or overwrite its movement abilities
     * directly. What the recorder's own client did with them is not the viewer's business.
     */
    private static boolean rewritesTheViewer(Packet<?> packet) {
        if (packet instanceof S39PacketPlayerAbilities) {
            return true;
        }
        // Reason 3 is "change game mode"; the others are weather, credits and demo messages.
        return packet instanceof S2BPacketChangeGameState
                && ((S2BPacketChangeGameState) packet).getGameState() == GAME_STATE_CHANGE_GAME_MODE;
    }

    /**
     * Gives a spawning player a tab-list entry when the recording never provided one.
     *
     * <p>handleSpawnPlayer reads getPlayerInfo(uuid).getGameProfile() with no null check, so a
     * player the tab list does not name cannot be spawned at all - the packet throws and they are
     * simply absent. That is not rare on a real server: of 37 players spawning in a recorded
     * Hypixel lobby, 21 were never named, because entries are added and removed again before the
     * recording started and the removal is what the recording caught.
     *
     * <p>A recording is a partial view of a session, and playback should fill the gaps rather than
     * drop what it cannot explain. The stand-in carries no skin properties, so the player renders
     * with the default skin for their id - which is what the real client falls back to anyway when
     * a profile has no textures.
     */
    private void ensureNamed(S0CPacketSpawnPlayer packet) {
        UUID id = packet.getPlayer();
        if (id == null || netHandler.getPlayerInfo(id) != null) {
            return;
        }
        PacketBuffer buffer = new PacketBuffer(Unpooled.buffer());
        try {
            buffer.writeVarIntToBuffer(0);  // ADD_PLAYER
            buffer.writeVarIntToBuffer(1);
            buffer.writeUuid(id);
            buffer.writeString(id.toString().substring(0, 8));
            buffer.writeVarIntToBuffer(0);  // no properties
            buffer.writeVarIntToBuffer(0);  // survival
            buffer.writeVarIntToBuffer(0);  // no ping
            buffer.writeBoolean(false);     // no display name
            Packet<?> tabEntry = EnumConnectionState.PLAY.getPacket(
                    EnumPacketDirection.CLIENTBOUND, PLAYER_LIST_ITEM_ID);
            tabEntry.readPacketData(buffer);
            ((Packet) tabEntry).processPacket(netHandler);
        } catch (Exception failure) {
            ClientLogger.error("replay", "could not name " + id + ": " + failure);
        } finally {
            buffer.release();
        }
    }

    private void apply(Frame frame) {
        if (frame.packet instanceof S08PacketPlayerPosLook) {
            // Addressed to the recorder, but playback has no recorder entity to address - it would
            // teleport the viewer's free camera instead, every time the recording moved. The avatar
            // gets its position from the movement track; the camera is placed when it is created.
            return;
        }
        if (rewritesTheViewer(frame.packet)) {
            return;
        }
        if (frame.packet instanceof S0CPacketSpawnPlayer) {
            ensureNamed((S0CPacketSpawnPlayer) frame.packet);
        }
        if (frame.packet instanceof S2DPacketOpenWindow && !possessing) {
            // A chest the recorder opened should not take over the screen of someone flying around
            // watching them. Possess them and you get their interface.
            return;
        }
        if (frame.packet != null) {
            try {
                // Raw cast: Packet's handler type is erased, and the only handler in play is ours.
                ((Packet) frame.packet).processPacket(netHandler);
            } catch (Exception failure) {
                // With the stack for the first few: the message alone says a packet failed but not
                // which field was missing, and that is the whole question when one does.
                ClientLogger.error("replay", "could not apply "
                        + frame.packet.getClass().getSimpleName() + ": " + failure);
                if (loggedFailures++ < MAX_LOGGED_FAILURES) {
                    failure.printStackTrace();
                }
            }
            return;
        }
        if (frame.windowId >= 0) {
            applyContainerSlot(frame);
            return;
        }
        if (frame.slot >= 0) {
            applyEquipment(frame);
            return;
        }
        applyLocalSample(frame);
    }

    /**
     * Puts a recorded slot back into whatever container is open, when it is the same one.
     *
     * <p>The window id has to match. A chest is only opened while its owner is possessed, so free
     * flying leaves the player's own inventory open under window zero and a chest's slots are
     * skipped rather than written into the wrong container.
     */
    private void applyContainerSlot(Frame frame) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null || mc.thePlayer.openContainer == null
                || mc.thePlayer.openContainer.windowId != frame.windowId
                || frame.slot >= mc.thePlayer.openContainer.inventorySlots.size()) {
            return;
        }
        mc.thePlayer.openContainer.putStackInSlot(frame.slot, frame.stack);
    }

    private void applyEquipment(Frame frame) {
        if (avatar == null) {
            // Equipment is written after the position sample that spawns the avatar, so this only
            // happens if a recording was cut between the two.
            return;
        }
        avatar.setCurrentItemOrArmor(frame.slot, frame.stack);
    }

    /** Drives the avatar from the recorder's own movement track. */
    private void applyLocalSample(Frame frame) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.theWorld == null) {
            return;
        }
        if (avatar == null) {
            avatar = new EntityOtherPlayerMP(mc.theWorld, recorderProfile);
            avatar.setEntityId(AVATAR_ENTITY_ID);
            avatar.setPositionAndRotation(frame.x, frame.y, frame.z, frame.yaw, frame.pitch);
            avatar.rotationYawHead = frame.yaw;
            avatar.prevRotationYawHead = frame.yaw;
            mc.theWorld.addEntityToWorld(AVATAR_ENTITY_ID, avatar);
            // Put the free camera where the recorder was, otherwise it starts at the world origin
            // and the viewer opens on nothing.
            mc.thePlayer.setPositionAndRotation(frame.x, frame.y, frame.z, frame.yaw, frame.pitch);
        }
        // One increment: samples are one client tick apart, so the avatar lands exactly on each
        // sample and the renderer interpolates between them the same way it does for any player.
        avatar.setPositionAndRotation2(frame.x, frame.y, frame.z, frame.yaw, frame.pitch, 1, false);
        avatar.rotationYawHead = frame.yaw;
        avatar.onGround = (frame.flags & ReplayFile.FLAG_ON_GROUND) != 0;
        avatar.setSneaking((frame.flags & ReplayFile.FLAG_SNEAKING) != 0);
        avatar.setSprinting((frame.flags & ReplayFile.FLAG_SPRINTING) != 0);
        if ((frame.flags & ReplayFile.FLAG_SWINGING) != 0 && !avatar.isSwingInProgress) {
            avatar.swingItem();
        }
        // The server never says a container was closed - the client tells it. Mirror what the
        // recorder had open, or the chest they opened stays on screen for the rest of the replay.
        if ((frame.flags & ReplayFile.FLAG_SCREEN_OPEN) == 0
                && mc.currentScreen instanceof net.minecraft.client.gui.inventory.GuiContainer) {
            mc.displayGuiScreen(null);
        }
    }

    private void updateCamera(Minecraft mc) {
        boolean attackDown = mc.gameSettings.keyBindAttack.isKeyDown();
        boolean sneakDown = mc.gameSettings.keyBindSneak.isKeyDown();

        if (attackDown && !attackWasDown && !possessing && mc.currentScreen == null && lookingAtAvatar(mc)) {
            possess();
        } else if (sneakDown && !sneakWasDown && possessing) {
            release();
        }
        attackWasDown = attackDown;
        sneakWasDown = sneakDown;

        if (possessing && avatar != null) {
            // The view comes from the avatar, so the mouse cannot turn it. Dragging the hidden
            // camera along keeps it facing the same way when possession is released.
            mc.thePlayer.setPositionAndRotation(avatar.posX, avatar.posY, avatar.posZ,
                    avatar.rotationYaw, avatar.rotationPitch);
            mc.thePlayer.motionX = 0.0d;
            mc.thePlayer.motionY = 0.0d;
            mc.thePlayer.motionZ = 0.0d;
        } else if (possessing) {
            release();
        }
    }

    /** Moves the view onto the recorder. Their rotation drives the camera; the mouse does not. */
    public void possess() {
        if (!active || avatar == null || possessing) {
            return;
        }
        possessing = true;
        Minecraft.getMinecraft().setRenderViewEntity(avatar);
    }

    public void release() {
        if (!possessing) {
            return;
        }
        possessing = false;
        Minecraft.getMinecraft().setRenderViewEntity(Minecraft.getMinecraft().thePlayer);
    }

    private boolean lookingAtAvatar(Minecraft mc) {
        if (avatar == null) {
            return false;
        }
        Vec3 eyes = mc.thePlayer.getPositionEyes(1.0f);
        Vec3 look = mc.thePlayer.getLook(1.0f);
        Vec3 reach = eyes.addVector(look.xCoord * POSSESS_REACH, look.yCoord * POSSESS_REACH,
                look.zCoord * POSSESS_REACH);
        // Generous box: the avatar is a person-sized target that may be a long way off, and asking
        // for pixel accuracy at that distance would make possession a game of its own.
        AxisAlignedBB box = avatar.getEntityBoundingBox().expand(0.5d, 0.5d, 0.5d);
        MovingObjectPosition hit = box.calculateIntercept(eyes, reach);
        return hit != null;
    }

    /** Reads and decodes off the client thread, so playback only pays for handling. */
    private final class Reader implements Runnable {
        private final ReplayFile.Header header;

        Reader(ReplayFile.Header header) {
            this.header = header;
        }

        @Override
        public void run() {
            try {
                ReplayFile.Record record;
                while (active && (record = ReplayFile.read(header)) != null) {
                    durationMillis = Math.max(durationMillis, record.millis);
                    Frame frame = decode(record);
                    if (frame == null) {
                        continue;
                    }
                    while (active && !queue.offer(frame, 100L, java.util.concurrent.TimeUnit.MILLISECONDS)) {
                        // Playback is paused or behind; hold here rather than grow without bound.
                    }
                }
            } catch (InterruptedException stopped) {
                Thread.currentThread().interrupt();
            } catch (Exception failure) {
                ClientLogger.error("replay", "reader failed: " + failure);
            } finally {
                readerFinished = true;
                try {
                    header.stream.close();
                } catch (Exception closeFailure) {
                    ClientLogger.error("replay", "could not close the recording: " + closeFailure);
                }
            }
        }

        private Frame decode(ReplayFile.Record record) {
            if (record.type == ReplayFile.TYPE_LOCAL_PLAYER) {
                return new Frame(record);
            }
            if (record.type == ReplayFile.TYPE_LOCAL_EQUIPMENT
                    || record.type == ReplayFile.TYPE_CONTAINER_SLOT) {
                try {
                    PacketBuffer buffer = new PacketBuffer(Unpooled.wrappedBuffer(record.payload));
                    return new Frame(record.millis, record.windowId, record.slot,
                            buffer.readItemStackFromBuffer());
                } catch (Exception failure) {
                    return null;
                }
            }
            try {
                Packet<?> packet = EnumConnectionState.PLAY.getPacket(
                        EnumPacketDirection.CLIENTBOUND, record.packetId);
                if (packet == null) {
                    return null;
                }
                packet.readPacketData(new PacketBuffer(Unpooled.wrappedBuffer(record.payload)));
                return new Frame(record.millis, packet);
            } catch (Exception failure) {
                // One unreadable packet degrades the replay; aborting it loses the session.
                return null;
            }
        }
    }

    private static final class Frame {
        final int millis;
        final Packet<?> packet;
        final int slot;
        final int windowId;
        final ItemStack stack;
        final double x;
        final double y;
        final double z;
        final float yaw;
        final float pitch;
        final int flags;

        Frame(int millis, Packet<?> packet) {
            this.millis = millis;
            this.packet = packet;
            this.slot = -1;
            this.windowId = -1;
            this.stack = null;
            this.x = 0.0d;
            this.y = 0.0d;
            this.z = 0.0d;
            this.yaw = 0.0f;
            this.pitch = 0.0f;
            this.flags = 0;
        }

        Frame(int millis, int windowId, int slot, ItemStack stack) {
            this.millis = millis;
            this.packet = null;
            this.slot = slot;
            this.windowId = windowId;
            this.stack = stack;
            this.x = 0.0d;
            this.y = 0.0d;
            this.z = 0.0d;
            this.yaw = 0.0f;
            this.pitch = 0.0f;
            this.flags = 0;
        }

        Frame(ReplayFile.Record record) {
            this.millis = record.millis;
            this.packet = null;
            this.slot = -1;
            this.windowId = -1;
            this.stack = null;
            this.x = record.x;
            this.y = record.y;
            this.z = record.z;
            this.yaw = record.yaw;
            this.pitch = record.pitch;
            this.flags = record.flags;
        }
    }

    /** A connection with nowhere to send. Outbound packets are dropped instead of queued forever. */
    private static final class SilentConnection extends NetworkManager {
        SilentConnection() {
            super(EnumPacketDirection.CLIENTBOUND);
        }

        @Override
        public void sendPacket(Packet packet) {
        }

        @Override
        public void sendPacket(Packet packet,
                io.netty.util.concurrent.GenericFutureListener<? extends io.netty.util.concurrent.Future<? super Void>> listener,
                io.netty.util.concurrent.GenericFutureListener<? extends io.netty.util.concurrent.Future<? super Void>>... listeners) {
        }

        /**
         * Vanilla dereferences the channel here without checking it exists - unlike isChannelOpen
         * right next to it. Leaving a replay through the in-game menu goes
         * Disconnect -> WorldClient.sendQuittingDisconnectingPacket -> closeChannel, so it threw
         * before unloading anything and left the client stuck in the session it had just left.
         */
        @Override
        public void closeChannel(IChatComponent message) {
        }

        /** Ends in channel.flush(). Nothing drives it during playback, but the contract is the same. */
        @Override
        public void processReceivedPackets() {
        }
    }
}

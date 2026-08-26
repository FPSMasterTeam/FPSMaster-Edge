package top.fpsmaster.replay;

import io.netty.buffer.Unpooled;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.entity.DataWatcher;
import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;
import net.minecraft.network.EnumConnectionState;
import net.minecraft.network.EnumPacketDirection;
import net.minecraft.network.Packet;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.server.S0CPacketSpawnPlayer;
import net.minecraft.network.play.server.S0FPacketSpawnMob;
import net.minecraft.network.play.server.S38PacketPlayerListItem;
import top.fpsmaster.modules.logger.ClientLogger;
import top.fpsmaster.utils.io.FileUtils;

import java.io.DataOutputStream;
import java.io.File;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * Records the inbound packet stream so a real session can be replayed as a benchmark workload.
 *
 * <p>Every conclusion in this project so far comes from synthetic scenes: a superflat world with
 * armour stands standing still. Those cannot produce the things that actually cost a PvP client —
 * players with individual skin textures, capes and nameplates, server-driven particles, scoreboard
 * and tab text, or the burst of chunk data when a map loads. Replaying a recorded stream does.
 *
 * <h3>Threading</h3>
 *
 * <p>Packets are serialised on the network thread, which is a memcpy, and handed to a writer thread
 * that does the compression and the disk I/O. Doing either of those inline would add latency to
 * packet handling and change the very timing being recorded. If the queue backs up the recording is
 * stopped rather than allowed to stall the network thread — a truncated recording is recoverable, a
 * lagging connection during capture is not.
 *
 * <h3>Starting mid-session</h3>
 *
 * <p>Chunks and entities already loaded are never re-sent, so a recording started in a world that is
 * already loaded would replay into empty space. Loaded chunks are therefore synthesised into the
 * stream at the start. Entities are not: reconstructing a spawn packet per entity type is a lot of
 * surface for something with a simpler answer — start recording <em>before</em> joining the target
 * world and the server streams everything naturally.
 */
public final class ReplayRecorder {

    /** Bounded so a stalled disk cannot turn into unbounded heap growth during a match. */
    private static final int QUEUE_CAPACITY = 4096;

    /** How much of a recording a hard crash may cost. */
    private static final long FLUSH_INTERVAL_MILLIS = 2000L;

    private static final ReplayRecorder INSTANCE = new ReplayRecorder();

    private final BlockingQueue<ReplayFile.Record> queue =
            new ArrayBlockingQueue<ReplayFile.Record>(QUEUE_CAPACITY);

    /** Held item plus the four armour pieces, numbered as the equipment packet numbers them. */
    private static final int EQUIPMENT_SLOTS = 5;

    private final ItemStack[] lastEquipment = new ItemStack[EQUIPMENT_SLOTS];

    private int lastWindowId = -1;
    private ItemStack[] lastSlots;

    /**
     * Time for the world to fill in before an automated capture snapshots it.
     *
     * <p>Overridable because a test that wants to prove the snapshot captured something has to start
     * recording after that something exists, and a benchmark scenario sets its world up on its own
     * schedule.
     */
    private static final long AUTO_START_DELAY_MILLIS =
            Long.getLong("edge.replay.recordDelay", 8L).longValue() * 1000L;

    private long autoStartFirstSeen;
    private boolean autoStartDone;
    private volatile boolean recording;
    private Thread writerThread;
    private File file;
    private long startMillis;
    private int packetsRecorded;
    private int packetsDropped;
    private int playersSpawned;
    private final java.util.Set<java.util.UUID> playersNamed = new java.util.HashSet<java.util.UUID>();

    /** Packet types already reported as unserialisable, so each is complained about once. */
    private final java.util.Set<String> serialisationFailures = new java.util.HashSet<String>();

    private ReplayRecorder() {
    }

    public static ReplayRecorder instance() {
        return INSTANCE;
    }

    /**
     * Starts recording if {@code -Dedge.replay.record=<name>} was passed.
     *
     * <p>Exists so the capture path can be exercised without a human typing a chat command, which is
     * both how it gets tested and how an automated capture would drive it.
     */
    public void startIfRequested() {
        String requested = System.getProperty("edge.replay.record");
        if (requested == null || requested.isEmpty() || recording || autoStartDone) {
            return;
        }
        // Wait for the world to actually populate. Firing on the first tick where theWorld is
        // non-null snapshots an empty world: no chunks have arrived and no entities exist yet, so
        // the recording opens on nothing.
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.theWorld == null || mc.thePlayer == null) {
            return;
        }
        if (autoStartFirstSeen == 0L) {
            autoStartFirstSeen = System.currentTimeMillis();
            return;
        }
        if (System.currentTimeMillis() - autoStartFirstSeen < AUTO_START_DELAY_MILLIS) {
            return;
        }
        autoStartDone = true;
        start(requested);
    }

    public boolean isRecording() {
        return recording;
    }

    public File currentFile() {
        return file;
    }

    public int packetsRecorded() {
        return packetsRecorded;
    }

    public int packetsDropped() {
        return packetsDropped;
    }

    /**
     * Size of the recording so far, as it stands on disk.
     *
     * <p>Counting bytes handed to the writer would report the uncompressed volume, which for a
     * recording of real terrain is forty times the file - so the display said 70 MB while the file
     * was 1.7 MB. The number people care about is the one the disk shows.
     */
    public long bytesWritten() {
        return file == null ? 0L : file.length();
    }

    public long elapsedMillis() {
        return recording ? System.currentTimeMillis() - startMillis : 0L;
    }

    public synchronized void start(String name) {
        if (recording) {
            return;
        }
        File directory = new File(FileUtils.dir, "replays");
        if (!directory.isDirectory() && !directory.mkdirs()) {
            ClientLogger.error("replay", "could not create " + directory);
            return;
        }
        file = new File(directory, FileUtils.fixName(name) + ".edgereplay");
        startMillis = System.currentTimeMillis();
        packetsRecorded = 0;
        packetsDropped = 0;
        playersSpawned = 0;
        playersNamed.clear();
        serialisationFailures.clear();
        java.util.Arrays.fill(lastEquipment, null);
        lastWindowId = -1;
        lastSlots = null;
        queue.clear();
        recording = true;

        Minecraft mc = Minecraft.getMinecraft();
        writerThread = new Thread(top.fpsmaster.benchmark.BenchCounters.trackWorker(
                new Writer(file, startMillis, mc.getSession().getUsername(),
                        mc.thePlayer == null ? mc.getSession().getProfile().getId()
                                : mc.thePlayer.getGameProfile().getId(),
                        mc.thePlayer == null ? 0 : mc.thePlayer.dimension)), "Edge-ReplayWriter");
        writerThread.setDaemon(true);
        writerThread.start();

        ReplaySnapshot.capture(mc, snapshotSink);
        // The snapshot writes the tab list as raw bytes, which never passes through offer(), so the
        // players it named have to be counted here or the report understates what was captured.
        if (mc.getNetHandler() != null) {
            for (net.minecraft.client.network.NetworkPlayerInfo info : mc.getNetHandler().getPlayerInfoMap()) {
                if (info.getGameProfile() != null && info.getGameProfile().getId() != null) {
                    playersNamed.add(info.getGameProfile().getId());
                }
            }
        }
        ClientLogger.info("replay", "recording to " + file.getName());
    }

    public synchronized void stop() {
        if (!recording) {
            return;
        }
        recording = false;
        try {
            writerThread.join(5000L);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        }
        ClientLogger.info("replay", "stopped after " + packetsRecorded + " packet(s), "
                + bytesWritten() / 1024L + " KiB" + (packetsDropped > 0
                ? ", " + packetsDropped + " dropped" : ""));
        ClientLogger.info("replay", "captured " + playersSpawned + " player spawn(s), "
                + playersNamed.size() + " player(s) named in the tab list");
    }

    /**
     * Snapshot output. Raw entries exist because some packets cannot be constructed on a client and
     * are written as protocol bytes instead — see {@link ReplaySnapshot}.
     */
    private final ReplaySnapshot.Sink snapshotSink = new ReplaySnapshot.Sink() {
        @Override
        public void accept(Packet<?> packet) {
            offer(packet);
        }

        @Override
        public void acceptRaw(int packetId, byte[] payload) {
            enqueue(new ReplayFile.Record(elapsed(), packetId, payload));
        }
    };

    /**
     * Samples the recording player's own position and action state.
     *
     * <p>Called once per client tick. The server never sends you your own movement, so replaying a
     * session with the recorder visible in it — as an avatar to watch or to attach the camera to —
     * means capturing this separately from the packet stream.
     */
    public void onClientTick() {
        if (!recording) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null) {
            return;
        }
        int flags = 0;
        if (mc.thePlayer.onGround) {
            flags |= ReplayFile.FLAG_ON_GROUND;
        }
        if (mc.thePlayer.isSneaking()) {
            flags |= ReplayFile.FLAG_SNEAKING;
        }
        if (mc.thePlayer.isSprinting()) {
            flags |= ReplayFile.FLAG_SPRINTING;
        }
        if (mc.thePlayer.isSwingInProgress) {
            flags |= ReplayFile.FLAG_SWINGING;
        }
        if (mc.currentScreen instanceof net.minecraft.client.gui.inventory.GuiContainer) {
            flags |= ReplayFile.FLAG_SCREEN_OPEN;
        }
        enqueue(new ReplayFile.Record(elapsed(), mc.thePlayer.posX, mc.thePlayer.posY,
                mc.thePlayer.posZ, mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch, flags));
        sampleEquipment(mc);
        sampleContainer(mc);
    }

    /**
     * Records the contents of an open container as they change.
     *
     * <p>Moving an item is invisible in a server-to-client recording. On an accepted click the
     * server sets isChangingQuantityOnly before detectAndSendChanges, and sendSlotContents checks
     * that flag - so it sends a transaction confirmation and nothing else, leaving the real client
     * to apply the prediction it made from its own click packet. That packet travels the other way
     * and is not recorded, so the result has to be observed rather than derived.
     *
     * <p>Diffed against the previous tick, like equipment, because a container is mostly still. The
     * first sight of a window is taken as a baseline and not written: the server has just sent the
     * whole thing in S30PacketWindowItems, and repeating it would double what opening a chest costs
     * for nothing.
     */
    private void sampleContainer(Minecraft mc) {
        if (!(mc.currentScreen instanceof GuiContainer) || mc.thePlayer.openContainer == null) {
            lastWindowId = -1;
            lastSlots = null;
            return;
        }
        Container container = mc.thePlayer.openContainer;
        int size = container.inventorySlots.size();
        if (container.windowId != lastWindowId || lastSlots == null || lastSlots.length != size) {
            lastWindowId = container.windowId;
            lastSlots = new ItemStack[size];
            for (int slot = 0; slot < size; slot++) {
                ItemStack stack = container.getSlot(slot).getStack();
                lastSlots[slot] = stack == null ? null : stack.copy();
            }
            return;
        }
        for (int slot = 0; slot < size; slot++) {
            ItemStack stack = container.getSlot(slot).getStack();
            if (ItemStack.areItemStacksEqual(stack, lastSlots[slot])) {
                continue;
            }
            lastSlots[slot] = stack == null ? null : stack.copy();

            PacketBuffer buffer = new PacketBuffer(Unpooled.buffer());
            try {
                buffer.writeItemStackToBuffer(stack);
                byte[] payload = new byte[buffer.readableBytes()];
                buffer.readBytes(payload);
                enqueue(ReplayFile.Record.containerSlot(elapsed(), container.windowId, slot, payload));
            } catch (Exception failure) {
                ClientLogger.error("replay", "could not record container slot " + slot + ": " + failure);
            } finally {
                buffer.release();
            }
        }
    }

    /**
     * Records what the player is holding and wearing, but only when it changes.
     *
     * <p>A held item is as visible as the player carrying it, and just as expensive to draw. It is
     * also almost always the same as it was a tick ago, so this writes a record on change rather than
     * five item stacks twenty times a second — which would have cost more than the entire rest of the
     * file. Damage and stack size count as changes, so a sword being worn down is followed too.
     */
    private void sampleEquipment(Minecraft mc) {
        for (int slot = 0; slot < EQUIPMENT_SLOTS; slot++) {
            ItemStack stack = mc.thePlayer.getEquipmentInSlot(slot);
            if (ItemStack.areItemStacksEqual(stack, lastEquipment[slot])) {
                continue;
            }
            lastEquipment[slot] = stack == null ? null : stack.copy();

            PacketBuffer buffer = new PacketBuffer(Unpooled.buffer());
            try {
                buffer.writeItemStackToBuffer(stack);
                byte[] payload = new byte[buffer.readableBytes()];
                buffer.readBytes(payload);
                enqueue(ReplayFile.Record.equipment(elapsed(), slot, payload));
            } catch (Exception failure) {
                ClientLogger.error("replay", "could not record slot " + slot + ": " + failure);
            } finally {
                buffer.release();
            }
        }
    }

    private int elapsed() {
        return (int) Math.min(Integer.MAX_VALUE, System.currentTimeMillis() - startMillis);
    }

    private void enqueue(ReplayFile.Record record) {
        if (!queue.offer(record)) {
            packetsDropped++;
            if (packetsDropped == 1) {
                ClientLogger.warn("replay: writer cannot keep up, recording will be truncated");
            }
        } else {
            packetsRecorded++;
        }
    }

    /** Called from the network thread for every inbound packet. */
    public void onPacket(Packet<?> packet) {
        if (!recording) {
            return;
        }
        offer(packet);
    }

    /**
     * Counts what the recording contains of other players, reported when it stops.
     *
     * <p>Both numbers matter and neither is obvious from the file size. handleSpawnPlayer looks the
     * profile up in the tab list without checking it exists, so a player who is spawned but never
     * named cannot appear during playback at all - the packet throws and they are simply missing.
     */
    private void countPlayers(Packet<?> packet) {
        if (packet instanceof S0CPacketSpawnPlayer) {
            playersSpawned++;
        } else if (packet instanceof S38PacketPlayerListItem) {
            S38PacketPlayerListItem list = (S38PacketPlayerListItem) packet;
            if (list.getAction() == S38PacketPlayerListItem.Action.ADD_PLAYER) {
                for (S38PacketPlayerListItem.AddPlayerData entry : list.getEntries()) {
                    if (entry.getProfile() != null && entry.getProfile().getId() != null) {
                        playersNamed.add(entry.getProfile().getId());
                    }
                }
            }
        }
    }

    private void offer(Packet<?> packet) {
        countPlayers(packet);
        Integer id = EnumConnectionState.PLAY.getPacketId(EnumPacketDirection.CLIENTBOUND, packet);
        if (id == null) {
            // Login and status packets are not part of a play-state recording.
            return;
        }
        byte[] payload;
        try {
            PacketBuffer buffer = new PacketBuffer(Unpooled.buffer());
            writePayload(packet, buffer);
            payload = new byte[buffer.readableBytes()];
            buffer.readBytes(payload);
            buffer.release();
        } catch (Exception failure) {
            // A packet that will not round-trip is dropped rather than allowed to abort the
            // recording; one missing packet degrades the replay, an exception here would end it.
            // Reported once per type, because that is the difference between noticing and not: the
            // spawn packets below failed on every single one and the only sign was a drop count
            // shared with queue overflow.
            packetsDropped++;
            if (serialisationFailures.add(packet.getClass().getSimpleName())) {
                ClientLogger.error("replay", "cannot serialise "
                        + packet.getClass().getSimpleName() + ", dropping every one: " + failure);
            }
            return;
        }

        enqueue(new ReplayFile.Record(elapsed(), id.intValue(), payload));
    }

    /**
     * Writes a packet's payload, by hand for the two whose own writer cannot handle a received one.
     *
     * <p>A spawn packet carries its metadata as a {@code DataWatcher} when the server builds it and
     * as a plain list when a client reads it, and {@code writePacketData} only knows about the
     * DataWatcher — so the field is null on everything that arrived over the network and every
     * player and mob the server ever spawned was thrown away here. What survived into a recording
     * were the spawns the snapshot built itself from live entities, which is why a replay showed
     * exactly the crowd that was standing there when recording started and nobody who arrived
     * after. {@code S1CPacketEntityMetadata} writes the same data through the static list writer
     * used below and has never had the problem.
     */
    private static void writePayload(Packet<?> packet, PacketBuffer buffer) throws Exception {
        if (packet instanceof S0CPacketSpawnPlayer) {
            S0CPacketSpawnPlayer spawn = (S0CPacketSpawnPlayer) packet;
            buffer.writeVarIntToBuffer(spawn.getEntityID());
            buffer.writeUuid(spawn.getPlayer());
            buffer.writeInt(spawn.getX());
            buffer.writeInt(spawn.getY());
            buffer.writeInt(spawn.getZ());
            buffer.writeByte(spawn.getYaw());
            buffer.writeByte(spawn.getPitch());
            buffer.writeShort(spawn.getCurrentItemID());
            DataWatcher.writeWatchedListToPacketBuffer(spawn.func_148944_c(), buffer);
            return;
        }
        if (packet instanceof S0FPacketSpawnMob) {
            S0FPacketSpawnMob spawn = (S0FPacketSpawnMob) packet;
            buffer.writeVarIntToBuffer(spawn.getEntityID());
            buffer.writeByte(spawn.getEntityType() & 0xFF);
            buffer.writeInt(spawn.getX());
            buffer.writeInt(spawn.getY());
            buffer.writeInt(spawn.getZ());
            buffer.writeByte(spawn.getYaw());
            buffer.writeByte(spawn.getPitch());
            buffer.writeByte(spawn.getHeadPitch());
            buffer.writeShort(spawn.getVelocityX());
            buffer.writeShort(spawn.getVelocityY());
            buffer.writeShort(spawn.getVelocityZ());
            DataWatcher.writeWatchedListToPacketBuffer(spawn.func_149027_c(), buffer);
            return;
        }
        packet.writePacketData(buffer);
    }

    private final class Writer implements Runnable {
        private final File target;
        private final long start;
        private final String recorderName;
        private final UUID recorderId;
        private final int dimension;

        Writer(File target, long start, String recorderName, UUID recorderId, int dimension) {
            this.target = target;
            this.start = start;
            this.recorderName = recorderName;
            this.recorderId = recorderId;
            this.dimension = dimension;
        }

        @Override
        public void run() {
            ReplayFile.Writer out = null;
            try {
                out = ReplayFile.openForWrite(target, "1.8.9", start, recorderName, recorderId, dimension);
                long lastFlush = System.currentTimeMillis();
                while (recording || !queue.isEmpty()) {
                    ReplayFile.Record record = queue.poll(200L, java.util.concurrent.TimeUnit.MILLISECONDS);
                    if (record != null) {
                        out.write(record);
                    }
                    // Bound how much a crash can cost. The stream is sync-flushed, so everything up
                    // to here stays readable even if the process never gets to close it.
                    if (System.currentTimeMillis() - lastFlush >= FLUSH_INTERVAL_MILLIS) {
                        out.flush();
                        lastFlush = System.currentTimeMillis();
                    }
                }
            } catch (Exception failure) {
                ClientLogger.error("replay", "writer failed: " + failure);
                recording = false;
            } finally {
                if (out != null) {
                    try {
                        out.close();
                    } catch (Exception closeFailure) {
                        ClientLogger.error("replay", "could not close " + target + ": " + closeFailure);
                    }
                }
            }
        }
    }
}

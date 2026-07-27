package top.fpsmaster.replay;

import io.netty.buffer.Unpooled;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
import net.minecraft.network.EnumConnectionState;
import net.minecraft.network.EnumPacketDirection;
import net.minecraft.network.Packet;
import net.minecraft.network.PacketBuffer;
import top.fpsmaster.modules.logger.ClientLogger;
import top.fpsmaster.utils.io.FileUtils;

import java.io.DataOutputStream;
import java.io.File;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

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
    private final AtomicLong bytesWritten = new AtomicLong();

    /** Held item plus the four armour pieces, numbered as the equipment packet numbers them. */
    private static final int EQUIPMENT_SLOTS = 5;

    private final ItemStack[] lastEquipment = new ItemStack[EQUIPMENT_SLOTS];

    /** Time for chunks and entities to arrive before an automated capture snapshots them. */
    private static final long AUTO_START_DELAY_MILLIS = 8000L;

    private long autoStartFirstSeen;
    private boolean autoStartDone;
    private volatile boolean recording;
    private Thread writerThread;
    private File file;
    private long startMillis;
    private int packetsRecorded;
    private int packetsDropped;

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

    public long bytesWritten() {
        return bytesWritten.get();
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
        java.util.Arrays.fill(lastEquipment, null);
        bytesWritten.set(0L);
        queue.clear();
        recording = true;

        Minecraft mc = Minecraft.getMinecraft();
        writerThread = new Thread(new Writer(file, startMillis, mc.getSession().getUsername(),
                mc.thePlayer == null ? mc.getSession().getProfile().getId()
                        : mc.thePlayer.getGameProfile().getId(),
                mc.thePlayer == null ? 0 : mc.thePlayer.dimension), "Edge-ReplayWriter");
        writerThread.setDaemon(true);
        writerThread.start();

        ReplaySnapshot.capture(Minecraft.getMinecraft(), snapshotSink);
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
                + bytesWritten.get() / 1024L + " KiB" + (packetsDropped > 0
                ? ", " + packetsDropped + " dropped" : ""));
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
        enqueue(new ReplayFile.Record(elapsed(), mc.thePlayer.posX, mc.thePlayer.posY,
                mc.thePlayer.posZ, mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch, flags));
        sampleEquipment(mc);
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

    private void offer(Packet<?> packet) {
        Integer id = EnumConnectionState.PLAY.getPacketId(EnumPacketDirection.CLIENTBOUND, packet);
        if (id == null) {
            // Login and status packets are not part of a play-state recording.
            return;
        }
        byte[] payload;
        try {
            PacketBuffer buffer = new PacketBuffer(Unpooled.buffer());
            packet.writePacketData(buffer);
            payload = new byte[buffer.readableBytes()];
            buffer.readBytes(payload);
            buffer.release();
        } catch (Exception failure) {
            // A packet that will not round-trip is dropped rather than allowed to abort the
            // recording; one missing packet degrades the replay, an exception here would end it.
            packetsDropped++;
            return;
        }

        enqueue(new ReplayFile.Record(elapsed(), id.intValue(), payload));
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
            DataOutputStream out = null;
            try {
                out = ReplayFile.openForWrite(target, "1.8.9", start, recorderName, recorderId, dimension);
                long lastFlush = System.currentTimeMillis();
                while (recording || !queue.isEmpty()) {
                    ReplayFile.Record record = queue.poll(200L, java.util.concurrent.TimeUnit.MILLISECONDS);
                    if (record != null) {
                        ReplayFile.write(out, record);
                        bytesWritten.addAndGet(ReplayFile.sizeOf(record));
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

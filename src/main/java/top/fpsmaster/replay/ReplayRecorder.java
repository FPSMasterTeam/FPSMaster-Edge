package top.fpsmaster.replay;

import io.netty.buffer.Unpooled;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ChunkProviderClient;
import net.minecraft.network.EnumConnectionState;
import net.minecraft.network.EnumPacketDirection;
import net.minecraft.network.Packet;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.server.S21PacketChunkData;
import net.minecraft.world.chunk.Chunk;
import top.fpsmaster.forge.mixin.accessor.ChunkProviderClientAccessor;
import top.fpsmaster.modules.logger.ClientLogger;
import top.fpsmaster.utils.io.FileUtils;

import java.io.DataOutputStream;
import java.io.File;
import java.util.List;
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
        if (requested != null && !requested.isEmpty() && !recording) {
            start(requested);
        }
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
        bytesWritten.set(0L);
        queue.clear();
        recording = true;

        writerThread = new Thread(new Writer(file, startMillis), "Edge-ReplayWriter");
        writerThread.setDaemon(true);
        writerThread.start();

        captureLoadedChunks();
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

        int millis = (int) Math.min(Integer.MAX_VALUE, System.currentTimeMillis() - startMillis);
        if (!queue.offer(new ReplayFile.Record(millis, id.intValue(), payload))) {
            packetsDropped++;
            if (packetsDropped == 1) {
                ClientLogger.warn("replay: writer cannot keep up, recording will be truncated");
            }
        } else {
            packetsRecorded++;
        }
    }

    /**
     * Writes the chunks already loaded, so a recording started mid-world is not empty on playback.
     *
     * <p>Runs on the caller's thread at start, which is a visible hitch on a large view distance —
     * acceptable once, at a moment the user chose.
     */
    private void captureLoadedChunks() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.theWorld == null || !(mc.theWorld.getChunkProvider() instanceof ChunkProviderClient)) {
            return;
        }
        List<Chunk> chunks = ((ChunkProviderClientAccessor) mc.theWorld.getChunkProvider())
                .getChunkListing();
        int captured = 0;
        for (Chunk chunk : chunks) {
            if (chunk == null || !chunk.isLoaded()) {
                continue;
            }
            offer(new S21PacketChunkData(chunk, true, 0xFFFF));
            captured++;
        }
        ClientLogger.info("replay", "seeded " + captured + " already-loaded chunk(s)");
    }

    private final class Writer implements Runnable {
        private final File target;
        private final long start;

        Writer(File target, long start) {
            this.target = target;
            this.start = start;
        }

        @Override
        public void run() {
            DataOutputStream out = null;
            try {
                out = ReplayFile.openForWrite(target, "1.8.9", start);
                long lastFlush = System.currentTimeMillis();
                while (recording || !queue.isEmpty()) {
                    ReplayFile.Record record = queue.poll(200L, java.util.concurrent.TimeUnit.MILLISECONDS);
                    if (record != null) {
                        ReplayFile.write(out, record);
                        bytesWritten.addAndGet(record.payload.length + 12L);
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

package top.fpsmaster.replay;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * On-disk format for a recorded server-to-client packet stream.
 *
 * <p>Deliberately minimal. This exists to reproduce a real workload for benchmarking, not to be a
 * general replay archive, so there is no seek index, no keyframe table and no metadata beyond what
 * playback needs. Records are written in arrival order and read back the same way.
 *
 * <pre>
 *   header   magic "EDGEREPL" | int version | UTF minecraft version | long wall-clock start
 *   record   int millis-since-start | int packet id | int payload length | payload bytes
 * </pre>
 *
 * <p>Gzipped: chunk payloads dominate the volume and compress by roughly an order of magnitude, and
 * the cost is paid on a writer thread rather than on the network thread.
 */
public final class ReplayFile {

    static final String MAGIC = "EDGEREPL";
    static final int VERSION = 1;

    private ReplayFile() {
    }

    public static DataOutputStream openForWrite(File file, String minecraftVersion, long startMillis)
            throws IOException {
        // syncFlush so a periodic flush emits a complete deflate block. Without it a crash mid-match
        // leaves an unreadable stream and the whole session is gone; with it, everything up to the
        // last flush survives.
        DataOutputStream out = new DataOutputStream(
                new GZIPOutputStream(new BufferedOutputStream(new FileOutputStream(file), 1 << 16),
                        true));
        out.writeUTF(MAGIC);
        out.writeInt(VERSION);
        out.writeUTF(minecraftVersion);
        out.writeLong(startMillis);
        return out;
    }

    public static Header openForRead(File file) throws IOException {
        DataInputStream in = new DataInputStream(
                new GZIPInputStream(new BufferedInputStream(new FileInputStream(file), 1 << 16)));
        String magic = in.readUTF();
        if (!MAGIC.equals(magic)) {
            in.close();
            throw new IOException("not an Edge replay: " + file.getName());
        }
        int version = in.readInt();
        if (version != VERSION) {
            in.close();
            throw new IOException("replay version " + version + ", this build reads " + VERSION);
        }
        return new Header(in, in.readUTF(), in.readLong());
    }

    public static final class Header {
        public final DataInputStream stream;
        public final String minecraftVersion;
        public final long startMillis;

        Header(DataInputStream stream, String minecraftVersion, long startMillis) {
            this.stream = stream;
            this.minecraftVersion = minecraftVersion;
            this.startMillis = startMillis;
        }
    }

    /** One recorded packet. {@code payload} is the packet's own serialised form, without the id. */
    public static final class Record {
        public final int millis;
        public final int packetId;
        public final byte[] payload;

        public Record(int millis, int packetId, byte[] payload) {
            this.millis = millis;
            this.packetId = packetId;
            this.payload = payload;
        }
    }

    public static void write(DataOutputStream out, Record record) throws IOException {
        out.writeInt(record.millis);
        out.writeInt(record.packetId);
        out.writeInt(record.payload.length);
        out.write(record.payload);
    }

    /**
     * Reads the next record, or returns null once no complete record remains.
     *
     * <p>A truncated tail is treated as the end rather than an error. A recording cut short by a
     * crash is still worth everything that was written before it, and refusing to open it would
     * throw away the session that is hardest to capture again.
     */
    public static Record read(DataInputStream in) {
        try {
            int millis = in.readInt();
            int packetId = in.readInt();
            int length = in.readInt();
            if (length < 0 || length > MAX_PAYLOAD) {
                return null;  // garbage length: the stream ended inside a record header
            }
            byte[] payload = new byte[length];
            in.readFully(payload);
            return new Record(millis, packetId, payload);
        } catch (IOException truncated) {
            return null;
        }
    }

    /** Sanity bound on a record; a chunk packet is well under a megabyte. */
    private static final int MAX_PAYLOAD = 8 << 20;
}

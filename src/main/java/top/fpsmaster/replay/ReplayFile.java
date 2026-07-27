package top.fpsmaster.replay;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.UUID;
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
 *   header    magic "EDGEREPL" | int version | UTF minecraft version | long wall-clock start
 *             UTF recorder name | long uuid-high | long uuid-low | int dimension
 *   record    byte type | int millis-since-start | ...
 *     type 0  int packet id | int payload length | payload bytes
 *     type 1  double x,y,z | float yaw,pitch | byte flags
 *     type 2  byte slot | int length | serialised item stack
 * </pre>
 *
 * <p>Types 1 and 2 are the recording client's own player. A server never sends you your own spawn,
 * movement or equipment, so replaying the session with the recorder visible in it means capturing
 * those separately. Position is sampled every tick; equipment only when it changes, because writing
 * five item stacks twenty times a second would cost more than everything else in the file combined.
 *
 * <p>Gzipped: chunk payloads dominate the volume and compress by roughly an order of magnitude, and
 * the cost is paid on a writer thread rather than on the network thread.
 */
public final class ReplayFile {

    static final String MAGIC = "EDGEREPL";
    static final int VERSION = 4;

    private ReplayFile() {
    }

    public static DataOutputStream openForWrite(File file, String minecraftVersion, long startMillis,
            String recorderName, UUID recorderId, int dimension) throws IOException {
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
        // Playback needs to know who was recording: the avatar is built from this profile, and its
        // skin comes from the tab-list entry the snapshot already carries under the same UUID.
        out.writeUTF(recorderName);
        out.writeLong(recorderId.getMostSignificantBits());
        out.writeLong(recorderId.getLeastSignificantBits());
        out.writeInt(dimension);
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
        String minecraftVersion = in.readUTF();
        long startMillis = in.readLong();
        String recorderName = in.readUTF();
        UUID recorderId = new UUID(in.readLong(), in.readLong());
        return new Header(in, minecraftVersion, startMillis, recorderName, recorderId, in.readInt());
    }

    public static final class Header {
        public final DataInputStream stream;
        public final String minecraftVersion;
        public final long startMillis;
        public final String recorderName;
        public final UUID recorderId;
        public final int dimension;

        Header(DataInputStream stream, String minecraftVersion, long startMillis, String recorderName,
                UUID recorderId, int dimension) {
            this.stream = stream;
            this.minecraftVersion = minecraftVersion;
            this.startMillis = startMillis;
            this.recorderName = recorderName;
            this.recorderId = recorderId;
            this.dimension = dimension;
        }
    }

    public static final int TYPE_PACKET = 0;
    public static final int TYPE_LOCAL_PLAYER = 1;
    public static final int TYPE_LOCAL_EQUIPMENT = 2;

    public static final int FLAG_ON_GROUND = 1;
    public static final int FLAG_SNEAKING = 2;
    public static final int FLAG_SPRINTING = 4;
    public static final int FLAG_SWINGING = 8;

    /**
     * One recorded event.
     *
     * <p>Either a server packet ({@link #TYPE_PACKET}, {@code payload} holding the packet's own
     * serialised form without the id) or a sample of the recording player's own state
     * ({@link #TYPE_LOCAL_PLAYER}).
     */
    public static final class Record {
        public final int type;
        public final int millis;
        public final int packetId;
        public final int slot;
        public final byte[] payload;
        public final double x;
        public final double y;
        public final double z;
        public final float yaw;
        public final float pitch;
        public final int flags;

        public Record(int millis, int packetId, byte[] payload) {
            this.type = TYPE_PACKET;
            this.millis = millis;
            this.packetId = packetId;
            this.slot = -1;
            this.payload = payload;
            this.x = 0.0d;
            this.y = 0.0d;
            this.z = 0.0d;
            this.yaw = 0.0f;
            this.pitch = 0.0f;
            this.flags = 0;
        }

        public Record(int millis, double x, double y, double z, float yaw, float pitch, int flags) {
            this.type = TYPE_LOCAL_PLAYER;
            this.millis = millis;
            this.packetId = -1;
            this.slot = -1;
            this.payload = null;
            this.x = x;
            this.y = y;
            this.z = z;
            this.yaw = yaw;
            this.pitch = pitch;
            this.flags = flags;
        }

        /** Slot 0 is the held item, 1 to 4 the armour, matching the equipment packet's numbering. */
        private Record(int type, int millis, int slot, byte[] payload) {
            this.type = type;
            this.millis = millis;
            this.packetId = -1;
            this.slot = slot;
            this.payload = payload;
            this.x = 0.0d;
            this.y = 0.0d;
            this.z = 0.0d;
            this.yaw = 0.0f;
            this.pitch = 0.0f;
            this.flags = 0;
        }

        public static Record equipment(int millis, int slot, byte[] payload) {
            return new Record(TYPE_LOCAL_EQUIPMENT, millis, slot, payload);
        }
    }

    /** Bytes a record occupies on the wire, for the recorder's progress counter. */
    static long sizeOf(Record record) {
        switch (record.type) {
            case TYPE_PACKET:
                return 13L + record.payload.length;
            case TYPE_LOCAL_EQUIPMENT:
                return 10L + record.payload.length;
            default:
                return 38L;
        }
    }

    public static void write(DataOutputStream out, Record record) throws IOException {
        out.writeByte(record.type);
        out.writeInt(record.millis);
        if (record.type == TYPE_PACKET) {
            out.writeInt(record.packetId);
            out.writeInt(record.payload.length);
            out.write(record.payload);
        } else if (record.type == TYPE_LOCAL_EQUIPMENT) {
            out.writeByte(record.slot);
            out.writeInt(record.payload.length);
            out.write(record.payload);
        } else {
            out.writeDouble(record.x);
            out.writeDouble(record.y);
            out.writeDouble(record.z);
            out.writeFloat(record.yaw);
            out.writeFloat(record.pitch);
            out.writeByte(record.flags);
        }
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
            int type = in.readByte();
            int millis = in.readInt();
            if (type == TYPE_LOCAL_PLAYER) {
                return new Record(millis, in.readDouble(), in.readDouble(), in.readDouble(),
                        in.readFloat(), in.readFloat(), in.readByte());
            }
            if (type == TYPE_LOCAL_EQUIPMENT) {
                int slot = in.readByte();
                byte[] stack = readPayload(in);
                return stack == null ? null : Record.equipment(millis, slot, stack);
            }
            if (type != TYPE_PACKET) {
                return null;  // unknown type: the stream ended inside a record
            }
            int packetId = in.readInt();
            byte[] payload = readPayload(in);
            return payload == null ? null : new Record(millis, packetId, payload);
        } catch (IOException truncated) {
            return null;
        }
    }

    private static byte[] readPayload(DataInputStream in) throws IOException {
        int length = in.readInt();
        if (length < 0 || length > MAX_PAYLOAD) {
            return null;  // garbage length: the stream ended inside a record header
        }
        byte[] payload = new byte[length];
        in.readFully(payload);
        return payload;
    }

    /** Sanity bound on a record; a chunk packet is well under a megabyte. */
    private static final int MAX_PAYLOAD = 8 << 20;
}

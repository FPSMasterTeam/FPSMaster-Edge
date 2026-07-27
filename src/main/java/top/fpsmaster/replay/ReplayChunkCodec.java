package top.fpsmaster.replay;

import net.minecraft.network.EnumConnectionState;
import net.minecraft.network.EnumPacketDirection;
import net.minecraft.network.Packet;

import java.util.ArrayList;
import java.util.List;

/**
 * Splits chunk packets into their per-chunk payloads so identical chunks can be stored once.
 *
 * <p>Chunk data is over 99% of a recording, and a great deal of it arrives more than once: a chunk
 * that leaves view and comes back is re-sent byte for byte, and the same terrain arrives once inside
 * a bulk packet and again on its own. Gzip does not catch any of this — a single chunk is tens of
 * kilobytes and deflate only looks back 32 KiB, so two copies of the same chunk are almost never in
 * the window together. Measured on a recording of generated terrain, referencing repeats instead of
 * storing them takes the file from 2504 KiB to 1569 KiB.
 *
 * <p>The split has to understand the two chunk packet layouts, and that knowledge is only correct for
 * as long as the protocol is. Every method here therefore returns null rather than guessing when the
 * bytes do not add up, and the caller stores the packet whole — a recording that is larger than it
 * needed to be is a far better failure than one holding subtly wrong terrain.
 */
final class ReplayChunkCodec {

    private static int singleId = -1;
    private static int bulkId = -1;

    private ReplayChunkCodec() {
    }

    /** True for the two packets whose payload is mostly chunk blobs. */
    static boolean isChunkPacket(int packetId) {
        resolveIds();
        return packetId == singleId || packetId == bulkId;
    }

    private static synchronized void resolveIds() {
        if (singleId >= 0) {
            return;
        }
        // Read from the protocol table rather than hard-coded, so a mapping change is a failure to
        // find the packet rather than a silently mis-parsed payload.
        for (int id = 0; id < 0x60; id++) {
            Packet<?> packet;
            try {
                packet = EnumConnectionState.PLAY.getPacket(EnumPacketDirection.CLIENTBOUND, id);
            } catch (Exception unmapped) {
                continue;
            }
            if (packet == null) {
                continue;
            }
            String name = packet.getClass().getSimpleName();
            if ("S21PacketChunkData".equals(name)) {
                singleId = id;
            } else if ("S26PacketMapChunkBulk".equals(name)) {
                bulkId = id;
            }
        }
        if (singleId < 0) {
            singleId = Integer.MIN_VALUE;
        }
        if (bulkId < 0) {
            bulkId = Integer.MIN_VALUE;
        }
    }

    /**
     * The chunk blobs at the end of a payload, or null if the payload does not parse as expected.
     *
     * <p>Both layouts put every blob last, one after another, so the bytes before them are a header
     * that can be stored verbatim and the blobs can be referenced individually.
     */
    static List<byte[]> split(int packetId, byte[] payload) {
        resolveIds();
        if (packetId == singleId) {
            return splitSingle(payload);
        }
        if (packetId == bulkId) {
            return splitBulk(payload);
        }
        return null;
    }

    /** Layout: chunk x, chunk z, full-chunk flag, section mask, then a length-prefixed blob. */
    private static List<byte[]> splitSingle(byte[] payload) {
        if (payload.length < 12) {
            return null;
        }
        int[] cursor = {11};
        int length = readVarInt(payload, cursor);
        if (length <= 0 || cursor[0] + length != payload.length) {
            return null;
        }
        List<byte[]> blobs = new ArrayList<byte[]>(1);
        blobs.add(slice(payload, cursor[0], length));
        return blobs;
    }

    /** Layout: sky-light flag, count, then count headers of (x, z, mask), then the blobs. */
    private static List<byte[]> splitBulk(byte[] payload) {
        if (payload.length < 2) {
            return null;
        }
        boolean skylight = payload[0] != 0;
        int[] cursor = {1};
        int count = readVarInt(payload, cursor);
        if (count <= 0 || count > 1024) {
            return null;
        }
        long headerEnd = (long) cursor[0] + (long) count * 10L;
        if (headerEnd > payload.length) {
            return null;
        }
        int[] sizes = new int[count];
        long total = 0L;
        for (int index = 0; index < count; index++) {
            int base = cursor[0] + index * 10;
            int mask = ((payload[base + 8] & 0xFF) << 8) | (payload[base + 9] & 0xFF);
            int sections = Integer.bitCount(mask);
            // Blocks, block light, sky light when the dimension has one, then the biome array.
            sizes[index] = sections * 8192 + sections * 2048 + (skylight ? sections * 2048 : 0) + 256;
            total += sizes[index];
        }
        if (headerEnd + total != payload.length) {
            return null;
        }
        List<byte[]> blobs = new ArrayList<byte[]>(count);
        int offset = (int) headerEnd;
        for (int index = 0; index < count; index++) {
            blobs.add(slice(payload, offset, sizes[index]));
            offset += sizes[index];
        }
        return blobs;
    }

    /** Rebuilds the original payload. Must be byte-identical or the packet will not decode. */
    static byte[] join(byte[] header, List<byte[]> blobs) {
        int length = header.length;
        for (int index = 0; index < blobs.size(); index++) {
            length += blobs.get(index).length;
        }
        byte[] payload = new byte[length];
        System.arraycopy(header, 0, payload, 0, header.length);
        int offset = header.length;
        for (int index = 0; index < blobs.size(); index++) {
            byte[] blob = blobs.get(index);
            System.arraycopy(blob, 0, payload, offset, blob.length);
            offset += blob.length;
        }
        return payload;
    }

    private static int readVarInt(byte[] data, int[] cursor) {
        int value = 0;
        for (int shift = 0; shift < 5; shift++) {
            if (cursor[0] >= data.length) {
                return -1;
            }
            int part = data[cursor[0]++] & 0xFF;
            value |= (part & 0x7F) << (shift * 7);
            if ((part & 0x80) == 0) {
                return value;
            }
        }
        return -1;
    }

    private static byte[] slice(byte[] data, int offset, int length) {
        byte[] slice = new byte[length];
        System.arraycopy(data, offset, slice, 0, length);
        return slice;
    }
}

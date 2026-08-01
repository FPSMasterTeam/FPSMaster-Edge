package top.fpsmaster.replay;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.Deflater;
import java.util.zip.GZIPOutputStream;

/**
 * Measures what a candidate encoding would actually save, after compression.
 *
 * <p>Raw byte counts are misleading here by an order of magnitude: a recording of a superflat world
 * carries 7.5 MB of chunk data and lands on disk at 98 KB, because deflate erases exactly the kind of
 * repetition that a hand-written dictionary would also target. Any scheme judged on raw bytes will
 * look like a win and deliver nothing. So each candidate is encoded in full and gzipped, and the
 * number reported is the file that would result.
 *
 * <pre>
 *   java -cp ... top.fpsmaster.replay.ReplaySizeLab &lt;file.edgereplay&gt;
 * </pre>
 */
public final class ReplaySizeLab {

    private ReplaySizeLab() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            System.err.println("usage: ReplaySizeLab <file.edgereplay>");
            System.exit(2);
        }
        File file = new File(args[0]);
        ReplayFile.Header header = ReplayFile.openForRead(file);
        List<ReplayFile.Record> records = new ArrayList<ReplayFile.Record>();
        ReplayFile.Record record;
        while ((record = ReplayFile.read(header)) != null) {
            records.add(record);
        }
        header.stream.close();

        long onDisk = file.length();
        System.out.printf("%s%n  %d records, %.1f KiB on disk%n%n", file.getName(),
                Integer.valueOf(records.size()), Double.valueOf(onDisk / 1024.0d));

        report("baseline", onDisk, encodeBaseline(records, Deflater.DEFAULT_COMPRESSION));
        report("deflate level 9", onDisk, encodeBaseline(records, Deflater.BEST_COMPRESSION));
        report("varint framing", onDisk, encodeVarint(records, Deflater.DEFAULT_COMPRESSION));
        report("exact-payload dedup", onDisk, encodeDedup(records, Deflater.DEFAULT_COMPRESSION));
        report("varint + dedup", onDisk, encodeVarintDedup(records, Deflater.DEFAULT_COMPRESSION));
        report("varint + dedup + level 9", onDisk, encodeVarintDedup(records, Deflater.BEST_COMPRESSION));
        for (int capMiB : new int[] {8, 16, 32, 64, Integer.MAX_VALUE / (1 << 20)}) {
            long start = System.nanoTime();
            long size = encodeChunkDictionary(records, Deflater.DEFAULT_COMPRESSION, capMiB * (1L << 20));
            report("chunk dict cap " + (capMiB > 4096 ? "none" : capMiB + "M"), onDisk, size,
                    (System.nanoTime() - start) / 1_000_000L);
        }
        long start = System.nanoTime();
        long size = encodeChunkDictionary(records, Deflater.BEST_COMPRESSION, 64L << 20);
        report("chunk dict cap 64M, lvl 9", onDisk, size, (System.nanoTime() - start) / 1_000_000L);

        reportChunkStructure(records);
        verifyDictionaryAgreement(records);
    }


    /**
     * Checks that the writing and reading dictionaries assign the same index to the same chunk.
     *
     * <p>This is the failure that would otherwise go unnoticed. If the two sides ever disagreed
     * about which entry a reference points at, a chunk would be replaced by a different chunk of the
     * same size - the packet would still deserialise, the file would still round-trip, and the only
     * symptom would be terrain that is quietly wrong. So both dictionaries are run over the same
     * blobs and every reference is resolved back and compared against the bytes it stood for.
     */
    private static void verifyDictionaryAgreement(List<ReplayFile.Record> records) {
        ReplayChunkDictionary.Write writer =
                new ReplayChunkDictionary.Write((long) ReplayChunkDictionary.DEFAULT_CAP_MIB << 20);
        ReplayChunkDictionary.Read reader =
                new ReplayChunkDictionary.Read((long) ReplayChunkDictionary.DEFAULT_CAP_MIB << 20);
        int references = 0;
        int inline = 0;
        int mismatches = 0;

        for (ReplayFile.Record record : records) {
            if (record.type != ReplayFile.TYPE_PACKET || !ReplayChunkCodec.isChunkPacket(record.packetId)) {
                continue;
            }
            List<byte[]> blobs = ReplayChunkCodec.split(record.packetId, record.payload);
            if (blobs == null) {
                continue;
            }
            for (byte[] blob : blobs) {
                int reference = writer.reference(blob);
                if (reference == 0) {
                    reader.offer(blob);
                    inline++;
                    continue;
                }
                references++;
                byte[] resolved = reader.get(reference);
                if (resolved == null || !java.util.Arrays.equals(resolved, blob)) {
                    mismatches++;
                }
            }
        }
        System.out.printf("%n  dictionary agreement: %d stored, %d referenced, %d mismatched%n",
                Integer.valueOf(inline), Integer.valueOf(references), Integer.valueOf(mismatches));
        if (mismatches > 0) {
            System.err.println("FAIL: a reference resolved to different bytes than it stood for");
            System.exit(1);
        }
    }

    /** Packet ids resolved from the protocol table rather than written down and hoped for. */
    private static int idOf(String simpleName) {
        for (int id = 0; id < 0x60; id++) {
            try {
                net.minecraft.network.Packet<?> packet = net.minecraft.network.EnumConnectionState.PLAY
                        .getPacket(net.minecraft.network.EnumPacketDirection.CLIENTBOUND, id);
                if (packet != null && packet.getClass().getSimpleName().equals(simpleName)) {
                    return id;
                }
            } catch (Exception ignored) {
                // Gaps in the table are expected; keep scanning.
            }
        }
        throw new IllegalStateException("no packet id for " + simpleName);
    }

    /**
     * Dedups whole chunks by content, across both the single and bulk chunk packets.
     *
     * <p>This is the one repetition worth chasing. A chunk that leaves view and comes back is sent
     * again byte for byte, and the same terrain arrives once as a bulk packet and once on its own —
     * so the two packet types have to share a dictionary or most of the repetition is invisible. A
     * chunk is tens of kilobytes, far wider than deflate's 32 KiB window, so none of this is
     * something compression already handles.
     */
    private static long encodeChunkDictionary(List<ReplayFile.Record> records, int level, long capBytes)
            throws IOException {
        int singleId = idOf("S21PacketChunkData");
        int bulkId = idOf("S26PacketMapChunkBulk");
        Sink sink = new Sink(level);
        Map<Blob, Integer> dictionary = new HashMap<Blob, Integer>();
        long dictionaryBytes = 0L;
        int next = 0;
        int previousMillis = 0;

        for (ReplayFile.Record record : records) {
            List<byte[]> blobs = record.type == ReplayFile.TYPE_PACKET
                    ? (record.packetId == singleId ? splitSingle(record.payload)
                            : record.packetId == bulkId ? splitBulk(record.payload) : null)
                    : null;
            if (blobs == null) {
                sink.out.writeByte(record.type);
                writeVarInt(sink.out, record.millis - previousMillis);
                previousMillis = record.millis;
                writeBody(sink.out, record);
                continue;
            }
            sink.out.writeByte(record.type);
            writeVarInt(sink.out, record.millis - previousMillis);
            previousMillis = record.millis;
            writeVarInt(sink.out, record.packetId);
            // Header of the original packet, minus the blobs: coordinates and masks stay verbatim.
            int headerLength = record.payload.length;
            for (byte[] blob : blobs) {
                headerLength -= blob.length;
            }
            writeVarInt(sink.out, headerLength);
            sink.out.write(record.payload, 0, headerLength);
            for (byte[] blob : blobs) {
                Blob key = new Blob(blob);
                Integer reference = dictionary.get(key);
                if (reference != null) {
                    writeVarInt(sink.out, reference.intValue() + 1);
                    continue;
                }
                // Past the cap, chunks are still written - just not remembered. Both sides stop
                // adding at the same byte count, so their dictionaries stay identical without
                // having to agree on an eviction policy.
                if (dictionaryBytes + blob.length <= capBytes) {
                    dictionary.put(key, Integer.valueOf(next++));
                    dictionaryBytes += blob.length;
                }
                writeVarInt(sink.out, 0);
                sink.out.write(blob);
            }
        }
        long total = sink.finish();
        if (capBytes > (1L << 40)) {
            System.out.printf("      (dictionary held %d chunks, %.1f MiB)%n",
                    Integer.valueOf(dictionary.size()), Double.valueOf(dictionaryBytes / 1048576.0d));
        }
        return total;
    }

    /** The single blob of an S21 payload, or null if this crude parse cannot follow it. */
    private static List<byte[]> splitSingle(byte[] payload) {
        if (payload.length < 12) {
            return null;
        }
        int offset = 11;
        int length = 0;
        for (int shift = 0; shift < 5; shift++) {
            int part = payload[offset++] & 0xFF;
            length |= (part & 0x7F) << (shift * 7);
            if ((part & 0x80) == 0) {
                break;
            }
        }
        if (length <= 0 || offset + length != payload.length) {
            return null;
        }
        List<byte[]> blobs = new ArrayList<byte[]>(1);
        byte[] blob = new byte[length];
        System.arraycopy(payload, offset, blob, 0, length);
        blobs.add(blob);
        return blobs;
    }

    /** Every chunk inside a bulk packet, so they share the dictionary with the single ones. */
    private static List<byte[]> splitBulk(byte[] payload) {
        if (payload.length < 2) {
            return null;
        }
        boolean skylight = payload[0] != 0;
        int offset = 1;
        int count = 0;
        for (int shift = 0; shift < 5; shift++) {
            int part = payload[offset++] & 0xFF;
            count |= (part & 0x7F) << (shift * 7);
            if ((part & 0x80) == 0) {
                break;
            }
        }
        if (count <= 0 || offset + count * 10 > payload.length) {
            return null;
        }
        int[] sizes = new int[count];
        long total = 0L;
        for (int index = 0; index < count; index++) {
            int mask = ((payload[offset + index * 10 + 8] & 0xFF) << 8)
                    | (payload[offset + index * 10 + 9] & 0xFF);
            int sections = Integer.bitCount(mask);
            sizes[index] = sections * 8192 + sections * 2048 + (skylight ? sections * 2048 : 0) + 256;
            total += sizes[index];
        }
        int cursor = offset + count * 10;
        if (cursor + total != payload.length) {
            return null;
        }
        List<byte[]> blobs = new ArrayList<byte[]>(count);
        for (int index = 0; index < count; index++) {
            byte[] blob = new byte[sizes[index]];
            System.arraycopy(payload, cursor, blob, 0, sizes[index]);
            cursor += sizes[index];
            blobs.add(blob);
        }
        return blobs;
    }

    private static void report(String name, long baseline, long size) {
        report(name, baseline, size, -1L);
    }

    private static void report(String name, long baseline, long size, long millis) {
        System.out.printf("  %-26s %8.1f KiB  %+6.1f%%%s%n", name, Double.valueOf(size / 1024.0d),
                Double.valueOf(100.0d * (size - baseline) / baseline),
                millis < 0 ? "" : String.format("   %5d ms encode", Long.valueOf(millis)));
    }

    /** Dictionary entry: the bytes, so a hash collision cannot silently swap one chunk for another. */
    private static final class Blob {
        final byte[] data;
        final int hash;

        Blob(byte[] data) {
            this.data = data;
            this.hash = java.util.Arrays.hashCode(data);
        }

        @Override
        public int hashCode() {
            return hash;
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof Blob && java.util.Arrays.equals(data, ((Blob) other).data);
        }
    }

    /** Writes the current format, so the lab's own framing overhead cancels out of comparisons. */
    private static long encodeBaseline(List<ReplayFile.Record> records, int level) throws IOException {
        Sink sink = new Sink(level);
        for (ReplayFile.Record record : records) {
            ReplayFile.write(sink.out, record);
        }
        return sink.finish();
    }

    /**
     * Same records, but millis as a delta and every count as a varint.
     *
     * <p>Framing is 13 bytes a record today and the stream is mostly small packets, so this removes
     * roughly 9 bytes per record — before compression. Whether any of it survives deflate is the
     * question, since fixed-width fields full of zero bytes are exactly what it is good at.
     */
    private static long encodeVarint(List<ReplayFile.Record> records, int level) throws IOException {
        Sink sink = new Sink(level);
        int previousMillis = 0;
        for (ReplayFile.Record record : records) {
            sink.out.writeByte(record.type);
            writeVarInt(sink.out, record.millis - previousMillis);
            previousMillis = record.millis;
            writeBody(sink.out, record);
        }
        return sink.finish();
    }

    /** Replaces a payload already seen anywhere in the stream with a reference to it. */
    private static long encodeDedup(List<ReplayFile.Record> records, int level) throws IOException {
        Sink sink = new Sink(level);
        Map<String, Integer> seen = new HashMap<String, Integer>();
        int next = 0;
        for (ReplayFile.Record record : records) {
            Integer reference = record.payload == null ? null : seen.get(key(record));
            if (reference != null) {
                sink.out.writeByte(9);
                sink.out.writeInt(record.millis);
                sink.out.writeInt(reference.intValue());
                continue;
            }
            if (record.payload != null) {
                seen.put(key(record), Integer.valueOf(next++));
            }
            ReplayFile.write(sink.out, record);
        }
        return sink.finish();
    }

    private static long encodeVarintDedup(List<ReplayFile.Record> records, int level) throws IOException {
        Sink sink = new Sink(level);
        Map<String, Integer> seen = new HashMap<String, Integer>();
        int next = 0;
        int previousMillis = 0;
        for (ReplayFile.Record record : records) {
            Integer reference = record.payload == null ? null : seen.get(key(record));
            if (reference != null) {
                sink.out.writeByte(9);
                writeVarInt(sink.out, record.millis - previousMillis);
                previousMillis = record.millis;
                writeVarInt(sink.out, reference.intValue());
                continue;
            }
            if (record.payload != null) {
                seen.put(key(record), Integer.valueOf(next++));
            }
            sink.out.writeByte(record.type);
            writeVarInt(sink.out, record.millis - previousMillis);
            previousMillis = record.millis;
            writeBody(sink.out, record);
        }
        return sink.finish();
    }

    private static void writeBody(DataOutputStream out, ReplayFile.Record record) throws IOException {
        if (record.type == ReplayFile.TYPE_PACKET) {
            writeVarInt(out, record.packetId);
            writeVarInt(out, record.payload.length);
            out.write(record.payload);
        } else if (record.type == ReplayFile.TYPE_LOCAL_EQUIPMENT) {
            out.writeByte(record.slot);
            writeVarInt(out, record.payload.length);
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

    private static String key(ReplayFile.Record record) {
        return record.type + ":" + record.packetId + ":" + record.slot + ":"
                + record.payload.length + ":" + java.util.Arrays.hashCode(record.payload);
    }

    private static void writeVarInt(DataOutputStream out, int value) throws IOException {
        int remaining = value;
        while ((remaining & 0xFFFFFF80) != 0) {
            out.writeByte(remaining & 0x7F | 0x80);
            remaining >>>= 7;
        }
        out.writeByte(remaining);
    }

    /**
     * How much of the chunk data is the same 16-block cube or light array appearing again.
     *
     * <p>Chunk payloads are the bulk of any recording, and a world is full of sections that are
     * identical to one another — solid stone underground, fully lit air above the surface. Deflate
     * only sees repeats inside a 32 KiB window, and a single section is 12 KiB, so it catches almost
     * none of this. Reported as a statistic before deciding whether the format should carry it.
     */
    private static void reportChunkStructure(List<ReplayFile.Record> records) {
        Map<Integer, Integer> blockSections = new HashMap<Integer, Integer>();
        Map<Integer, Integer> lightSections = new HashMap<Integer, Integer>();
        long blockBytes = 0L;
        long lightBytes = 0L;
        int chunks = 0;

        for (ReplayFile.Record record : records) {
            if (record.type != ReplayFile.TYPE_PACKET || record.payload.length < 11) {
                continue;
            }
            byte[] payload = record.payload;
            // chunkX, chunkZ, fullChunk flag, section mask, then a varint-prefixed blob.
            int mask = ((payload[9] & 0xFF) << 8) | (payload[10] & 0xFF);
            int sections = Integer.bitCount(mask);
            if (sections == 0) {
                continue;
            }
            int offset = 11;
            int length = 0;
            for (int shift = 0; shift < 5; shift++) {
                if (offset >= payload.length) {
                    break;
                }
                int part = payload[offset++] & 0xFF;
                length |= (part & 0x7F) << (shift * 7);
                if ((part & 0x80) == 0) {
                    break;
                }
            }
            if (length <= 0 || offset + length > payload.length) {
                continue;  // not a chunk packet, or one this crude parse cannot follow
            }
            // Blocks, then block light, then optionally sky light, then optionally biomes.
            long expectedWithSky = sections * 12288L + 256L;
            long expectedNoSky = sections * 10240L + 256L;
            boolean hasSky = length == expectedWithSky;
            if (!hasSky && length != expectedNoSky) {
                continue;
            }
            chunks++;
            int cursor = offset;
            for (int section = 0; section < sections; section++) {
                blockSections.put(Integer.valueOf(hash(payload, cursor, 8192)),
                        Integer.valueOf(1));
                blockBytes += 8192L;
                cursor += 8192;
            }
            int lightArrays = hasSky ? sections * 2 : sections;
            for (int array = 0; array < lightArrays; array++) {
                lightSections.put(Integer.valueOf(hash(payload, cursor, 2048)), Integer.valueOf(1));
                lightBytes += 2048L;
                cursor += 2048;
            }
        }

        if (chunks == 0) {
            return;
        }
        long uniqueBlockBytes = blockSections.size() * 8192L;
        long uniqueLightBytes = lightSections.size() * 2048L;
        System.out.printf("%n  chunk structure across %d chunk packets:%n", Integer.valueOf(chunks));
        System.out.printf("    block sections   %8.1f KiB -> %8.1f KiB unique (%.0f%% repeated)%n",
                Double.valueOf(blockBytes / 1024.0d), Double.valueOf(uniqueBlockBytes / 1024.0d),
                Double.valueOf(100.0d * (blockBytes - uniqueBlockBytes) / Math.max(blockBytes, 1L)));
        System.out.printf("    light arrays     %8.1f KiB -> %8.1f KiB unique (%.0f%% repeated)%n",
                Double.valueOf(lightBytes / 1024.0d), Double.valueOf(uniqueLightBytes / 1024.0d),
                Double.valueOf(100.0d * (lightBytes - uniqueLightBytes) / Math.max(lightBytes, 1L)));
    }

    private static int hash(byte[] data, int offset, int length) {
        int hash = 1;
        for (int index = offset; index < offset + length && index < data.length; index++) {
            hash = 31 * hash + data[index];
        }
        return hash;
    }

    private static final class Sink {
        final ByteArrayOutputStream bytes = new ByteArrayOutputStream(1 << 20);
        final DataOutputStream out;
        private final GZIPOutputStream gzip;

        Sink(final int level) throws IOException {
            this.gzip = new GZIPOutputStream(bytes, 1 << 16) {
                {
                    def.setLevel(level);
                }
            };
            this.out = new DataOutputStream(gzip);
        }

        long finish() throws IOException {
            out.flush();
            gzip.finish();
            return bytes.size();
        }
    }
}

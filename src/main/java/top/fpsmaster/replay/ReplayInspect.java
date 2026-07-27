package top.fpsmaster.replay;

import io.netty.buffer.Unpooled;
import net.minecraft.network.EnumConnectionState;
import net.minecraft.network.EnumPacketDirection;
import net.minecraft.network.Packet;
import net.minecraft.network.PacketBuffer;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Reads a recording back and checks every packet deserialises.
 *
 * <p>A recording that writes without error but cannot be read is worthless, and the only way to know
 * is to round-trip it: look up the packet class by id, deserialise the stored payload, and confirm
 * the whole payload was consumed. A packet that leaves bytes behind read differently than it was
 * written, which is exactly the kind of fault that would otherwise surface as a corrupted replay
 * long after the session it came from is gone.
 *
 * <p>Runs without a client — no LaunchWrapper, no GL — because packet classes are plain bytecode.
 *
 * <pre>
 *   java -cp ... top.fpsmaster.replay.ReplayInspect &lt;file.edgereplay&gt;
 * </pre>
 */
public final class ReplayInspect {

    private ReplayInspect() {
    }

    /**
     * Reads the wire form of an item stack without touching the item registry.
     *
     * <p>This tool runs without a game, so the registry is empty and anything that resolves an id to
     * an Item throws. The numbers are what matter here anyway - a wrong item shows up as a wrong id.
     */
    private static String describe(byte[] payload) {
        try {
            PacketBuffer buffer = new PacketBuffer(Unpooled.wrappedBuffer(payload));
            int id = buffer.readShort();
            if (id < 0) {
                return "empty";
            }
            int count = buffer.readByte();
            int damage = buffer.readShort();
            byte nbt = buffer.readByte();
            return count + "x id " + id + " dmg " + damage + (nbt == 0 ? "" : " +nbt");
        } catch (Exception failure) {
            return "undecodable: " + failure;
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            System.err.println("usage: ReplayInspect <file.edgereplay>");
            System.exit(2);
        }
        File file = new File(args[0]);
        ReplayFile.Header header = ReplayFile.openForRead(file);

        Map<String, Integer> byType = new HashMap<String, Integer>();
        Map<String, long[]> bytesByType = new HashMap<String, long[]>();
        Map<String, Long> firstSeen = new HashMap<String, Long>();
        long duplicateBytes = 0L;
        int duplicateRecords = 0;
        long chunkBytes = 0L;
        int records = 0;
        int undecodable = 0;
        int trailingBytes = 0;
        int lastMillis = 0;
        long payloadBytes = 0L;

        int localSamples = 0;
        int equipmentChanges = 0;
        int containerSlots = 0;
        StringBuilder containerLog = new StringBuilder();
        ReplayFile.Record record;
        while ((record = ReplayFile.read(header)) != null) {
            records++;
            lastMillis = record.millis;
            if (record.type == ReplayFile.TYPE_LOCAL_PLAYER) {
                localSamples++;
                continue;
            }
            if (record.type == ReplayFile.TYPE_CONTAINER_SLOT) {
                containerSlots++;
                payloadBytes += record.payload.length;
                if (containerLog.length() < 600) {
                    containerLog.append(String.format("    %6dms  window %d slot %-3d %s%n",
                            Integer.valueOf(record.millis), Integer.valueOf(record.windowId),
                            Integer.valueOf(record.slot), describe(record.payload)));
                }
                continue;
            }
            if (record.type == ReplayFile.TYPE_LOCAL_EQUIPMENT) {
                equipmentChanges++;
                payloadBytes += record.payload.length;
                continue;
            }
            payloadBytes += record.payload.length;
            // Repeats among everything the chunk dictionary does not already handle, which is what
            // would decide whether a second dictionary is worth adding. Chunk packets are counted
            // separately: their repeats are removed before they reach the file, so including them
            // here would advertise a saving that has already been taken.
            if (ReplayChunkCodec.isChunkPacket(record.packetId)) {
                chunkBytes += record.payload.length;
            } else {
                String fingerprint = record.packetId + ":" + java.util.Arrays.hashCode(record.payload)
                        + ":" + record.payload.length;
                if (firstSeen.containsKey(fingerprint)) {
                    duplicateBytes += record.payload.length;
                    duplicateRecords++;
                } else {
                    firstSeen.put(fingerprint, Long.valueOf(0L));
                }
            }

            Packet<?> packet;
            try {
                packet = EnumConnectionState.PLAY.getPacket(EnumPacketDirection.CLIENTBOUND, record.packetId);
            } catch (Exception failure) {
                packet = null;
            }
            if (packet == null) {
                undecodable++;
                continue;
            }
            PacketBuffer buffer = new PacketBuffer(Unpooled.wrappedBuffer(record.payload));
            try {
                packet.readPacketData(buffer);
                if (buffer.readableBytes() != 0) {
                    trailingBytes++;
                }
            } catch (Exception failure) {
                undecodable++;
                continue;
            }
            String name = packet.getClass().getSimpleName();
            Integer count = byType.get(name);
            byType.put(name, Integer.valueOf(count == null ? 1 : count.intValue() + 1));
            long[] bytes = bytesByType.get(name);
            if (bytes == null) {
                bytes = new long[1];
                bytesByType.put(name, bytes);
            }
            bytes[0] += record.payload.length;
        }
        header.stream.close();

        System.out.printf("%s%n", file.getName());
        System.out.printf("  minecraft      %s%n", header.minecraftVersion);
        System.out.printf("  recorder       %s (%s), dimension %d%n",
                header.recorderName, header.recorderId, Integer.valueOf(header.dimension));
        System.out.printf("  duration       %.1fs%n", lastMillis / 1000.0d);
        System.out.printf("  records        %d (%d local-player samples, %d equipment changes,"
                        + " %d container slots)%n",
                records, localSamples, equipmentChanges, containerSlots);
        System.out.printf("  payload        %.1f KiB (%.0f KiB/s)%n",
                payloadBytes / 1024.0d, payloadBytes / 1024.0d / Math.max(lastMillis / 1000.0d, 1e-9));
        System.out.printf("  undecodable    %d%n", undecodable);
        System.out.printf("  trailing bytes %d%n", trailingBytes);

        System.out.printf("  file on disk   %.1f KiB (%.1fx compression)%n",
                file.length() / 1024.0d, payloadBytes / (double) Math.max(file.length(), 1L));
        System.out.printf("  chunk data     %.1f KiB (%.0f%% of payload, deduplicated in the file)%n",
                chunkBytes / 1024.0d, 100.0d * chunkBytes / Math.max(payloadBytes, 1L));
        System.out.printf("  other repeats  %.1f KiB in %d records (%.0f%% of payload)%n",
                duplicateBytes / 1024.0d, duplicateRecords,
                100.0d * duplicateBytes / Math.max(payloadBytes, 1L));

        if (containerLog.length() > 0) {
            System.out.println("  container slots recorded:");
            System.out.print(containerLog);
        }

        System.out.println("  heaviest packets:");
        bytesByType.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue()[0], a.getValue()[0]))
                .limit(12)
                .forEach(e -> System.out.printf("    %-38s %6d x %8.1f KiB%n", e.getKey(),
                        byType.get(e.getKey()), e.getValue()[0] / 1024.0d));

        if (undecodable > 0 || trailingBytes > 0) {
            System.err.println("\nFAIL: the recording does not round-trip cleanly");
            System.exit(1);
        }
        System.out.println("\nOK: every packet round-trips");
    }
}

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

    /** Uuid and name of every ADD_PLAYER entry, read by hand so no registry is needed. */
    private static java.util.List<String[]> tabEntriesOf(byte[] payload) {
        java.util.List<String[]> entries = new java.util.ArrayList<String[]>();
        try {
            PacketBuffer buffer = new PacketBuffer(Unpooled.wrappedBuffer(payload));
            int action = buffer.readVarIntFromBuffer();
            int count = buffer.readVarIntFromBuffer();
            if (action != 0) {
                return entries;
            }
            for (int index = 0; index < count; index++) {
                String uuid = buffer.readUuid().toString();
                String name = buffer.readStringFromBuffer(16);
                entries.add(new String[]{uuid, name});
                int properties = buffer.readVarIntFromBuffer();
                for (int property = 0; property < properties; property++) {
                    buffer.readStringFromBuffer(32767);
                    buffer.readStringFromBuffer(32767);
                    if (buffer.readBoolean()) {
                        buffer.readStringFromBuffer(32767);
                    }
                }
                buffer.readVarIntFromBuffer();
                buffer.readVarIntFromBuffer();
                if (buffer.readBoolean()) {
                    buffer.readStringFromBuffer(32767);
                }
            }
        } catch (Exception truncated) {
            // Partial is still informative.
        }
        return entries;
    }

    private static String spawnUuidOf(byte[] payload) {
        try {
            PacketBuffer buffer = new PacketBuffer(Unpooled.wrappedBuffer(payload));
            buffer.readVarIntFromBuffer();
            return buffer.readUuid().toString();
        } catch (Exception failure) {
            return "?";
        }
    }

    private static int spawnEntityIdOf(byte[] payload) {
        try {
            return new PacketBuffer(Unpooled.wrappedBuffer(payload)).readVarIntFromBuffer();
        } catch (Exception failure) {
            return -1;
        }
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
        java.util.Set<String> tabEntries = new java.util.HashSet<String>();
        java.util.Set<Integer> spawnedPlayers = new java.util.HashSet<Integer>();
        int tabPackets = 0;
        java.util.Map<String, Integer> namedAt = new java.util.HashMap<String, Integer>();
        java.util.List<int[]> spawnTimes = new java.util.ArrayList<int[]>();
        java.util.List<String> spawnUuids = new java.util.ArrayList<String>();
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
            } catch (Throwable failure) {
                // Throwable, not Exception: some packets resolve ids through a registry, and with no
                // game running that surfaces as ExceptionInInitializerError - an Error, which was
                // escaping and taking the whole report with it.
                undecodable++;
                continue;
            }
            String name = packet.getClass().getSimpleName();
            if ("S38PacketPlayerListItem".equals(name)) {
                tabPackets++;
                for (String[] entry : tabEntriesOf(record.payload)) {
                    tabEntries.add(entry[1]);
                    if (!namedAt.containsKey(entry[0])) {
                        namedAt.put(entry[0], Integer.valueOf(record.millis));
                    }
                }
            } else if ("S0CPacketSpawnPlayer".equals(name)) {
                spawnedPlayers.add(Integer.valueOf(spawnEntityIdOf(record.payload)));
                spawnUuids.add(spawnUuidOf(record.payload));
                spawnTimes.add(new int[]{record.millis});
            }
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

        System.out.printf("  other players  %d spawn packets for %d distinct entities,"
                        + " %d tab-list packets naming %d players%n",
                Integer.valueOf(byType.containsKey("S0CPacketSpawnPlayer")
                        ? byType.get("S0CPacketSpawnPlayer").intValue() : 0),
                Integer.valueOf(spawnedPlayers.size()),
                Integer.valueOf(tabPackets), Integer.valueOf(tabEntries.size()));
        if (!tabEntries.isEmpty()) {
            System.out.printf("    named: %s%n", String.join(", ",
                    tabEntries.size() > 12
                            ? new java.util.ArrayList<String>(tabEntries).subList(0, 12)
                            : new java.util.ArrayList<String>(tabEntries)));
        }
        if (!spawnedPlayers.isEmpty() && tabEntries.isEmpty()) {
            System.out.println("    WARNING: players are spawned but never named. handleSpawnPlayer"
                    + " looks the profile up in the tab list without checking, so none of them"
                    + " will appear during playback.");
        }

        int neverNamed = 0;
        int namedLate = 0;
        for (int index = 0; index < spawnUuids.size(); index++) {
            Integer when = namedAt.get(spawnUuids.get(index));
            if (when == null) {
                neverNamed++;
            } else if (when.intValue() > spawnTimes.get(index)[0]) {
                namedLate++;
            }
        }
        if (neverNamed > 0 || namedLate > 0) {
            System.out.printf("    %d spawned without ever being named, %d named only after they"
                            + " spawned - handleSpawnPlayer needs the name first, so these cannot"
                            + " appear during playback%n",
                    Integer.valueOf(neverNamed), Integer.valueOf(namedLate));
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

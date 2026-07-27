package top.fpsmaster.replay;

import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Remembers chunk blobs already written, so a repeat costs a reference instead of a copy.
 *
 * <p>The two sides hold different things. Writing only needs to recognise a blob, so it keeps a
 * digest — a few dozen bytes per chunk instead of tens of kilobytes, which matters because recording
 * happens during a match. Reading needs to hand the bytes back, so it keeps them.
 *
 * <p>Both sides must agree on which blobs are in the dictionary and in what order, or a reference
 * resolves to the wrong terrain. Rather than share an eviction policy, they share a rule that needs
 * no coordination: entries are added in the order blobs are written, and once the byte cap is
 * reached nothing more is added. Later repeats are simply stored again. The cap is written into the
 * file, so changing it later cannot silently misread an older recording.
 */
final class ReplayChunkDictionary {

    /**
     * Chosen from measurement, not intuition. On a recording of generated terrain the file shrinks
     * 3% at 8 MiB, 9% at 16, 27% at 32 and 37% at 64, and no further beyond that — the whole working
     * set fits. Reading holds this much; writing holds digests and costs almost nothing.
     */
    static final int DEFAULT_CAP_MIB = 64;

    private ReplayChunkDictionary() {
    }

    /** Recognises repeats by digest. A 128-bit digest makes a false match not worth reasoning about. */
    static final class Write {
        private final Map<Key, Integer> known = new HashMap<Key, Integer>();
        private final long capBytes;
        private long bytes;
        private int next;

        Write(long capBytes) {
            this.capBytes = capBytes;
        }

        /** Dictionary index plus one, or zero if this blob has to be written out. */
        int reference(byte[] blob) {
            Key key = Key.of(blob);
            if (key == null) {
                return 0;  // no digest available: store it, correctness over size
            }
            Integer index = known.get(key);
            if (index != null) {
                return index.intValue() + 1;
            }
            if (bytes + blob.length <= capBytes) {
                known.put(key, Integer.valueOf(next++));
                bytes += blob.length;
            }
            return 0;
        }
    }

    /** Holds the blobs themselves, since reading has to reproduce them exactly. */
    static final class Read {
        private final List<byte[]> blobs = new ArrayList<byte[]>();
        private final long capBytes;
        private long bytes;

        Read(long capBytes) {
            this.capBytes = capBytes;
        }

        /** Called for every blob written out, in order, mirroring what the writer remembered. */
        void offer(byte[] blob) {
            if (bytes + blob.length <= capBytes) {
                blobs.add(blob);
                bytes += blob.length;
            }
        }

        byte[] get(int reference) {
            int index = reference - 1;
            return index >= 0 && index < blobs.size() ? blobs.get(index) : null;
        }
    }

    private static final class Key {
        private final byte[] digest;
        private final int hash;

        private Key(byte[] digest) {
            this.digest = digest;
            this.hash = Arrays.hashCode(digest);
        }

        static Key of(byte[] blob) {
            try {
                MessageDigest sha = MessageDigest.getInstance("SHA-256");
                sha.update(blob);
                return new Key(sha.digest());
            } catch (Exception unavailable) {
                return null;
            }
        }

        @Override
        public int hashCode() {
            return hash;
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof Key && Arrays.equals(digest, ((Key) other).digest);
        }
    }
}

package top.fpsmaster.runtime.remap;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Tiny v2 mappings: official (notch) ↔ named (MCP), enough for runtime deobfuscation.
 */
public final class RuntimeMappings {

    /** official internal name → named */
    public final Map<String, String> classOfficialToNamed = new HashMap<String, String>();
    /** named internal name → official */
    public final Map<String, String> classNamedToOfficial = new HashMap<String, String>();
    /** officialOwner + '\0' + officialName + officialDesc → namedName */
    public final Map<String, String> methodOfficialToNamed = new HashMap<String, String>();
    /** officialOwner + '\0' + officialName + officialDesc → namedName (fields use field desc) */
    public final Map<String, String> fieldOfficialToNamed = new HashMap<String, String>();

    public static RuntimeMappings load(Path tinyFile) throws IOException {
        InputStream in = Files.newInputStream(tinyFile);
        try {
            return load(in);
        } finally {
            in.close();
        }
    }

    public static RuntimeMappings load(InputStream in) throws IOException {
        RuntimeMappings out = new RuntimeMappings();
        BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
        String header = reader.readLine();
        if (header == null || !header.startsWith("tiny\t")) {
            throw new IOException("Not a tiny mappings file");
        }
        // tiny 2 0 official intermediary named  → namespaces[0]=official, [2]=named
        String[] headerParts = header.split("\t");
        if (headerParts.length < 5) {
            throw new IOException("Unexpected tiny header: " + header);
        }

        String currentOfficialClass = null;
        String line;
        while ((line = reader.readLine()) != null) {
            if (line.isEmpty() || line.charAt(0) == '#') {
                continue;
            }
            if (line.charAt(0) == 'c' && line.length() > 1 && line.charAt(1) == '\t') {
                String[] p = line.split("\t");
                // c <official> <intermediary> <named>
                if (p.length >= 4) {
                    currentOfficialClass = p[1];
                    String named = p[3];
                    out.classOfficialToNamed.put(currentOfficialClass, named);
                    out.classNamedToOfficial.put(named, currentOfficialClass);
                }
                continue;
            }
            if (currentOfficialClass == null) {
                continue;
            }
            if (line.startsWith("\tm\t") || line.startsWith("\tf\t")) {
                // Leading tab ⇒ split yields ["", "m"|"f", desc, officialName, intermediary, named]
                String[] p = line.split("\t", -1);
                if (p.length >= 6) {
                    String kind = p[1];
                    String desc = p[2];
                    String officialName = p[3];
                    String namedName = p[5];
                    String k = key(currentOfficialClass, officialName, desc);
                    if ("m".equals(kind)) {
                        out.methodOfficialToNamed.put(k, namedName);
                    } else {
                        out.fieldOfficialToNamed.put(k, namedName);
                    }
                }
            }
        }
        return out;
    }

    static String key(String owner, String name, String desc) {
        return owner + '\0' + name + desc;
    }

    public String toNamedClass(String officialInternal) {
        String named = classOfficialToNamed.get(officialInternal);
        return named != null ? named : officialInternal;
    }

    public String toOfficialClass(String namedInternal) {
        String official = classNamedToOfficial.get(namedInternal);
        return official != null ? official : namedInternal;
    }
}

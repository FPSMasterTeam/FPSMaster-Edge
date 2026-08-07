package top.fpsmaster.runtime.remap;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.commons.Remapper;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * ASM remapper: official (notch) → named (MCP).
 * Climbs supers <b>and interfaces</b> so interface methods (e.g. {@code IStringSerializable.getName})
 * remap consistently on implementing classes.
 */
final class OfficialToNamedRemapper extends Remapper {

    private final RuntimeMappings mappings;
    private final Map<String, String> superOf = new HashMap<String, String>();
    private final Map<String, String[]> interfacesOf = new HashMap<String, String[]>();

    OfficialToNamedRemapper(RuntimeMappings mappings) {
        this.mappings = mappings;
    }

    void putHierarchy(String officialClass, String officialSuper, String[] officialInterfaces) {
        if (officialClass == null) {
            return;
        }
        if (officialSuper != null) {
            superOf.put(officialClass, officialSuper);
        }
        if (officialInterfaces != null && officialInterfaces.length > 0) {
            interfacesOf.put(officialClass, officialInterfaces);
        }
    }

    void learnHierarchy(byte[] classBytes) {
        if (classBytes == null) {
            return;
        }
        ClassReader reader = new ClassReader(classBytes);
        putHierarchy(reader.getClassName(), reader.getSuperName(), reader.getInterfaces());
    }

    @Override
    public String map(String typeName) {
        return mappings.toNamedClass(typeName);
    }

    @Override
    public String mapFieldName(String owner, String name, String desc) {
        String mapped = lookupField(owner, name, desc, new HashSet<String>());
        return mapped != null ? mapped : name;
    }

    @Override
    public String mapMethodName(String owner, String name, String desc) {
        if ("<init>".equals(name) || "<clinit>".equals(name)) {
            return name;
        }
        String mapped = lookupMethod(owner, name, desc, new HashSet<String>());
        return mapped != null ? mapped : name;
    }

    private String lookupField(String owner, String name, String desc, Set<String> seen) {
        if (owner == null || !seen.add(owner)) {
            return null;
        }
        String mapped = mappings.fieldOfficialToNamed.get(RuntimeMappings.key(owner, name, desc));
        if (mapped != null) {
            return mapped;
        }
        String fromSuper = lookupField(superOf.get(owner), name, desc, seen);
        if (fromSuper != null) {
            return fromSuper;
        }
        String[] ifaces = interfacesOf.get(owner);
        if (ifaces != null) {
            for (String iface : ifaces) {
                String fromIface = lookupField(iface, name, desc, seen);
                if (fromIface != null) {
                    return fromIface;
                }
            }
        }
        return null;
    }

    private String lookupMethod(String owner, String name, String desc, Set<String> seen) {
        if (owner == null || !seen.add(owner)) {
            return null;
        }
        String mapped = mappings.methodOfficialToNamed.get(RuntimeMappings.key(owner, name, desc));
        if (mapped != null) {
            return mapped;
        }
        String fromSuper = lookupMethod(superOf.get(owner), name, desc, seen);
        if (fromSuper != null) {
            return fromSuper;
        }
        String[] ifaces = interfacesOf.get(owner);
        if (ifaces != null) {
            for (String iface : ifaces) {
                String fromIface = lookupMethod(iface, name, desc, seen);
                if (fromIface != null) {
                    return fromIface;
                }
            }
        }
        return null;
    }

    // silence unused warning if any
    @SuppressWarnings("unused")
    private static String dump(String[] a) {
        return Arrays.toString(a);
    }
}

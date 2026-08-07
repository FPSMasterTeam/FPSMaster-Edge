package top.fpsmaster.runtime.remap;

import net.minecraft.launchwrapper.IClassNameTransformer;
import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.RemappingClassAdapter;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * LaunchWrapper deobfuscation: load real notch {@code client.jar} bytes, expose MCP-named classes.
 *
 * <p>{@link IClassNameTransformer} makes LaunchClassLoader look up {@code aya.class} when code asks
 * for {@code GuiMainMenu}, then this transformer rewrites the bytecode to named members so Mixin
 * can target MCP names.
 */
public final class RuntimeDeobfTransformer implements IClassTransformer, IClassNameTransformer {

    private static final String MAPPINGS_PROP = "fpsmaster.runtime.mappings";
    private static final String MAPPINGS_PROP_LEGACY = "fpsmaster.poc.mappings";
    private static final String VANILLA_JAR_PROP = "fpsmaster.runtime.vanillaJar";
    private static final String VANILLA_JAR_PROP_LEGACY = "fpsmaster.poc.vanillaJar";

    private final RuntimeMappings mappings;
    private final OfficialToNamedRemapper remapper;

    public RuntimeDeobfTransformer() {
        String mappingsPath = firstProp(MAPPINGS_PROP, MAPPINGS_PROP_LEGACY);
        if (mappingsPath == null || mappingsPath.isEmpty()) {
            throw new IllegalStateException("Missing -D" + MAPPINGS_PROP + "=/path/to/mappings.tiny");
        }
        try {
            this.mappings = RuntimeMappings.load(new File(mappingsPath).toPath());
        } catch (IOException e) {
            throw new ExceptionInInitializerError(e);
        }
        this.remapper = new OfficialToNamedRemapper(this.mappings);
        preScanHierarchy(firstProp(VANILLA_JAR_PROP, VANILLA_JAR_PROP_LEGACY));
        System.out.println("[FPSMaster Runtime] Deobf transformer ready ("
                + mappings.classOfficialToNamed.size() + " classes, official→named)");
    }

    private static String firstProp(String primary, String legacy) {
        String v = System.getProperty(primary);
        if (v != null && !v.isEmpty()) {
            return v;
        }
        return System.getProperty(legacy);
    }

    private void preScanHierarchy(String vanillaJarPath) {
        if (vanillaJarPath == null || vanillaJarPath.isEmpty()) {
            System.out.println("[FPSMaster Runtime] WARN: no -D" + VANILLA_JAR_PROP + "; inherited member remap may miss");
            return;
        }
        File jar = new File(vanillaJarPath);
        if (!jar.isFile()) {
            System.out.println("[FPSMaster Runtime] WARN: vanilla jar missing: " + jar);
            return;
        }
        int count = 0;
        try {
            JarFile jarFile = new JarFile(jar);
            try {
                Enumeration<JarEntry> entries = jarFile.entries();
                while (entries.hasMoreElements()) {
                    JarEntry entry = entries.nextElement();
                    String name = entry.getName();
                    if (!name.endsWith(".class") || entry.isDirectory()) {
                        continue;
                    }
                    InputStream in = jarFile.getInputStream(entry);
                    try {
                        byte[] bytes = readAll(in);
                        ClassReader reader = new ClassReader(bytes);
                        remapper.putHierarchy(reader.getClassName(), reader.getSuperName(), reader.getInterfaces());
                        count++;
                    } finally {
                        in.close();
                    }
                }
            } finally {
                jarFile.close();
            }
        } catch (IOException e) {
            throw new ExceptionInInitializerError(e);
        }
        System.out.println("[FPSMaster Runtime] Hierarchy pre-scan: " + count + " classes from " + jar.getName());
    }

    private static byte[] readAll(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream(4096);
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) >= 0) {
            out.write(buf, 0, n);
        }
        return out.toByteArray();
    }

    @Override
    public String unmapClassName(String name) {
        // Runtime / named name → notch name for jar resource lookup.
        String internal = name.replace('.', '/');
        String official = mappings.classNamedToOfficial.get(internal);
        return official != null ? official.replace('/', '.') : name;
    }

    @Override
    public String remapClassName(String name) {
        // Notch name → named runtime name for defineClass.
        String internal = name.replace('.', '/');
        String named = mappings.classOfficialToNamed.get(internal);
        return named != null ? named.replace('/', '.') : name;
    }

    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        if (basicClass == null) {
            return null;
        }
        String officialInternal = name != null ? name.replace('.', '/') : null;
        if (officialInternal == null) {
            return basicClass;
        }
        boolean minecraftClass = mappings.classOfficialToNamed.containsKey(officialInternal);
        boolean optifineClass = isOptiFinePackage(officialInternal);
        if (!minecraftClass && !optifineClass) {
            return basicClass;
        }
        try {
            remapper.learnHierarchy(basicClass);
            ClassReader reader = new ClassReader(basicClass);
            ClassWriter writer = new ClassWriter(reader, 0);
            reader.accept(new RemappingClassAdapter(writer, remapper), ClassReader.EXPAND_FRAMES);
            byte[] out = writer.toByteArray();
            // OptiFine Config toggles Display.setResizable after create; on modern macOS that
            // throws NSInternalInconsistencyException (geometry only on main thread) and kills the JVM.
            if ("Config".equals(officialInternal) && isMacOs()) {
                out = stripLwjglDisplaySetResizable(out);
            }
            return out;
        } catch (Throwable t) {
            System.err.println("[FPSMaster Runtime] Failed to deobfuscate " + name + ": " + t);
            t.printStackTrace(System.err);
            return basicClass;
        }
    }

    private static boolean isMacOs() {
        String os = System.getProperty("os.name", "");
        return os.toLowerCase().contains("mac");
    }

    private static byte[] stripLwjglDisplaySetResizable(byte[] classBytes) {
        ClassReader reader = new ClassReader(classBytes);
        ClassWriter writer = new ClassWriter(reader, 0);
        reader.accept(new ClassVisitor(Opcodes.ASM5, writer) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
                MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
                return new MethodVisitor(Opcodes.ASM5, mv) {
                    @Override
                    public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
                        if (opcode == Opcodes.INVOKESTATIC
                                && "org/lwjgl/opengl/Display".equals(owner)
                                && "setResizable".equals(name)
                                && "(Z)V".equals(desc)) {
                            // Pop the boolean arg; skip the native call.
                            super.visitInsn(Opcodes.POP);
                            return;
                        }
                        super.visitMethodInsn(opcode, owner, name, desc, itf);
                    }
                };
            }
        }, 0);
        System.out.println("[FPSMaster Runtime] macOS: stripped Display.setResizable from OptiFine Config");
        return writer.toByteArray();
    }

    /**
     * OptiFine ships with compile-time notch references into MC ({@code ldc class adg} etc.),
     * and also embeds notch-signature Forge stub interfaces under {@code net/minecraftforge/}.
     * With {@link IClassNameTransformer} defining MC under MCP names, those OF/stub classes must
     * be remapped too or they die with {@code NoClassDefFoundError}/{@code NoSuchMethodError}.
     */
    private static boolean isOptiFinePackage(String internalName) {
        return "Config".equals(internalName)
                || internalName.startsWith("optifine/")
                || internalName.startsWith("net/optifine/")
                || internalName.startsWith("shadersmod/")
                || internalName.startsWith("net/minecraftforge/");
    }
}

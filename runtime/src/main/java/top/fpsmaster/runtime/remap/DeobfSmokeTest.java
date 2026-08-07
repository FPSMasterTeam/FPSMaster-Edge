package top.fpsmaster.runtime.remap;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.commons.RemappingClassAdapter;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/** Offline check: remap aya → GuiMainMenu and verify splashText / initGui. */
public final class DeobfSmokeTest {
    public static void main(String[] args) throws Exception {
        String mappingsPath = System.getProperty("fpsmaster.runtime.mappings");
        String jarPath = System.getProperty("fpsmaster.runtime.vanillaJar");
        RuntimeMappings mappings = RuntimeMappings.load(new File(mappingsPath).toPath());
        OfficialToNamedRemapper remapper = new OfficialToNamedRemapper(mappings);

        JarFile jar = new JarFile(jarPath);
        try {
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (!entry.getName().endsWith(".class")) continue;
                InputStream in = jar.getInputStream(entry);
                try {
                    byte[] b = read(in);
                    ClassReader cr = new ClassReader(b);
                    remapper.putHierarchy(cr.getClassName(), cr.getSuperName(), cr.getInterfaces());
                } finally {
                    in.close();
                }
            }

            System.out.println("field aya/r => " + remapper.mapFieldName("aya", "r", "Ljava/lang/String;"));
            System.out.println("class aya => " + remapper.map("aya"));
            System.out.println("method aya/b ()V => " + remapper.mapMethodName("aya", "b", "()V"));
            System.out.println("method axu/b ()V => " + remapper.mapMethodName("axu", "b", "()V"));
            System.out.println("method ajy$a/l ()Ljava/lang/String; => "
                    + remapper.mapMethodName("ajy$a", "l", "()Ljava/lang/String;"));
            System.out.println("method nw/l ()Ljava/lang/String; => "
                    + remapper.mapMethodName("nw", "l", "()Ljava/lang/String;"));

            InputStream in = jar.getInputStream(jar.getJarEntry("aya.class"));
            byte[] raw = read(in);
            in.close();

            ClassReader reader = new ClassReader(raw);
            ClassWriter writer = new ClassWriter(reader, 0);
            reader.accept(new RemappingClassAdapter(writer, remapper), ClassReader.EXPAND_FRAMES);
            byte[] out = writer.toByteArray();

            // Write and javap via simple UTF scan for names in constant pool-ish: use ClassReader again
            final boolean[] found = new boolean[] { false, false };
            new ClassReader(out).accept(new org.objectweb.asm.ClassVisitor(org.objectweb.asm.Opcodes.ASM5) {
                @Override
                public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
                    System.out.println("remapped class name=" + name + " super=" + superName);
                }

                @Override
                public org.objectweb.asm.FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
                    if ("splashText".equals(name) || "r".equals(name)) {
                        System.out.println("field " + name + " " + desc);
                        if ("splashText".equals(name)) found[0] = true;
                    }
                    return null;
                }

                @Override
                public org.objectweb.asm.MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
                    if ("initGui".equals(name) || ("b".equals(name) && "()V".equals(desc))) {
                        System.out.println("method " + name + desc);
                        if ("initGui".equals(name)) found[1] = true;
                    }
                    return null;
                }
            }, 0);

            System.out.println("OK splashText=" + found[0] + " initGui=" + found[1]);
            if (!found[0] || !found[1]) {
                System.exit(1);
            }
        } finally {
            jar.close();
        }
    }

    private static byte[] read(InputStream in) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) >= 0) out.write(buf, 0, n);
        return out.toByteArray();
    }
}

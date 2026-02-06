package top.fpsmaster.event;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import top.fpsmaster.utils.runtime.EventClassLoader;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static org.objectweb.asm.Opcodes.*;

public class ASMHandler {
    private static int index = 0;

    public static Handler loadHandlerClass(Object listener, Method method) {
        Class<?> declaringClass = method.getDeclaringClass();

        index++;

        String className = "EventHandler_" + index;

        ClassWriter cv = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);

        cv.visit(V1_8, ACC_PUBLIC | ACC_SUPER, className, null, Type.getInternalName(Handler.class), null);
        cv.visitSource(className + ".java", null);

        // Original code: super(listener, method)
        MethodVisitor mv = cv.visitMethod(ACC_PUBLIC, "<init>", "(Ljava/lang/Object;Ljava/lang/reflect/Method;)V", null, null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitVarInsn(ALOAD, 1);
        mv.visitVarInsn(ALOAD, 2);
        mv.visitMethodInsn(INVOKESPECIAL, Type.getInternalName(Handler.class), "<init>", "(Ljava/lang/Object;Ljava/lang/reflect/Method;)V", false);
        mv.visitInsn(RETURN);
        mv.visitMaxs(3, 3);
        mv.visitEnd();

        // Original code: this.getMethod().invoke(this.getListener(), event)
        mv = cv.visitMethod(Opcodes.ACC_PUBLIC, "invoke", "(L" + Type.getInternalName(Event.class) + ";)V", null, null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKEVIRTUAL, className, "getMethod", "()Ljava/lang/reflect/Method;", false);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKEVIRTUAL, className, "getListener", "()Ljava/lang/Object;", false);
        mv.visitInsn(ICONST_1);
        mv.visitTypeInsn(ANEWARRAY, "java/lang/Object");
        mv.visitInsn(DUP);
        mv.visitInsn(ICONST_0);
        mv.visitVarInsn(ALOAD, 1);
        mv.visitInsn(AASTORE);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/reflect/Method", "invoke", "(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;", false);
        mv.visitInsn(POP);
        mv.visitInsn(RETURN);
        mv.visitMaxs(6, 3);
        mv.visitEnd();

        // Original code: this.getListener().getClass().getSimpleName() + " -> " + this.getMethod().getName()
        mv = cv.visitMethod(ACC_PUBLIC, "getLog", "()Ljava/lang/String;", null, null);
        mv.visitCode();
        mv.visitTypeInsn(NEW, "java/lang/StringBuilder");
        mv.visitInsn(DUP);
        mv.visitMethodInsn(INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "()V", false);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKEVIRTUAL, className, "getListener", "()Ljava/lang/Object;", false);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Object", "getClass", "()Ljava/lang/Class;", false);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Class", "getSimpleName", "()Ljava/lang/String;", false);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
        mv.visitLdcInsn(" -> ");
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKEVIRTUAL, className, "getMethod", "()Ljava/lang/reflect/Method;", false);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/reflect/Method", "getName", "()Ljava/lang/String;", false);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false);
        mv.visitInsn(ARETURN);
        mv.visitMaxs(5, 1);
        mv.visitEnd();

        cv.visitEnd();

        try {
            return (Handler) EventClassLoader.defineClass(declaringClass.getClassLoader(), className, cv.toByteArray()).getConstructor(Object.class, Method.class).newInstance(listener, method);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException |
                 ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
}




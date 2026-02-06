package top.fpsmaster.utils.runtime;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

public class EventClassLoader {
    private static final Map<ClassLoader, GeneratedClassLoader> loaders = Collections.synchronizedMap(new WeakHashMap<>());

    public static Class<?> defineClass(ClassLoader parentLoader, String name, byte[] data) throws ClassNotFoundException {
        GeneratedClassLoader loader = loaders.computeIfAbsent(parentLoader, GeneratedClassLoader::new);
        synchronized (loader.getClassLoadingLock(name)) {
            Class<?> clazz = loader.define(name, data);
            assert clazz.getName().equals(name);
            return clazz;
        }
    }

    private static class GeneratedClassLoader extends ClassLoader {
        public GeneratedClassLoader(ClassLoader parent) {
            super(parent);
            ClassLoader.registerAsParallelCapable();
        }

        public Class<?> define(String name, byte[] data) {
            synchronized (getClassLoadingLock(name)) {
                assert !hasClass(name);
                Class<?> clazz = defineClass(name, data, 0, data.length);
                resolveClass(clazz);
                return clazz;
            }
        }

        @Override
        public Object getClassLoadingLock(String name) {
            return super.getClassLoadingLock(name);
        }

        public boolean hasClass(String name) {
            synchronized (getClassLoadingLock(name)) {
                return findLoadedClass(name) != null;
            }
        }
    }
}




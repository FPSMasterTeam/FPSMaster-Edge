package top.fpsmaster.event;

import top.fpsmaster.exception.ExceptionHandler;
import top.fpsmaster.modules.logger.ClientLogger;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class EventDispatcher {
    // ConcurrentHashMap so registration (off-thread or concurrent) can't corrupt the map while
    // dispatch reads it; the per-event lists are CopyOnWriteArrayList (read-heavy, write-rare).
    private static final Map<Class<? extends Event>, List<Handler>> eventListeners = new ConcurrentHashMap<>();

    public static void registerListener(Object listener) {
        Method[] methods = listener.getClass().getDeclaredMethods();
        for (Method method : methods) {
            if (method.isAnnotationPresent(Subscribe.class) && method.getParameterCount() == 1) {
                Class<?> parameterType = method.getParameterTypes()[0];
                if (Event.class.isAssignableFrom(parameterType)) {
                    Class<? extends Event> eventType = (Class<? extends Event>) parameterType;
                    List<Handler> listeners = eventListeners.computeIfAbsent(eventType, k -> new CopyOnWriteArrayList<>());
                    // Identity + method: Module.onEnable / always-on ctor registration used to stack
                    // duplicate handlers for the same listener, retaining extra MethodHandleHandler
                    // instances and double-firing every event.
                    if (alreadyRegistered(listeners, listener, method)) {
                        continue;
                    }
                    listeners.add(new MethodHandleHandler(listener, method));
                }
            }
        }
    }

    public static void unregisterListener(Object listener) {
        for (List<Handler> listeners : eventListeners.values()) {
            // Identity, not class equality: class-based removal dropped every instance of a type
            // (or left orphans when the same class had multiple live listeners).
            listeners.removeIf(handler -> handler.getListener() == listener);
        }
    }

    public static void dispatchEvent(Event event) {
        List<Handler> listeners = eventListeners.get(event.getClass());
        if (listeners != null) {
            for (Handler listener : listeners) {
                try {
                    listener.invoke(event);
                } catch (Throwable e) {
                    ClientLogger.warn("Failed to dispatch event " + event.getClass().getSimpleName() + " to listener " + listener.getLog());
                    if (e instanceof Exception) {
                        ExceptionHandler.handleModuleException((Exception) e, "Failed to dispatch event " + event.getClass().getSimpleName());
                    } else {
                        ClientLogger.error("Non-Exception Throwable: " + e.getMessage());
                    }
                }
            }
        }
    }

    private static boolean alreadyRegistered(List<Handler> listeners, Object listener, Method method) {
        for (Handler handler : listeners) {
            if (handler.getListener() == listener && handler.getMethod().equals(method)) {
                return true;
            }
        }
        return false;
    }
}

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

    /**
     * Subscribes every {@code @Subscribe} method on {@code listener}.
     *
     * <p>Registering the same object twice is ignored rather than doubled. Some listeners are
     * attached from more than one place — {@code ClientSettings} subscribes in its constructor so it
     * keeps working while the module reads as disabled, and {@code Module#onEnable} subscribes again
     * if config later enables it — and a second copy of a handler both fires the body twice and
     * keeps the listener referenced after one matching unregister.
     */
    public static void registerListener(Object listener) {
        Method[] methods = listener.getClass().getDeclaredMethods();
        for (Method method : methods) {
            if (method.isAnnotationPresent(Subscribe.class) && method.getParameterCount() == 1) {
                Class<?> parameterType = method.getParameterTypes()[0];
                if (Event.class.isAssignableFrom(parameterType)) {
                    Class<? extends Event> eventType = (Class<? extends Event>) parameterType;
                    List<Handler> listeners = eventListeners.computeIfAbsent(eventType, k -> new CopyOnWriteArrayList<>());
                    if (!isRegistered(listeners, listener, method)) {
                        listeners.add(new MethodHandleHandler(listener, method));
                    }
                }
            }
        }
    }

    /**
     * Unsubscribes {@code listener}.
     *
     * <p>Matched by identity. Matching by class instead — which is what this did — meant unregistering
     * one instance detached every other instance of the same class, leaving those listeners believing
     * they were still subscribed while their handlers were gone; and a handler that was removed on
     * someone else's behalf is a live object the bus no longer holds but nobody knows to re-add.
     */
    public static void unregisterListener(Object listener) {
        for (List<Handler> listeners : eventListeners.values()) {
            listeners.removeIf(eventListener -> eventListener.getListener() == listener);
        }
    }

    private static boolean isRegistered(List<Handler> listeners, Object listener, Method method) {
        for (Handler handler : listeners) {
            if (handler.getListener() == listener && method.equals(handler.getMethod())) {
                return true;
            }
        }
        return false;
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
}




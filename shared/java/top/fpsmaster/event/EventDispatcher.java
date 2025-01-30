package top.fpsmaster.event;

import top.fpsmaster.modules.logger.ClientLogger;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

public class EventDispatcher {
    private static final Map<Class<? extends Event>, List<Handler>> eventListeners = new HashMap<>();

    public static void registerListener(Object listener) {
        Method[] methods = listener.getClass().getDeclaredMethods();
        for (Method method : methods) {
            if (method.isAnnotationPresent(Subscribe.class) && method.getParameterCount() == 1) {
                Class<?> parameterType = method.getParameterTypes()[0];
                if (Event.class.isAssignableFrom(parameterType)) {
                    Class<? extends Event> eventType = (Class<? extends Event>) parameterType;
                    List<Handler> listeners = eventListeners.computeIfAbsent(eventType, k -> new CopyOnWriteArrayList<>());
                    listeners.add(ASMHandler.loadHandlerClass(listener, method));
                }
            }
        }
    }

    public static void unregisterListener(Object listener) {
        for (List<Handler> listeners : eventListeners.values()) {
            listeners.removeIf(eventListener -> eventListener.getListener().getClass().equals(listener.getClass()));
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
                    e.printStackTrace();
                }
            }
        }
    }
}

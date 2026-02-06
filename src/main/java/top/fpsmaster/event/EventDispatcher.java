package top.fpsmaster.event;

import top.fpsmaster.exception.ExceptionHandler;
import top.fpsmaster.modules.dev.DevMode;
import top.fpsmaster.modules.logger.ClientLogger;
import top.fpsmaster.utils.Utility;

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
            long l1 = System.currentTimeMillis();
            for (Handler listener : listeners) {
                try {
//                        Map<String, Long> usedTime = new HashMap<>();
                    long l = System.currentTimeMillis();
                    listener.invoke(event);
                    long time = System.currentTimeMillis() - l;
//                        usedTime.put(listener.getListener().getClass().getSimpleName(), time);
                    if (time > 3) {
                        String msg = "Event " + event.getClass().getSimpleName() + " in " + listener.listener.getClass().getSimpleName() + " took " + time + "ms to process";
                        Utility.sendClientDebug(msg);
                        ClientLogger.warn(msg);
                        for (StackTraceElement stackTraceElement : Thread.currentThread().getStackTrace()) {
                            ClientLogger.warn(stackTraceElement.toString());
                        }
                    }
                } catch (Throwable e) {
                    ClientLogger.warn("Failed to dispatch event " + event.getClass().getSimpleName() + " to listener " + listener.getLog());
                    if (e instanceof Exception) {
                        ExceptionHandler.handleModuleException((Exception) e, "Failed to dispatch event " + event.getClass().getSimpleName());
                        e.printStackTrace();
                    } else {
                        // For non-Exception Throwables, we still need to log them
                        ClientLogger.error("Non-Exception Throwable: " + e.getMessage());
                        e.printStackTrace();
                    }
                }
            }
            long time2 = System.currentTimeMillis() - l1;
            if (time2 > 50) {
                String msg = "Client events took " + time2 + "ms to process! please see logs to check it.";
                Utility.sendClientDebug(msg);
                ClientLogger.warn(msg);
            }
        }
    }
}

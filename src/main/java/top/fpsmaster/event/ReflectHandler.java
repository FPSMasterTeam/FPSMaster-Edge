package top.fpsmaster.event;

import top.fpsmaster.modules.logger.ClientLogger;

import java.lang.reflect.Method;

public class ReflectHandler extends Handler {
    public ReflectHandler(Class<? extends Event> listener, Method method) {
        super(listener, method);
    }

    @Override
    public void invoke(Event event) {
        try {
            method.invoke(listener, event);
        } catch (Exception e) {
            ClientLogger.error("Error when invoking event " + listener.getClass().getSimpleName() + " -> " + method.getName());
            e.printStackTrace();
        }
    }

    @Override
    public String getLog() {
        return listener.getClass().getSimpleName() + " -> " + method.getName();
    }
}




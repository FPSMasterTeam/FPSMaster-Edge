package top.fpsmaster.event;

import top.fpsmaster.modules.logger.ClientLogger;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;

public class MethodHandleHandler extends Handler {
    private final MethodHandle handle;

    public MethodHandleHandler(Object listener, Method method) {
        super(listener, method);
        try {
            method.setAccessible(true);
            this.handle = MethodHandles.lookup()
                    .unreflect(method)
                    .bindTo(listener)
                    .asType(MethodType.methodType(void.class, Event.class));
        } catch (IllegalAccessException exception) {
            throw new RuntimeException("Failed to create event handler for " + getLog(), exception);
        }
    }

    @Override
    public void invoke(Event event) {
        try {
            handle.invokeExact(event);
        } catch (Throwable throwable) {
            ClientLogger.error("Error when invoking event " + getLog());
            if (throwable instanceof RuntimeException) {
                throw (RuntimeException) throwable;
            }
            throw new RuntimeException(throwable);
        }
    }

    @Override
    public String getLog() {
        return listener.getClass().getSimpleName() + " -> " + method.getName();
    }
}

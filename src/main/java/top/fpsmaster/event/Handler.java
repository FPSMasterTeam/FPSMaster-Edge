package top.fpsmaster.event;

import java.lang.reflect.Method;

public abstract class Handler {
    Object listener;
    Method method;

    public Handler(Object listener, Method method) {
        this.listener = listener;
        this.method = method;
    }

    public abstract void invoke(Event event);

    public abstract String getLog();

    public Object getListener() {
        return listener;
    }

    public Method getMethod() {
        return method;
    }
}

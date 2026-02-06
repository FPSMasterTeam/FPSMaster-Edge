package top.fpsmaster.event;

public class CancelableEvent implements Event {
    private boolean isCanceled = false;

    public void cancel() {
        isCanceled = true;
    }

    public boolean isCanceled() {
        return isCanceled;
    }
}

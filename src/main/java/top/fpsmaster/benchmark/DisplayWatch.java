package top.fpsmaster.benchmark;

import org.lwjgl.opengl.Display;

/**
 * Detects frames that were disturbed by the desktop rather than by the workload.
 *
 * <p>Dragging the window puts Windows into a modal move loop: the application stops pumping
 * messages entirely, and the frame that spans the drag is recorded as a single enormous sample.
 * One such frame is enough to move a 60k-sample 1% low by tens of percent, which silently destroys
 * exactly the tail statistics this harness exists to measure — and an unattended run has no way to
 * know it happened.
 *
 * <p>Only <em>changes</em> count as disturbances. A window that is unfocused for the whole run is
 * in a steady state, not being perturbed; treating that as a disturbance excluded every frame of
 * several runs, because an unattended benchmark usually does not own the foreground. The focused
 * and unfocused frame counts are reported separately so the two can be compared rather than
 * conflated.
 */
public final class DisplayWatch {

    /**
     * Frames to discard after a disturbance ends. The first frame back is still paying for the
     * interruption (cold caches, a backlog of queued input), so it is not representative either.
     */
    private static final int RECOVERY_FRAMES = 1;

    private int lastX = Integer.MIN_VALUE;
    private int lastY = Integer.MIN_VALUE;
    private boolean lastActive;
    private boolean haveLastActive;
    private int recoveryRemaining;
    private long disturbedFrames;
    private long unfocusedFrames;

    /** Returns true when the frame just completed should be excluded from the sample set. */
    public boolean pollDisturbed() {
        int x = Display.getX();
        int y = Display.getY();
        boolean active = Display.isActive();

        boolean moved = lastX != Integer.MIN_VALUE && (x != lastX || y != lastY);
        boolean focusChanged = haveLastActive && active != lastActive;

        lastX = x;
        lastY = y;
        lastActive = active;
        haveLastActive = true;
        if (!active) {
            unfocusedFrames++;
        }

        boolean disturbed = moved || focusChanged || Display.wasResized();
        if (disturbed) {
            recoveryRemaining = RECOVERY_FRAMES;
        } else if (recoveryRemaining > 0) {
            recoveryRemaining--;
            disturbed = true;
        }
        if (disturbed) {
            disturbedFrames++;
        }
        return disturbed;
    }

    public void reset() {
        disturbedFrames = 0L;
        unfocusedFrames = 0L;
        recoveryRemaining = 0;
    }

    public long disturbedFrames() {
        return disturbedFrames;
    }

    /**
     * Frames rendered without the window focused. Not excluded — reported so that a run which spent
     * its measurement window in the background can be compared against one that did not, instead of
     * the difference being silently absorbed.
     */
    public long unfocusedFrames() {
        return unfocusedFrames;
    }
}

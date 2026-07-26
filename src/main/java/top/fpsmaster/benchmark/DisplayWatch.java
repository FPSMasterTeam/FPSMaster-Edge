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
 * <p>Focus changes, resizes and window moves are all detectable. Frames where any of them occurred
 * are excluded and counted; the count is always reported, never silently absorbed.
 */
public final class DisplayWatch {

    /**
     * Frames to discard after a disturbance ends. The first frame back is still paying for the
     * interruption (cold caches, a backlog of queued input), so it is not representative either.
     */
    private static final int RECOVERY_FRAMES = 1;

    private int lastX = Integer.MIN_VALUE;
    private int lastY = Integer.MIN_VALUE;
    private int recoveryRemaining;
    private long disturbedFrames;

    /** Returns true when the frame just completed should be excluded from the sample set. */
    public boolean pollDisturbed() {
        int x = Display.getX();
        int y = Display.getY();
        boolean moved = lastX != Integer.MIN_VALUE && (x != lastX || y != lastY);
        lastX = x;
        lastY = y;

        boolean disturbed = moved || Display.wasResized() || !Display.isActive() || !Display.isVisible();
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
        recoveryRemaining = 0;
    }

    public long disturbedFrames() {
        return disturbedFrames;
    }
}

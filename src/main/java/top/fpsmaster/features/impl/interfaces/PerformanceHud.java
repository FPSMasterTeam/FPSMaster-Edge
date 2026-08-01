package top.fpsmaster.features.impl.interfaces;

import top.fpsmaster.features.impl.InterfaceModule;
import top.fpsmaster.features.manager.Category;
import top.fpsmaster.features.settings.impl.BooleanSetting;

/**
 * The performance overlay: frame rate, what the bad frames look like, and what the heap is doing.
 *
 * <p>Separate from {@code FPSDisplay}, which is one number in a corner and should stay that way.
 * This is the diagnostic view — the thing to have open while judging whether a setting helped, and
 * the reason it exists is that a frame rate cannot answer that on its own. A change can raise the
 * median and make every hundredth frame worse at the same time, and one number shows the half of
 * that which flatters it.
 *
 * <p>Each row is a switch, so it can sit at one line while playing and open up while testing.
 */
public class PerformanceHud extends InterfaceModule {

    /**
     * The frame time trace: six seconds of history, worst frame per fifty-millisecond column.
     *
     * <p>Worth having on even when the rows are off. A number says the frame rate is 300; the trace
     * says whether it has been 300 for six seconds or has been sawing between 500 and 90, and those
     * two feel nothing alike.
     */
    public BooleanSetting showGraph = new BooleanSetting("ShowGraph", true);

    /** Frame time distribution: average, 1% low, median, worst, and the count of hitches. */
    public BooleanSetting showDistribution = new BooleanSetting("ShowDistribution", true);

    /** Heap in use against the maximum, and the rate the client thread is allocating at. */
    public BooleanSetting showMemory = new BooleanSetting("ShowMemory", true);

    /** Collections a second, and the milliseconds a second they stop the game for. */
    public BooleanSetting showGarbageCollection = new BooleanSetting("ShowGC", true);

    /**
     * Colours the frame rate and the 1% low by how good they are.
     *
     * <p>The thresholds are deliberately about this game rather than about video: 60 is where the
     * frame stops keeping up with a 20-tick server's interpolation, and 30 is where aim starts to
     * suffer. Somebody watching for a stutter should not have to read the number to see one.
     */
    public BooleanSetting colorByHealth = new BooleanSetting("ColorByHealth", true);

    public PerformanceHud() {
        super("PerformanceHud", Category.Interface);
        addSettings(showGraph, showDistribution, showMemory, showGarbageCollection, colorByHealth);
        addSettings(rounded, backgroundColor, fontShadow, betterFont, bg, roundRadius, spacing);
    }
}

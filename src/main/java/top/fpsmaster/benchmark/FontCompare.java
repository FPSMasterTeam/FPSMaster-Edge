package top.fpsmaster.benchmark;

import net.minecraft.client.Minecraft;
import top.fpsmaster.FPSMaster;
import top.fpsmaster.modules.logger.ClientLogger;

/**
 * Draws the same strings through vanilla's font renderer and the client's own, and times both.
 *
 * <p>The two had never been compared on equal terms. The numbers that existed came from different
 * text in different places — the client's renderer measured on HUD components, vanilla's on chat —
 * which says nothing about which is faster. This draws one fixed set of strings through each, the
 * same number of times, in the same frame.
 *
 * <p>They are not equivalent renderers and the comparison should be read with that in mind: vanilla
 * draws a bitmap font at its native size with no coverage beyond its texture pages, while the
 * client's rasterises TrueType into an atlas and draws it at half scale, which is what lets it show
 * CJK at all. The Latin lines are the like-for-like part; the CJK line is there because vanilla
 * reaches its unicode page fallback for it, which is a different path again.
 *
 * <pre>
 *   -Dedge.exp.fontCompare=true
 * </pre>
 */
public final class FontCompare {

    private static final String[] SAMPLES = {
            "Play",
            "SuperSkidder joined the lobby",
            "§aYou §7have §c14 §7coins and a very long tail of text to lay out",
            "你已加入大厅，祝你游戏愉快",
    };

    /** Enough repeats that a frame's worth of work is measurable against the clock's resolution. */
    private static final int REPEATS = 8;

    private static final long[] VANILLA = new long[SAMPLES.length];
    private static final long[] OURS = new long[SAMPLES.length];
    private static long strings;
    private static long frames;

    private FontCompare() {
    }

    public static boolean enabled() {
        return Experiments.active(Experiments.FONT_COMPARE);
    }

    /** Called once per overlay, from inside the frame so both pay the same GL state costs. */
    public static void draw() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.fontRendererObj == null || FPSMaster.fontManager == null
                || FPSMaster.fontManager.s16 == null) {
            return;
        }

        // Per sample, because one of them is CJK and vanilla reaches its unicode page fallback
        // for it - averaging that in would flatter the client's renderer for the wrong reason.
        for (int i = 0; i < SAMPLES.length; i++) {
            long start = System.nanoTime();
            for (int repeat = 0; repeat < REPEATS; repeat++) {
                mc.fontRendererObj.drawString(SAMPLES[i], 4, 4 + i * 10, 0xFFFFFF);
            }
            VANILLA[i] += System.nanoTime() - start;

            start = System.nanoTime();
            for (int repeat = 0; repeat < REPEATS; repeat++) {
                FPSMaster.fontManager.s16.drawString(SAMPLES[i], 200f, 4f + i * 10f, 0xFFFFFF);
            }
            OURS[i] += System.nanoTime() - start;
        }

        strings += REPEATS * SAMPLES.length;
        if (++frames % 600L == 0L) {
            StringBuilder line = new StringBuilder("over " + frames + " frames:");
            long perSample = strings / SAMPLES.length;
            for (int i = 0; i < SAMPLES.length; i++) {
                line.append(String.format(" | \"%s\" vanilla %.2fus ours %.2fus (%.2fx)",
                        SAMPLES[i].length() > 18 ? SAMPLES[i].substring(0, 18) + ".." : SAMPLES[i],
                        VANILLA[i] / 1000.0d / perSample, OURS[i] / 1000.0d / perSample,
                        OURS[i] / (double) VANILLA[i]));
            }
            ClientLogger.info("fontcmp", line.toString());
        }
    }
}

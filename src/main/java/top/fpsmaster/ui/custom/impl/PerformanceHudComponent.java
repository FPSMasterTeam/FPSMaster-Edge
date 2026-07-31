package top.fpsmaster.ui.custom.impl;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import org.lwjgl.opengl.GL11;
import top.fpsmaster.features.impl.interfaces.PerformanceHud;
import top.fpsmaster.modules.perf.PerformanceMonitor;
import top.fpsmaster.ui.custom.Component;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

/**
 * Draws the performance overlay.
 *
 * <p>The frame rate is on its own line and larger than the rest, because it is what someone glances
 * at; everything under it is what they read when the glance was not reassuring. Rows are built into
 * a list first and measured as a block, so a row switched off does not leave a gap and the panel is
 * never wider than the widest thing actually in it.
 */
public class PerformanceHudComponent extends Component {

    private static final int HEADLINE_FONT = 22;
    private static final int ROW_FONT = 16;
    private static final float PADDING = 4f;
    private static final float ROW_HEIGHT = 11f;

    private static final Color GOOD = new Color(126, 211, 133);
    private static final Color FAIR = new Color(232, 205, 116);
    private static final Color POOR = new Color(226, 118, 118);
    private static final Color LABEL = new Color(160, 160, 160);
    private static final Color VALUE = new Color(235, 235, 235);

    /** Below this the frame is no longer keeping up with a 20-tick server's interpolation. */
    private static final double FAIR_FPS = 60.0d;
    /** Below this, aiming suffers. */
    private static final double POOR_FPS = 30.0d;

    /** Height of the trace, and the frame time its top of scale represents. */
    private static final float GRAPH_HEIGHT = 26f;
    private static final float GRAPH_CEILING_MS = 50f;

    /** Where the reference line sits: sixty frames a second. */
    private static final float REFERENCE_MS = 1000f / 60f;

    private final List<String> rows = new ArrayList<String>();
    private final float[] trace = new float[PerformanceMonitor.columns()];

    public PerformanceHudComponent() {
        super(PerformanceHud.class);
        allowScale = true;
    }

    @Override
    public void draw(float x, float y) {
        super.draw(x, y);
        PerformanceHud hud = (PerformanceHud) mod;

        String headline = String.format("%.0f FPS", PerformanceMonitor.fps());
        rows.clear();
        if (hud.showDistribution.getValue()) {
            rows.add(String.format("avg %.0f    1%% low %.0f",
                    PerformanceMonitor.averageFps(), PerformanceMonitor.onePercentLowFps()));
            rows.add(String.format("p50 %.1fms    worst %.1fms    hitches %d",
                    PerformanceMonitor.medianFrameMs(), PerformanceMonitor.worstFrameMs(),
                    PerformanceMonitor.hitches()));
        }
        if (hud.showMemory.getValue()) {
            rows.add(String.format("heap %d/%d MB (%.0f%%)    alloc %.0f MB/s",
                    PerformanceMonitor.heapUsedMb(), PerformanceMonitor.heapMaxMb(),
                    PerformanceMonitor.heapFraction() * 100.0d,
                    PerformanceMonitor.allocatedMbPerSecond()));
        }
        if (hud.showGarbageCollection.getValue()) {
            rows.add(String.format("gc %.1f/s    %.1f ms/s",
                    PerformanceMonitor.gcPerSecond(), PerformanceMonitor.gcMillisPerSecond()));
        }
        boolean graph = hud.showGraph.getValue();

        float widest = getStringWidth(HEADLINE_FONT, headline);
        for (int i = 0; i < rows.size(); i++) {
            widest = Math.max(widest, getStringWidth(ROW_FONT, rows.get(i)));
        }
        if (graph) {
            widest = Math.max(widest, trace.length);
        }
        width = widest + PADDING * 2f;
        height = PADDING * 2f + 12f + rows.size() * ROW_HEIGHT + (graph ? GRAPH_HEIGHT + 3f : 0f);

        drawRect(x - 2, y, width, height, mod.backgroundColor.getColor());

        int headlineColor = hud.colorByHealth.getValue()
                ? health(PerformanceMonitor.fps()).getRGB()
                : VALUE.getRGB();
        drawString(HEADLINE_FONT, true, headline, x, y + PADDING - 2f, headlineColor);

        float rowY = y + PADDING + 10f;
        for (int i = 0; i < rows.size(); i++) {
            // The first row carries the 1% low, which is the number worth colouring: it is the one
            // that says how bad it gets, and it is the one a change can quietly make worse while
            // improving the headline above it.
            int color = i == 0 && hud.showDistribution.getValue() && hud.colorByHealth.getValue()
                    ? health(PerformanceMonitor.onePercentLowFps()).getRGB()
                    : (i == 0 ? VALUE.getRGB() : LABEL.getRGB());
            drawString(ROW_FONT, rows.get(i), x, rowY, color);
            rowY += ROW_HEIGHT;
        }
        if (graph) {
            drawTrace(x, rowY + 1f, widest);
        }
    }

    /**
     * Draws the whole trace in one batch.
     *
     * <p>A hundred and twenty columns and a reference line, submitted as a single
     * {@code POSITION_COLOR} batch and one {@code draw()}. Drawing them as a hundred and twenty
     * filled rectangles would be a hundred and twenty draw calls a frame, and an overlay that costs
     * more than the things it is there to measure is worse than no overlay — this client has
     * already found one bracket where the measuring apparatus was most of what the bracket read.
     *
     * <p>Scaled against a fixed ceiling rather than the worst column in view. An autoscaling trace
     * redraws its own axis every time something spikes, so a stutter makes the graph jump and
     * everything before it shrink, and two moments cannot be compared by looking at them.
     */
    private void drawTrace(float x, float y, float availableWidth) {
        PerformanceMonitor.traceInto(trace);
        float columnWidth = availableWidth / trace.length;

        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0);
        WorldRenderer worldRenderer = Tessellator.getInstance().getWorldRenderer();
        worldRenderer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);

        quad(worldRenderer, x, y, availableWidth, GRAPH_HEIGHT, 255, 255, 255, 18);

        for (int i = 0; i < trace.length; i++) {
            float ms = trace[i];
            if (ms <= 0f) {
                continue;
            }
            float bar = Math.min(GRAPH_HEIGHT, GRAPH_HEIGHT * (ms / GRAPH_CEILING_MS));
            Color color = health(ms <= 0f ? 0f : 1000f / ms);
            quad(worldRenderer, x + i * columnWidth, y + GRAPH_HEIGHT - bar,
                    Math.max(1f, columnWidth), bar,
                    color.getRed(), color.getGreen(), color.getBlue(), 210);
        }

        float referenceY = y + GRAPH_HEIGHT - GRAPH_HEIGHT * (REFERENCE_MS / GRAPH_CEILING_MS);
        quad(worldRenderer, x, referenceY, availableWidth, 1f, 255, 255, 255, 70);

        Tessellator.getInstance().draw();
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
    }

    private static void quad(WorldRenderer worldRenderer, float x, float y, float width, float height,
                             int red, int green, int blue, int alpha) {
        worldRenderer.pos(x, y + height, 0.0d).color(red, green, blue, alpha).endVertex();
        worldRenderer.pos(x + width, y + height, 0.0d).color(red, green, blue, alpha).endVertex();
        worldRenderer.pos(x + width, y, 0.0d).color(red, green, blue, alpha).endVertex();
        worldRenderer.pos(x, y, 0.0d).color(red, green, blue, alpha).endVertex();
    }

    private static Color health(double fps) {
        if (fps >= FAIR_FPS) {
            return GOOD;
        }
        return fps >= POOR_FPS ? FAIR : POOR;
    }
}

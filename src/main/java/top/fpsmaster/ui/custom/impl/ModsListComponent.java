package top.fpsmaster.ui.custom.impl;

import org.lwjgl.opengl.GL11;
import top.fpsmaster.FPSMaster;
import top.fpsmaster.features.impl.interfaces.ModsList;
import top.fpsmaster.features.manager.Category;
import top.fpsmaster.features.manager.Module;
import top.fpsmaster.ui.custom.Component;
import top.fpsmaster.utils.math.anim.AnimMath;
import top.fpsmaster.utils.render.draw.Colors;
import top.fpsmaster.utils.render.draw.Rects;

import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class ModsListComponent extends Component {
    /** Per-module enter/exit progress (0 = fully hidden). */
    private final Map<Module, Float> progress = new HashMap<>();

    /** Stable draw order: rebuilt only when the set of visible modules changes, so departing
     *  modules fade out in place instead of jumping to the end of the list. */
    private List<Module> displayList = new ArrayList<>();
    private Set<Module> lastVisible = new HashSet<>();

    public ModsListComponent() {
        super(ModsList.class);
        this.x = 1f;
        this.allowScale = true;
    }

    @Override
    public void draw(float x, float y) {
        super.draw(x, y);
        float modY = 0f;

        ModsList modlist = (ModsList) mod;
        if (modlist.showLogo.getValue()) {
            int titleSize = modlist.titleSize.getValue().intValue();
            String logo = modlist.text.getValue();
            float stringWidth = getStringWidth(titleSize, logo);
            drawString(titleSize, logo, (float) (x + (0.5 + width - stringWidth) * scale), y + 0.5f, new Color(0, 0, 0, 100).getRGB());
            drawString(titleSize, logo, x + (width - stringWidth) * scale, y, modlist.titleColor.getRGB());
            modY = (getStringHeight(titleSize) + 2f) * scale;
        }

        String animMode = modlist.animation.getModeName();
        boolean animated = !"None".equals(animMode);
        float animSpeed = modlist.animationSpeed.getValue().intValue() / 100.0f;

        Map<Module, Float> widths = new HashMap<>();
        for (Module m : FPSMaster.moduleManager.modules) {
            String label = modlist.english.getValue() ? m.name : FPSMaster.i18n.get(m.name.toLowerCase());
            widths.put(m, getStringWidth(18, label));
        }
        List<Module> sortedVisible = FPSMaster.moduleManager.modules.stream()
                .filter(m -> m.isEnabled() && m.category != Category.Interface)
                .sorted(Comparator.comparing(widths::get, Comparator.reverseOrder()))
                .collect(Collectors.toList());

        if (animated) {
            Set<Module> visibleSet = new HashSet<>(sortedVisible);
            boolean first = progress.isEmpty();
            if (first) {
                // First frame: settle, do not animate the whole list in.
                for (Module m : sortedVisible) {
                    progress.put(m, 1f);
                }
            } else {
                for (Module m : sortedVisible) {
                    Float p = progress.get(m);
                    progress.put(m, (float) AnimMath.base(p == null ? 0f : p, 1f, animSpeed));
                }
                for (Map.Entry<Module, Float> e : progress.entrySet()) {
                    if (!visibleSet.contains(e.getKey())) {
                        progress.put(e.getKey(), (float) AnimMath.base(e.getValue(), 0f, animSpeed));
                    }
                }
                progress.entrySet().removeIf(e -> e.getValue() <= 0.01f && !visibleSet.contains(e.getKey()));
            }
            if (first || !visibleSet.equals(lastVisible)) {
                // Keep departing ghosts interleaved at their width position, merge newly visible in.
                List<Module> ghosts = new ArrayList<>();
                for (Module m : displayList) {
                    if (!visibleSet.contains(m) && progress.getOrDefault(m, 0f) > 0.01f) {
                        ghosts.add(m);
                    }
                }
                displayList = mergeByWidth(sortedVisible, ghosts, widths);
                lastVisible = visibleSet;
            }
            displayList.removeIf(m -> !visibleSet.contains(m) && progress.getOrDefault(m, 0f) <= 0.01f);
        } else {
            progress.clear();
            displayList = sortedVisible;
            lastVisible = new HashSet<>(sortedVisible);
        }

        x += this.width * scale;
        float maxWidth = 40f;
        float textHeight = getStringHeight(18);
        float rowHeight = textHeight + modlist.spacing.getValue().intValue();
        boolean solid = modlist.bg.getValue();
        boolean bar = modlist.bg.getValue() && modlist.bgStyle.isMode("Bar");
        float barW = modlist.barWidth.getValue().intValue();
        float barGap = 3f;
        Color bgColor = modlist.backgroundColor.getColor();
        Color barColor = modlist.barColor.getColor();
        int yOffset = (int) (-textHeight / 2);
        int index = 0;
        for (Module module : displayList) {
            float p = animated ? Math.max(progress.getOrDefault(module, 1f), 0.01f) : 1f;

            String name = modlist.english.getValue() ? module.name : FPSMaster.i18n.get(module.name.toLowerCase());
            float textWidth = getStringWidth(18, name);
            maxWidth = Math.max(maxWidth, textWidth + 5);

            // The bar sits on the right of the text; shift the text left to make room for it.
            float shift = bar ? (barW + barGap) * scale : 0f;
            float slide = "Slide".equals(animMode) ? (1f - p) * (textWidth + 6f + shift) * scale : 0f;
            float textX = x - (textWidth + 2f) * scale - shift + slide;
            float rowY = y + modY;
            // Rows grow/shrink while entering/exiting, and alpha fades with the progress so the
            // transition is clearly visible in every mode.
            float pRowH = animated ? rowHeight * p : rowHeight;
            // Text is drawn centered on rowY (yOffset = -textHeight/2), so center the row
            // rectangles on the text center too instead of spanning from the row top.
            float rectY = rowY + yOffset + (textHeight - pRowH) * scale / 2f;

            if (solid) {
                Rects.fill(x - (textWidth + 4f) * scale - shift + slide, rectY,
                        (textWidth + 4f) * scale + shift, pRowH * scale,
                        Colors.alpha(bgColor, (int) (bgColor.getAlpha() * p)).getRGB());
            }
            if (bar) {
                Rects.fill(x - 2f * scale - barW * scale + slide, rectY,
                        barW * scale, pRowH * scale,
                        Colors.alpha(barColor, (int) (barColor.getAlpha() * p)).getRGB());
            }

            Color color = modlist.color.getColor(index / (float) Math.max(displayList.size(), 1));
            if (animated) {
                color = Colors.alpha(color, (int) (color.getAlpha() * p));
            }
            if ("Zoom".equals(animMode)) {
                float cx = textX + textWidth * scale / 2f;
                float cy = rowY + pRowH * scale / 2f;
                GL11.glPushMatrix();
                GL11.glTranslatef(cx, cy, 0f);
                GL11.glScalef(p, p, 1f);
                GL11.glTranslatef(-cx, -cy, 0f);
                drawString(18, name, textX, rowY + yOffset, color.getRGB());
                GL11.glPopMatrix();
            } else {
                drawString(18, name, textX, rowY + yOffset, color.getRGB());
            }
            index++;
            modY += pRowH * scale;
        }

        this.width = maxWidth;
        height = modY;
    }

    /** Merges two width-descending lists; {@code ghosts} may contain only modules not in {@code visible}. */
    private static List<Module> mergeByWidth(List<Module> visible, List<Module> ghosts, Map<Module, Float> widths) {
        List<Module> merged = new ArrayList<>(visible.size() + ghosts.size());
        int i = 0;
        int j = 0;
        while (i < visible.size() || j < ghosts.size()) {
            if (i >= visible.size()) {
                merged.add(ghosts.get(j++));
            } else if (j >= ghosts.size()) {
                merged.add(visible.get(i++));
            } else if (widths.get(visible.get(i)) >= widths.get(ghosts.get(j))) {
                merged.add(visible.get(i++));
            } else {
                merged.add(ghosts.get(j++));
            }
        }
        return merged;
    }
}

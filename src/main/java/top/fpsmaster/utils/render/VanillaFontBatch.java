package top.fpsmaster.utils.render;

import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

/**
 * Hoists vanilla's per-character {@code glBegin}/{@code glEnd} out to one pair per string.
 *
 * <p>{@code FontRenderer.renderDefaultChar} opens a primitive, writes four vertices and closes it —
 * <b>per character</b> — and a busy overlay draws several hundred characters a frame. The vertices
 * are unchanged and so is the picture; only the number of primitives is.
 *
 * <p><b>An earlier version of this collected into a {@code WorldRenderer} and it was measurably
 * worse</b>: hud went 50us to 65us on {@code text-dense}, three passes, no overlap. The mixin's
 * per-character callback is in both sides of that comparison, so the regression was the collection
 * itself — {@code pos().tex().endVertex()} does offset arithmetic and a capacity check per vertex,
 * and four of those cost more than the {@code glBegin}/{@code glEnd} pair they replaced. Immediate
 * mode keeps vanilla's exact per-vertex calls and removes only the bracketing, which is the one
 * thing that was ever redundant.
 *
 * <p>Three things force the primitive closed, all of them illegal inside {@code glBegin}: binding a
 * different page, anything that issues its own GL (vanilla's underline goes through the
 * tessellator), and the end of the string, after which the colour is no longer the one these
 * vertices were meant to be drawn in.
 */
public final class VanillaFontBatch {

    private static boolean open;

    /**
     * The bound page, compared by identity.
     *
     * <p>Identity rather than equality because {@code locationFontTexture} is one stable field and
     * the unicode page locations are cached objects, so the comparison is always between the same
     * references — and {@code ResourceLocation.equals} compares two strings, per character, which is
     * exactly the kind of cost that sank the previous attempt.
     */
    private static ResourceLocation texture;

    private VanillaFontBatch() {
    }

    /**
     * Declares which page the next quads come from, closing the primitive if it differs.
     *
     * <p>Returns true when the caller still has to bind — this tracks the page but does not own the
     * binding, because vanilla reaches the ASCII page and the unicode pages by different routes.
     */
    public static boolean use(ResourceLocation page) {
        if (page == texture) {
            return false;
        }
        flush();
        texture = page;
        return true;
    }

    /**
     * One glyph, in the order vanilla writes it: top-left, bottom-left, top-right, bottom-right.
     *
     * <p>Reordered to quad winding on the way in. Vanilla uses a triangle strip, where the third and
     * fourth vertices are the other diagonal; writing them to a quad in strip order folds the glyph
     * over itself.
     */
    public static void glyph(float x0, float y0, float u0, float v0,
                             float x1, float y1, float u1, float v1,
                             float x2, float y2, float u2, float v2,
                             float x3, float y3, float u3, float v3) {
        if (!open) {
            GL11.glBegin(GL11.GL_QUADS);
            open = true;
        }
        GL11.glTexCoord2f(u0, v0);
        GL11.glVertex3f(x0, y0, 0.0f);
        GL11.glTexCoord2f(u1, v1);
        GL11.glVertex3f(x1, y1, 0.0f);
        GL11.glTexCoord2f(u3, v3);
        GL11.glVertex3f(x3, y3, 0.0f);
        GL11.glTexCoord2f(u2, v2);
        GL11.glVertex3f(x2, y2, 0.0f);
    }

    /**
     * Closes the primitive, if one is open.
     *
     * <p>Called at the end of every string and before anything that issues GL of its own. Forgets the
     * page as well: between one string and the next, any code at all may bind a texture, so
     * remembering that the ASCII page was bound would skip a bind that is no longer true. That costs
     * one bind per string, against vanilla's one per character.
     */
    public static void flush() {
        texture = null;
        if (!open) {
            return;
        }
        open = false;
        GL11.glEnd();
    }

    public static boolean isOpen() {
        return open;
    }
}

package top.fpsmaster.benchmark;

import net.minecraft.client.Minecraft;

/**
 * Draws one line of every formatting code through vanilla's renderer, so a screenshot can say
 * whether a font replacement still honours them.
 *
 * <p>Nothing in a recorded lobby exercises italic, obfuscated, strikethrough or underline — servers
 * use colour and bold and little else — so the only way to see them is to ask for them. Drawn
 * through {@code fontRendererObj} rather than the client's renderer directly, because what needs
 * checking is the path vanilla's callers take.
 *
 * <pre>
 *   -Dedge.exp.fontStyles=true
 * </pre>
 */
public final class FontStyles {

    private static final String[] LINES = {
            "plain Ag0 中文",
            "§lbold Ag0 中文",
            "§oitalic Ag0 中文",
            "§kobfuscated",
            "§mstrikethrough Ag0",
            "§nunderline Ag0",
            "§cred §athen green §rthen reset",
            "§lbold §cthen a colour ends it",
            "§9§lblue bold §o+ italic",
    };

    private FontStyles() {
    }

    public static boolean enabled() {
        return Experiments.active(Experiments.FONT_STYLES);
    }

    public static void draw() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.fontRendererObj == null) {
            return;
        }
        for (int i = 0; i < LINES.length; i++) {
            mc.fontRendererObj.drawStringWithShadow(LINES[i], 4.0f, 4.0f + i * 10, 0xFFFFFFFF);
        }
    }
}

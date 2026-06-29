package top.fpsmaster.features.impl.utility;

import top.fpsmaster.event.Subscribe;
import top.fpsmaster.event.events.EventTick;
import top.fpsmaster.features.manager.Category;
import top.fpsmaster.features.manager.Module;
import top.fpsmaster.features.settings.impl.TextSetting;
import top.fpsmaster.utils.core.Utility;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NameProtect extends Module {
    private static boolean using = false;
    private static volatile String playerName = "";
    private static volatile String replacement = "";
    // Precompiled once when the name/replacement changes, instead of compiling a regex on every
    // filter() call (filter() runs on the per-frame text/width hot path).
    private static volatile Pattern namePattern = null;
    private static volatile String quotedReplacement = "";
    public static TextSetting name = new TextSetting("Name", "Hide");

    public NameProtect() {
        super("NameProtect", Category.Utility);
        addSettings(name);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        using = true;
    }

    @Override
    public void onDisable() {
        super.onDisable();
        using = false;
    }

    @Subscribe
    public void onTick(EventTick e) {
        if (Utility.mc.thePlayer != null) {
            String newName = Utility.mc.thePlayer.getName();
            String newReplacement = name.getValue().replace("&", "§");
            // Recompile only when the inputs actually change, not every tick.
            if (!newName.equals(playerName) || !newReplacement.equals(replacement)) {
                playerName = newName;
                replacement = newReplacement;
                quotedReplacement = Matcher.quoteReplacement(newReplacement);
                namePattern = (newName == null || newName.isEmpty())
                        ? null
                        : Pattern.compile(Pattern.quote(newName));
            }
        }
    }

    public static String filter(String s) {
        if (!using || s == null) {
            return s;
        }
        Pattern pattern = namePattern;
        String target = playerName;
        if (pattern == null || target == null || target.isEmpty()) {
            return s;
        }
        // Fast path: most rendered strings don't contain the player's name, so skip the
        // Matcher allocation and scan entirely. indexOf matches the literal-quoted pattern.
        if (s.indexOf(target) < 0) {
            return s;
        }
        return pattern.matcher(s).replaceAll(quotedReplacement);
    }
}




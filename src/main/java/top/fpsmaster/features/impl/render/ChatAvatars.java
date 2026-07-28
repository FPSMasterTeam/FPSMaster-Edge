package top.fpsmaster.features.impl.render;

import top.fpsmaster.features.manager.Category;
import top.fpsmaster.features.manager.Module;
import top.fpsmaster.features.settings.impl.BooleanSetting;
import top.fpsmaster.features.settings.impl.NumberSetting;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.ResourceLocation;

public class ChatAvatars extends Module {
    public static boolean using = false;

    public static final BooleanSetting mojangFallback = new BooleanSetting("MojangFallback", false);
    public static final NumberSetting size = new NumberSetting("Size", 8, 6, 9, 1);
    public static final NumberSetting gap = new NumberSetting("Gap", 4, 1, 8, 1);
    public static final NumberSetting offsetX = new NumberSetting("OffsetX", 0, -8, 8, 1);
    public static final NumberSetting offsetY = new NumberSetting("OffsetY", 1, -4, 6, 1);

    public ChatAvatars() {
        super("ChatAvatars", Category.RENDER);
        addSettings(mojangFallback, size, gap, offsetX, offsetY);
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

    public static boolean isUsing() {
        return using;
    }

    public static int getAvatarSize() {
        return Math.max(6, Math.min(9, size.getValue().intValue()));
    }

    public static int getChatOffset() {
        if (!isUsing()) {
            return 0;
        }
        return getAvatarSize() + Math.max(1, Math.min(8, gap.getValue().intValue()));
    }

    public static int getOffsetX() {
        return offsetX.getValue().intValue();
    }

    public static int getOffsetY() {
        return offsetY.getValue().intValue();
    }

    public static ResourceLocation getAvatar(IChatComponent chatComponent) {
        if (!isUsing()) {
            return null;
        }
        return ChatAvatarCache.getAvatar(chatComponent, mojangFallback.getValue());
    }
}

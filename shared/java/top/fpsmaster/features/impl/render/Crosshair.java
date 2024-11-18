package top.fpsmaster.features.impl.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.player.EntityPlayer;
import top.fpsmaster.event.Subscribe;
import top.fpsmaster.event.events.EventRender2D;
import top.fpsmaster.features.manager.Category;
import top.fpsmaster.features.manager.Module;
import top.fpsmaster.features.settings.impl.BooleanSetting;
import top.fpsmaster.features.settings.impl.ColorSetting;
import top.fpsmaster.features.settings.impl.NumberSetting;
import top.fpsmaster.utils.math.animation.AnimationUtils;
import top.fpsmaster.utils.render.Render2DUtils;
import top.fpsmaster.interfaces.ProviderManager;

import java.awt.Color;

public class Crosshair extends Module {
    private final NumberSetting dynamic = new NumberSetting("Dynamic", 4, 0, 10, 0.1);
    private final BooleanSetting outline = new BooleanSetting("Outline", true);
    private final NumberSetting outlineWidth = new NumberSetting("OutlineWidth", 1, 0, 10, 0.1, () -> outline.getValue());
    private final BooleanSetting dot = new BooleanSetting("Dot", true);
    private final NumberSetting gap = new NumberSetting("Gap", 6, 0, 10, 0.1);
    private final NumberSetting width = new NumberSetting("Width", 0.6, 0, 10, 0.1);
    private final NumberSetting length = new NumberSetting("Length", 3.5, 0, 10, 0.1);
    private final ColorSetting color = new ColorSetting("Color", new Color(255, 255, 255));
    private final ColorSetting outlineColor = new ColorSetting("OutlineColor", new Color(161, 161, 161), () -> outline.getValue());
    private final ColorSetting enemyColor = new ColorSetting("Enemy", new Color(255, 55, 50));
    private final ColorSetting friendColor = new ColorSetting("Friend", new Color(20, 255, 55));

    private float dyna = 0f;

    public Crosshair() {
        super("Crosshair", Category.RENDER);
        addSettings(dynamic, outline, outlineColor, outlineWidth, gap, width, length, color, enemyColor, friendColor);
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
    public void onRender(EventRender2D e) {
        ScaledResolution sr = new ScaledResolution(Minecraft.getMinecraft());
        float gapValue = gap.getValue().floatValue() + dyna;
        float lineWidth = width.getValue().floatValue();
        float lengthValue = length.getValue().floatValue();
        float dynamicValue = dynamic.getValue().floatValue();
        boolean outlineValue = outline.getValue();
        boolean dotValue = dot.getValue();

        dyna = isMoving() ?
                (float) AnimationUtils.base(dyna, dynamicValue, 0.2) :
                (float) AnimationUtils.base(dyna, 0.0, 0.2);

        Color col = color.getColor();
        if (Minecraft.getMinecraft().objectMouseOver != null && Minecraft.getMinecraft().objectMouseOver.entityHit != null) {
            if (isFriend(Minecraft.getMinecraft().objectMouseOver.entityHit)) {
                col = friendColor.getColor();
            } else if (isEnemy(Minecraft.getMinecraft().objectMouseOver.entityHit)) {
                col = enemyColor.getColor();
            }
        }

        // Vertical lines
        drawOutlineRect(sr.getScaledWidth() / 2f - lineWidth / 2f, sr.getScaledHeight() / 2f - lengthValue - gapValue, lineWidth, lengthValue, outlineValue ? outlineWidth.getValue().floatValue() : 0f, col);
        drawOutlineRect(sr.getScaledWidth() / 2f - lineWidth / 2f, sr.getScaledHeight() / 2f + gapValue, lineWidth, lengthValue, outlineValue ? outlineWidth.getValue().floatValue() : 0f, col);

        // Horizontal lines
        drawOutlineRect(sr.getScaledWidth() / 2f - lengthValue - gapValue, sr.getScaledHeight() / 2f - lineWidth / 2f, lengthValue, lineWidth, outlineValue ? outlineWidth.getValue().floatValue() : 0f, col);
        drawOutlineRect(sr.getScaledWidth() / 2f + gapValue, sr.getScaledHeight() / 2f - lineWidth / 2f, lengthValue, lineWidth, outlineValue ? outlineWidth.getValue().floatValue() : 0f, col);

        // Center dot
        if (dotValue) {
            if (outlineValue) {
                Render2DUtils.drawRect(sr.getScaledWidth() / 2f - 1 - outlineWidth.getValue().floatValue(),
                        sr.getScaledHeight() / 2f - 1 - outlineWidth.getValue().floatValue(),
                        2 + outlineWidth.getValue().floatValue() * 2,
                        2 + outlineWidth.getValue().floatValue() * 2,
                        outlineColor.getColor());
            }
            Render2DUtils.drawRect(sr.getScaledWidth() / 2f - 1, sr.getScaledHeight() / 2f - 1, 2f, 2f, col);
        }

        GlStateManager.disableBlend();
        GlStateManager.resetColor();
    }

    private void drawOutlineRect(float x, float y, float width, float height, float outlineWidth, Color color) {
        Render2DUtils.drawRect(x - outlineWidth, y - outlineWidth, width + outlineWidth * 2, height + outlineWidth * 2, outlineColor.getColor());
        Render2DUtils.drawRect(x, y, width, height, color);
    }

    private boolean isFriend(Object entity) {
        if (entity instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) entity;
            return isTeammate(player) || entity instanceof EntityAnimal;
        }
        return false;
    }

    private boolean isEnemy(Object entity) {
        if (entity instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) entity;
            return !isTeammate(player);
        }
        return entity instanceof EntityMob;
    }

    private boolean isTeammate(EntityPlayer e) {
        EntityPlayer player = ProviderManager.mcProvider.getPlayer();
        return player != null && e.getTeam() != null && e.getTeam().isSameTeam(player.getTeam());
    }

    private boolean isMoving() {
        EntityPlayer player = ProviderManager.mcProvider.getPlayer();
        return player != null && player.isSprinting();
    }

    public static boolean using = false;
}

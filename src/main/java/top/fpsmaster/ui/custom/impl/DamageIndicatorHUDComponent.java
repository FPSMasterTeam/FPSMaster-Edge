package top.fpsmaster.ui.custom.impl;

import top.fpsmaster.features.impl.interfaces.DamageIndicatorHUD;
import top.fpsmaster.ui.custom.TextComponent;

import java.text.DecimalFormat;

public class DamageIndicatorHUDComponent extends TextComponent {
    private static final DecimalFormat FORMAT = new DecimalFormat("0.00");

    public DamageIndicatorHUDComponent() {
        super(DamageIndicatorHUD.class);
        allowScale = true;
    }

    @Override
    protected String text() {
        float damage = DamageIndicatorHUD.recentDamage();
        return damage < 0f ? null : label() + FORMAT.format(damage);
    }

    /** No hit has landed while the editor is open, so a representative one is shown. */
    @Override
    protected String previewText() {
        return label() + FORMAT.format(3.5f);
    }

    private String label() {
        return resolveLabel(null, "damageindicatorhud.label", "Damage");
    }

    @Override
    protected int fontSize() {
        return 16;
    }

    @Override
    protected int textColor() {
        return DamageIndicatorHUD.textColor.getRGB();
    }

    @Override
    protected float boxHeight() {
        return 16f;
    }

    @Override
    protected float textOffsetY() {
        return 4f;
    }
}

package top.fpsmaster.ui.devspace.map.expressions;

import top.fpsmaster.FPSMaster;
import top.fpsmaster.modules.lua.parser.Expression;
import top.fpsmaster.utils.render.Render2DUtils;

import java.awt.*;

public class LiteralExpressionComponent extends ExpressionComponent {

    String value;

    public LiteralExpressionComponent(Expression.LiteralExpression expression) {
        super(expression);
        value = expression.value;
    }

    @Override
    public void draw(int x, int y, int mouseX, int mouseY) {
        super.draw(x, y, mouseX, mouseY);
        Expression.LiteralExpression exp = (Expression.LiteralExpression) expression;
        if ("STRING".equals(exp.type)) {
            this.width = FPSMaster.fontManager.s16.getStringWidth(value) + 10;
            this.height = FPSMaster.fontManager.s16.getHeight() + 4;
            Render2DUtils.drawOptimizedRoundedRect(x, y, width, height, 3, new Color(50, 50, 50).getRGB());
            FPSMaster.fontManager.s16.drawString("\"" + value + "\"", x + 2, y + 2, new Color(197, 134, 192).getRGB());
        } else if ("NUMBER".equals(exp.type)) {
            this.width = FPSMaster.fontManager.s16.getStringWidth(value) + 2;
            this.height = FPSMaster.fontManager.s16.getHeight() + 2;
            FPSMaster.fontManager.s16.drawString(value, x + 1, y + 2, new Color(255, 255, 255).getRGB());
        } else if ("BOOLEAN".equals(exp.type)) {
            this.width = FPSMaster.fontManager.s16.getStringWidth(value) + 2;
            this.height = FPSMaster.fontManager.s16.getHeight() + 2;
            FPSMaster.fontManager.s16.drawString(value, x + 1, y + 2, new Color(220, 120, 100).getRGB());
        }
    }
}

package top.fpsmaster.ui.devspace.map.expressions;

import top.fpsmaster.FPSMaster;
import top.fpsmaster.modules.lua.parser.Expression;

public class VariableExpressionComponent extends ExpressionComponent {
    String name;

    public VariableExpressionComponent(Expression.VariableExpression expression) {
        super(expression);
        name = expression.getName();
    }

    @Override
    public void draw(int x, int y, int mouseX, int mouseY) {
        super.draw(x, y, mouseX, mouseY);
        FPSMaster.fontManager.s16.drawString(name, x, y + 2, -1);
        height = 10;
        width = FPSMaster.fontManager.s16.getStringWidth(name);
    }
}

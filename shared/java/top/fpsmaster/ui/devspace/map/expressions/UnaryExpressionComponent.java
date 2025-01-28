package top.fpsmaster.ui.devspace.map.expressions;

import top.fpsmaster.FPSMaster;
import top.fpsmaster.modules.lua.parser.Expression;
import top.fpsmaster.ui.devspace.DevSpace;

public class UnaryExpressionComponent extends ExpressionComponent {

    String operator;
    ExpressionComponent expression;

    public UnaryExpressionComponent(Expression.UnaryExpression expression) {
        super(expression);
        this.operator = expression.operator;
        this.expression = DevSpace.parseExpression(expression.expression);
    }

    @Override
    public void draw(int x, int y, int mouseX, int mouseY) {
        super.draw(x, y, mouseX, mouseY);
        FPSMaster.fontManager.s16.drawString(operator, x, y, -1);
        expression.draw(x + 10, y + 10, mouseX, mouseY);
        height = expression.height + 20;
    }
}

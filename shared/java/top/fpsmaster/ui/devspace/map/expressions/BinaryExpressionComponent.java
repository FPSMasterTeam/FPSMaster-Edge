package top.fpsmaster.ui.devspace.map.expressions;

import top.fpsmaster.FPSMaster;
import top.fpsmaster.modules.lua.parser.Expression;
import top.fpsmaster.ui.devspace.DevSpace;

public class BinaryExpressionComponent extends ExpressionComponent {
    ExpressionComponent left;
    ExpressionComponent right;
    String operator;

    public BinaryExpressionComponent(Expression.BinaryExpression expression) {
        super(expression);
        left = DevSpace.parseExpression(expression.left);
        right = DevSpace.parseExpression(expression.right);
        operator = expression.operator;
    }

    @Override
    public void draw(int x, int y, int mouseX, int mouseY) {
        super.draw(x, y, mouseX, mouseY);
        FPSMaster.fontManager.s16.drawString(operator, x + 10, y + 10, -1);
    }
}

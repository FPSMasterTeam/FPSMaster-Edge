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
        int operatorWidth = FPSMaster.fontManager.s16.getStringWidth(operator);
        left.draw(x, y, mouseX, mouseY);
        FPSMaster.fontManager.s16.drawString(operator, x + left.width + 2, y + 2, -1);
        right.draw(x + left.width + operatorWidth + 2, y, mouseX, mouseY);
        width = left.width + right.width + operatorWidth + 4;
        height = 10;
    }
}

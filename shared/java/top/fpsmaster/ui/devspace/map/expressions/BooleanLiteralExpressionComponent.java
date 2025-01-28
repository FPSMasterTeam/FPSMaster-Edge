package top.fpsmaster.ui.devspace.map.expressions;

import top.fpsmaster.modules.lua.parser.Expression;

public class BooleanLiteralExpressionComponent extends ExpressionComponent{
    boolean value;
    public BooleanLiteralExpressionComponent(Expression.BooleanLiteralExpression expression) {
        super(expression);
        value = expression.getValue();
    }

    @Override
    public void draw(int x, int y, int mouseX, int mouseY) {
        super.draw(x, y, mouseX, mouseY);

    }
}

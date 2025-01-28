package top.fpsmaster.ui.devspace.map.expressions;

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
    }
}

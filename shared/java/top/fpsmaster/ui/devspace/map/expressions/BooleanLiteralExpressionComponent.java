package top.fpsmaster.ui.devspace.map.expressions;

import top.fpsmaster.modules.lua.parser.Expression;

public class BooleanLiteralExpressionComponent extends ExpressionComponent{
    boolean value;
    public BooleanLiteralExpressionComponent(Expression.BooleanLiteralExpression expression) {
        super(expression);
        value = expression.getValue();
    }
}

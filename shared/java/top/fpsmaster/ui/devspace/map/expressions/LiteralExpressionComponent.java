package top.fpsmaster.ui.devspace.map.expressions;

import top.fpsmaster.modules.lua.parser.Expression;

public class LiteralExpressionComponent extends ExpressionComponent{

    String value;
    public LiteralExpressionComponent(Expression.LiteralExpression expression) {
        super(expression);
        value = expression.value;
    }
}

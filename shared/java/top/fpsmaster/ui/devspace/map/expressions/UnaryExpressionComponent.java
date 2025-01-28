package top.fpsmaster.ui.devspace.map.expressions;

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
}

package top.fpsmaster.ui.devspace.map.expressions;

import top.fpsmaster.modules.lua.parser.Expression;
import top.fpsmaster.ui.devspace.DevSpace;

import java.util.ArrayList;
import java.util.List;

public class FunctionCallExpressionComponent extends ExpressionComponent{
    String name;
    List<ExpressionComponent> arguments;
    public FunctionCallExpressionComponent(Expression.FunctionCallExpression expression) {
        super(expression);
        this.name = expression.name;
        this.arguments = DevSpace.parseExpressions(expression.arguments);
    }

    @Override
    public void draw(int x, int y, int mouseX, int mouseY) {
        super.draw(x, y, mouseX, mouseY);
    }
}

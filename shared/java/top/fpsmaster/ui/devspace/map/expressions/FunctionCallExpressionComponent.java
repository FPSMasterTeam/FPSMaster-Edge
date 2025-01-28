package top.fpsmaster.ui.devspace.map.expressions;

import top.fpsmaster.FPSMaster;
import top.fpsmaster.modules.lua.parser.Expression;
import top.fpsmaster.ui.devspace.DevSpace;

import java.util.ArrayList;
import java.util.List;

public class FunctionCallExpressionComponent extends ExpressionComponent {
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
        FPSMaster.fontManager.s16.drawString("call " + name, x, y, -1);
        FPSMaster.fontManager.s16.drawString("args: ", x, y + 10, -1);
        height = 20;

        for (ExpressionComponent arg : arguments) {
            arg.draw(x, y+height, mouseX, mouseY);
            height += arg.height;
        }

    }
}

package top.fpsmaster.ui.devspace.map.expressions;

import top.fpsmaster.FPSMaster;
import top.fpsmaster.ui.devspace.DevSpace;
import top.skidder.parser.Expression;

import java.awt.*;
import java.util.List;

public class MethodCallExpressionComponent extends ExpressionComponent {
    ExpressionComponent object;
    String method;
    List<ExpressionComponent> arguments;
    boolean isColonCall;

    public MethodCallExpressionComponent(Expression.MethodCallExpression expression) {
        super(expression);
        this.object = DevSpace.parseExpression(expression.getObject());
        this.method = expression.getMethod();
        this.arguments = DevSpace.parseExpressions(expression.getArguments());
        this.isColonCall = expression.isColonCall;
    }

    @Override
    public void draw(int x, int y, int mouseX, int mouseY) {
        super.draw(x, y, mouseX, mouseY);
        object.draw(x, y, mouseX, mouseY);
        FPSMaster.fontManager.s16.drawString(":" + method + "()", x + 1 + object.width, y, new Color(206, 206, 114).getRGB());
        height = object.height;
    }
}

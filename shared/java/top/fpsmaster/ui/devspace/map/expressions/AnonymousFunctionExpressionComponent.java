package top.fpsmaster.ui.devspace.map.expressions;

import top.fpsmaster.FPSMaster;
import top.fpsmaster.modules.lua.parser.Expression;
import top.fpsmaster.ui.devspace.DevSpace;
import top.fpsmaster.ui.devspace.map.statements.StatementComponent;

import java.util.List;

public class AnonymousFunctionExpressionComponent extends ExpressionComponent {
    List<String> parameters;
    List<StatementComponent> body;

    public AnonymousFunctionExpressionComponent(Expression.AnonymousFunctionExpression expression) {
        super(expression);
        parameters = expression.parameters;
        body = DevSpace.parseStatements(expression.body);
    }

    @Override
    public void draw(int x, int y, int mouseX, int mouseY) {
        super.draw(x, y, mouseX, mouseY);
        FPSMaster.fontManager.s16.drawString("anonymous function", x, y, -1);
        height = 10;
        for (StatementComponent statement : body) {
            statement.draw(x, y + height, mouseX, mouseY);
            height += statement.height;
        }
    }
}

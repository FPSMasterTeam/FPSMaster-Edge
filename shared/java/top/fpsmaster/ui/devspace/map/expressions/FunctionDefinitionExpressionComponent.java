package top.fpsmaster.ui.devspace.map.expressions;

import top.fpsmaster.FPSMaster;
import top.fpsmaster.modules.lua.parser.Expression;
import top.fpsmaster.ui.devspace.DevSpace;
import top.fpsmaster.ui.devspace.map.statements.StatementComponent;

import java.util.List;

public class FunctionDefinitionExpressionComponent extends ExpressionComponent {
    String name;
    List<String> parameters;
    List<StatementComponent> body;

    public FunctionDefinitionExpressionComponent(Expression.FunctionDefinitionExpression expression) {
        super(expression);
        name = expression.name;
        parameters = expression.parameters;
        body = DevSpace.parseStatements(expression.body);
    }

    @Override
    public void draw(int x, int y, int mouseX, int mouseY) {
        super.draw(x, y, mouseX, mouseY);
        FPSMaster.fontManager.s16.drawString("define " + name + "(" + String.join(",", parameters) + ")", x, y, -1);
        height = 20;
        for (StatementComponent statement : body) {
            statement.draw(x + 10, y + height, mouseX, mouseY);
            height += statement.height;
        }
    }
}

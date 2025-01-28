package top.fpsmaster.ui.devspace.map.statements;

import top.fpsmaster.modules.lua.parser.Statement;
import top.fpsmaster.ui.devspace.map.expressions.ExpressionComponent;

public class ExpressionStatementComponent extends StatementComponent {
    ExpressionComponent expr;
    public ExpressionStatementComponent(Statement.ExpressionStatement statement) {
        super(statement);
        expr = new ExpressionComponent(statement.getExpression());
    }

    @Override
    public void draw(int x, int y, int mouseX, int mouseY) {
        super.draw(x, y, mouseX, mouseY);
        expr.draw(x, y, mouseX, mouseY);
    }
}

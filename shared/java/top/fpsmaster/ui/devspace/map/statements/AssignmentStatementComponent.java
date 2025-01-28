package top.fpsmaster.ui.devspace.map.statements;

import top.fpsmaster.modules.lua.parser.Statement;
import top.fpsmaster.ui.devspace.DevSpace;
import top.fpsmaster.ui.devspace.map.expressions.ExpressionComponent;

public class AssignmentStatementComponent extends StatementComponent {
    String variable;
    ExpressionComponent value;

    public AssignmentStatementComponent(Statement.AssignmentStatement statement) {
        super(statement);
        variable = statement.variable;
        value = DevSpace.parseExpression(statement.value);
    }

    @Override
    public void draw(int x, int y, int mouseX, int mouseY) {
        super.draw(x, y, mouseX, mouseY);
    }
}

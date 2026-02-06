package top.fpsmaster.ui.devspace.map.statements;

import top.fpsmaster.FPSMaster;
import top.fpsmaster.ui.devspace.DevSpace;
import top.fpsmaster.ui.devspace.map.expressions.ExpressionComponent;
import top.skidder.parser.Statement;

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
        FPSMaster.fontManager.s16.drawString(variable + " = ", x, y, -1);
        value.draw(x + 10, y + 10, mouseX, mouseY);
        height = 20 + value.height;
    }
}

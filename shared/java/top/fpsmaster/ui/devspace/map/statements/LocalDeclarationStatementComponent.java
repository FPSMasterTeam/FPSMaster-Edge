package top.fpsmaster.ui.devspace.map.statements;

import top.fpsmaster.FPSMaster;
import top.fpsmaster.modules.lua.parser.Statement;
import top.fpsmaster.ui.devspace.DevSpace;
import top.fpsmaster.ui.devspace.map.expressions.ExpressionComponent;

public class LocalDeclarationStatementComponent extends StatementComponent {
    String variableName;
    ExpressionComponent initializer;

    public LocalDeclarationStatementComponent(Statement.LocalDeclarationStatement statement) {
        super(statement);
        variableName = statement.variableName;
        initializer = DevSpace.parseExpression(statement.initializer);
    }

    @Override
    public void draw(int x, int y, int mouseX, int mouseY) {
        super.draw(x, y, mouseX, mouseY);
        FPSMaster.fontManager.s16.drawString("local " + variableName, x, y, -1);
        height = 20;
        if (initializer != null) {
            initializer.draw(x + 10, y + height, mouseX, mouseY);
            height += initializer.height;
        }
    }
}

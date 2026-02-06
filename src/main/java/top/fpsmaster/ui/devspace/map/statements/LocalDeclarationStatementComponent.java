package top.fpsmaster.ui.devspace.map.statements;

import top.fpsmaster.FPSMaster;
import top.fpsmaster.ui.devspace.DevSpace;
import top.fpsmaster.ui.devspace.map.expressions.ExpressionComponent;
import top.fpsmaster.utils.render.Render2DUtils;
import top.skidder.parser.Statement;

import java.awt.*;

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
        int w = FPSMaster.fontManager.s16.getStringWidth("local " + variableName + " = ");
        Render2DUtils.drawRect(x, y, w + 20, 14, new Color(17, 17, 17));
        FPSMaster.fontManager.s16.drawString("local " + variableName + " = ", x + 5, y, -1);

        height = 14;
        if (initializer != null) {
            initializer.draw(x + 10, y + height, mouseX, mouseY);
            height += initializer.height;
        }
    }
}

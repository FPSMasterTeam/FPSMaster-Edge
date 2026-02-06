package top.fpsmaster.ui.devspace.map.statements;

import top.fpsmaster.FPSMaster;
import top.fpsmaster.ui.devspace.DevSpace;
import top.fpsmaster.ui.devspace.map.expressions.ExpressionComponent;
import top.skidder.parser.Statement;

import java.util.List;

public class ReturnStatementComponent extends StatementComponent {
    List<ExpressionComponent> returnValues;

    public ReturnStatementComponent(Statement.ReturnStatement statement) {
        super(statement);
        returnValues = DevSpace.parseExpressions(statement.getReturnValues());
    }

    @Override
    public void draw(int x, int y, int mouseX, int mouseY) {
        super.draw(x, y, mouseX, mouseY);
        height = 20;
        FPSMaster.fontManager.s16.drawString("return", x + 10, y, -1);
        for (ExpressionComponent returnValue : returnValues) {
            returnValue.draw(x + 20, y + height, mouseX, mouseY);
            height += returnValue.height;
        }
    }
}

package top.fpsmaster.ui.devspace.map.statements;

import top.fpsmaster.modules.lua.parser.Statement;
import top.fpsmaster.ui.devspace.DevSpace;
import top.fpsmaster.ui.devspace.map.expressions.ExpressionComponent;

import java.util.List;

public class ReturnStatementComponent extends StatementComponent {
    List<ExpressionComponent> returnValues;

    public ReturnStatementComponent(Statement.ReturnStatement statement) {
        super(statement);
        returnValues = DevSpace.parseExpressions(statement.getReturnValues());
    }
}

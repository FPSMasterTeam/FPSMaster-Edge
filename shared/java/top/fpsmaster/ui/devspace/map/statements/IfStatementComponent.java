package top.fpsmaster.ui.devspace.map.statements;

import top.fpsmaster.modules.lua.parser.Statement;
import top.fpsmaster.ui.devspace.DevSpace;
import top.fpsmaster.ui.devspace.map.expressions.ExpressionComponent;

import java.util.List;

public class IfStatementComponent extends StatementComponent {

    ExpressionComponent condition;
    List<StatementComponent> ifStatements;
    List<StatementComponent> elseifStatements;
    List<ExpressionComponent> elseifConditions;
    List<StatementComponent> elseStatements;

    public IfStatementComponent(Statement.IfStatement statement) {
        super(statement);
        this.condition = DevSpace.parseExpression(statement.getCondition());
        this.ifStatements = DevSpace.parseStatements(statement.getIfStatements());
        this.elseifStatements = DevSpace.parseStatements(statement.getElseStatements());
        this.elseifConditions = DevSpace.parseExpressions(statement.getElseifConditions());
        this.elseStatements = DevSpace.parseStatements(statement.getElseStatements());
    }

    @Override
    public void draw(int x, int y, int mouseX, int mouseY) {
        super.draw(x, y, mouseX, mouseY);
        condition.draw(x, y, mouseX, mouseY);
//        ifStatements.forEach(stmt -> stmt.draw(x, y, mouseX, mouseY));
//        elseifStatements.forEach(stmt -> stmt.draw(x, y, mouseX, mouseY));
//        elseifConditions.forEach(stmt -> stmt.draw(x, y, mouseX, mouseY));
//        elseStatements.forEach(stmt -> stmt.draw(x, y, mouseX, mouseY));
    }
}

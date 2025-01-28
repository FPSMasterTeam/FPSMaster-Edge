package top.fpsmaster.ui.devspace.map.expressions;

import top.fpsmaster.modules.lua.parser.Expression;
import top.fpsmaster.ui.devspace.DevSpace;

import java.util.List;
import java.util.Map;

public class TableExpressionComponent extends ExpressionComponent{
    List<ExpressionComponent> arrayElements;
    Map<String, ExpressionComponent> tableElements;
    public TableExpressionComponent(Expression.TableExpression expression) {
        super(expression);
        arrayElements = DevSpace.parseExpressions(expression.getArrayElements());
        expression.getTableEntries().forEach((k, v) -> tableElements.put(k, DevSpace.parseExpression(v)));
    }

    @Override
    public void draw(int x, int y, int mouseX, int mouseY) {
        super.draw(x, y, mouseX, mouseY);
    }
}

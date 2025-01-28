package top.fpsmaster.ui.devspace.map.expressions;

import top.fpsmaster.FPSMaster;
import top.fpsmaster.modules.lua.parser.Expression;
import top.fpsmaster.ui.devspace.DevSpace;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TableExpressionComponent extends ExpressionComponent {
    List<ExpressionComponent> arrayElements;
    Map<String, ExpressionComponent> tableElements = new HashMap<>();

    public TableExpressionComponent(Expression.TableExpression expression) {
        super(expression);
        arrayElements = DevSpace.parseExpressions(expression.getArrayElements());
        expression.getTableEntries().forEach((k, v) -> tableElements.put(k, DevSpace.parseExpression(v)));
    }

    @Override
    public void draw(int x, int y, int mouseX, int mouseY) {
        super.draw(x, y, mouseX, mouseY);
        height = 10;
        FPSMaster.fontManager.s16.drawString("array:", x + 10, y, -1);
        for (ExpressionComponent arrayElement : arrayElements) {
            arrayElement.draw(x + 20, y + height, mouseX, mouseY);
            height += arrayElement.height;
        }
        FPSMaster.fontManager.s16.drawString("table:", x + 10, y + height + 10, -1);
        tableElements.forEach((k, v) -> {
            FPSMaster.fontManager.s16.drawString(k + ":", x+20, y + height + 20, -1);
            v.draw(x + 20, y + height + 30, mouseX, mouseY);
            height += v.height;
            height += 10;
        });
    }
}

package top.fpsmaster.ui.devspace.map.expressions;

import net.minecraft.util.ResourceLocation;
import top.fpsmaster.FPSMaster;
import top.fpsmaster.ui.devspace.DevSpace;
import top.fpsmaster.ui.devspace.map.statements.StatementComponent;
import top.fpsmaster.utils.render.Render2DUtils;
import top.skidder.parser.Expression;

import java.awt.*;
import java.util.List;

public class FunctionDefinitionExpressionComponent extends ExpressionComponent {
    String name;
    List<String> parameters;
    List<StatementComponent> body;

    public FunctionDefinitionExpressionComponent(Expression.FunctionDefinitionExpression expression) {
        super(expression);
        name = expression.name;
        parameters = expression.parameters;
        body = DevSpace.parseStatements(expression.body);
    }

    @Override
    public void draw(int x, int y, int mouseX, int mouseY) {
        super.draw(x, y, mouseX, mouseY);
        int newH = 14;
        int newWidth = 80;
        Render2DUtils.drawOptimizedRoundedRect(x, y, width, this.height - 10, new Color(17, 17, 17));
        Render2DUtils.drawImage(new ResourceLocation("client/gui/scripts/func.png"), x + 5, y + 2, 8, 8, -1);
        Render2DUtils.drawOptimizedRoundedRect(x, y + 14, width, this.height - 38, new Color(37, 37, 37));
        FPSMaster.fontManager.s16.drawString(name + "(" + String.join(",", parameters) + ")", x + 16, y, -1);
        for (StatementComponent statement : body) {
            statement.draw(x + 10, y + newH, mouseX, mouseY);
            newH += statement.height;
            if (statement.width + 20 > newWidth) {
                newWidth = statement.width + 10;
            }
        }
        Render2DUtils.drawImage(new ResourceLocation("client/gui/scripts/end.png"), x + 7, y + newH + 3, 5, 5.5f, -1);
        FPSMaster.fontManager.s16.drawString("nil", x + 16, y + newH, -1);
        newH += 14;
        this.height = newH + 10;
        this.width = newWidth + 10;
    }
}
